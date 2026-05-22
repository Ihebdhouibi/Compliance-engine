import {
  Component, OnInit, OnDestroy, inject, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { AuditRequestService } from '../../services/audit-request.service';
import { AuditFormService } from '../../services/audit-form.service';
import { AuditProcessStepService, AuditProcessStep } from '../../services/audit-process-step.service';
import { AuditStepResultService, AuditStepResult } from '../../services/audit-step-result.service';
import { TokenService } from '../../shared/token.service';
import { AuditBotComponent } from '../../layout/audit-bot/audit-bot.component';
import { AuditRequest } from '../../models/audit.model';
import { environment } from '../../environments/environment';

/**
 * Unified step model used by the workspace stepper.
 * Built from intake form steps (preferred — they carry the actual fields)
 * or from auditor process steps (fallback when no form template is available).
 */
interface WorkspaceStep {
  id:        number;        // intake form step id, or -1 for synthetic fallback
  name:      string;
  stepOrder: number;
  fieldIds:  number[];      // empty array => show all answers (fallback mode)
  isDefault?: boolean;
}

/** Auditor's per-question verdict. */
export type Verdict = 'compliant' | 'observation' | 'non-conformity' | 'na';

/** Step-level roll-up derived from per-question verdicts. */
export type StepRollup = 'pending' | 'compliant' | 'partial' | 'non-compliant';

export interface Finding {
  id:          string;
  severity:    'low' | 'medium' | 'high' | 'critical';
  description: string;
}

export interface Recommendation {
  id:          string;
  description: string;
}

/** Persisted alongside the step's description as a hidden HTML-comment block. */
interface StepMeta {
  verdicts:        Record<number, Verdict>;   // keyed by fieldId
  findings:        Finding[];
  recommendations: Recommendation[];
}

const META_OPEN  = '<!--AUDIT_META_V1:';
const META_CLOSE = ':END-->';

/** OCR row returned by GET /api/v1/audits/{id}/ocr. */
interface OcrResult {
  id:             number;
  auditRequestId: number;
  mediaId:        number;
  fileName?:      string;
  mimeType?:      string;
  jobId?:         string;
  status:         'PENDING' | 'RUNNING' | 'DONE' | 'FAILED';
  pageCount?:     number;
  engine?:        string;
  elapsedMs?:     number;
  rawText?:       string;
  error?:         string;
  createdAt?:     string;
  updatedAt?:     string;
}

@Component({
  selector: 'app-audit-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule, AuditBotComponent],
  templateUrl: './audit-workspace.component.html',
  styleUrl: './audit-workspace.component.scss'
})
export class AuditWorkspaceComponent implements OnInit, OnDestroy {

  request:     AuditRequest | null = null;
  steps:       WorkspaceStep[]     = [];
  results:     AuditStepResult[]   = [];
  activeStep   = 0;
  botOpen      = false;
  isSaving     = false;
  isCompleting = false;
  saveMsg      = '';
  saveError    = '';

  drafts: Record<number, string> = {};

  // ── Phase 2: per-question verdicts + findings/recs (per step) ──────
  /** verdicts[stepId][fieldId] = verdict */
  verdicts:        Record<number, Record<number, Verdict>> = {};
  findings:        Record<number, Finding[]>        = {};
  recommendations: Record<number, Recommendation[]> = {};

  showFileViewer   = false;
  viewingFileUrl:  SafeResourceUrl | null = null;
  viewingFileName: string | null = null;
  isLoadingFile    = false;

  // ── OCR (extracted-text preview per evidence file) ─────────────────
  ocrByMedia: Record<number, OcrResult> = {};
  showOcrViewer = false;
  viewingOcr:   OcrResult | null = null;
  private ocrPollHandle: any = null;

  isAuditorMode = false;

  readonly serverBase = environment.serverBaseUrl;

  private route     = inject(ActivatedRoute);
  private router    = inject(Router);
  private reqSvc    = inject(AuditRequestService);
  private formSvc   = inject(AuditFormService);
  private stepSvc   = inject(AuditProcessStepService);
  private resSvc    = inject(AuditStepResultService);
  private sanitizer = inject(DomSanitizer);
  private http      = inject(HttpClient);
  private tokenSvc  = inject(TokenService);
  private cdr       = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.isAuditorMode = this.router.url.includes('/auditor/');
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadRequest(id);
  }

  ngOnDestroy(): void {
    if (this.ocrPollHandle) {
      clearTimeout(this.ocrPollHandle);
      this.ocrPollHandle = null;
    }
  }

  loadRequest(id: number): void {
    const request$ = this.isAuditorMode
      ? this.reqSvc.auditorGetAuditById(id)
      : this.reqSvc.adminGetRequest(id);

    request$.subscribe({
      next: req => {
        this.request = req;

        if (req.status === 'ASSIGNED') {
          this.autoStart(id);
        }

        // Load intake form template — each form step becomes a workspace step.
        // This gives the stepper meaningful navigation (Prev/Next across each
        // intake section) AND exact per-step answer filtering by fieldId.
        this.loadFormSteps(req.auditType);

        this.loadResults(id);
        this.loadOcr(id);
        this.cdr.detectChanges();
      },
      error: () => this.flash('Failed to load audit request.', true)
    });
  }

  private loadFormSteps(auditType: string): void {
    this.formSvc.getTemplateByType(auditType as any).subscribe({
      next: tpl => {
        const formSteps = (tpl?.steps ?? [])
          .slice()
          .sort((a, b) => (a.stepOrder ?? 0) - (b.stepOrder ?? 0));

        if (formSteps.length > 0) {
          this.steps = formSteps.map(s => ({
            id:        s.id,
            name:      s.title,
            stepOrder: s.stepOrder,
            fieldIds:  (s.fields ?? []).map(f => f.id)
          }));
        } else {
          // No intake form steps — fall back to auditor process steps.
          this.loadStepsByAuditType(auditType);
          return;
        }

        this.steps.forEach(s => {
          if (this.drafts[s.id] === undefined) this.drafts[s.id] = '';
        });
        // Re-apply any results that loaded before steps were ready.
        this.applyResultsToDrafts();
        this.cdr.detectChanges();
      },
      error: () => this.loadStepsByAuditType(auditType)
    });
  }

  // Fallback when no intake form template exists.
  private loadStepsByAuditType(auditType: string): void {
    this.reqSvc.getProcessStepsByAuditType(auditType, this.isAuditorMode).subscribe({
      next: steps => {
        const real = steps.filter((s: any) => s.id > 0 && !s.isDefault);
        if (real.length > 0) {
          this.steps = real.map((s: AuditProcessStep) => ({
            id:        s.id,
            name:      s.name,
            stepOrder: s.stepOrder,
            fieldIds:  [],
            isDefault: s.isDefault
          }));
          this.steps.forEach(s => {
            if (this.drafts[s.id] === undefined) this.drafts[s.id] = '';
          });
          this.applyResultsToDrafts();
        } else {
          this.setDefaultStep();
        }
        this.cdr.detectChanges();
      },
      error: () => this.setDefaultStep()
    });
  }

  private setDefaultStep(): void {
    this.steps = [{ id: -1, name: 'Audit Review', stepOrder: 1, fieldIds: [], isDefault: true }];
    this.drafts[-1] = '';
    this.applyResultsToDrafts();
    this.cdr.detectChanges();
  }

  private autoStart(id: number): void {
    const start$ = this.isAuditorMode
      ? this.reqSvc.auditorStartAudit(id)
      : this.reqSvc.adminStartAudit(id);

    start$.subscribe({
      next: updated => {
        this.request = updated;
        this.cdr.detectChanges();
      }
    });
  }

  loadResults(requestId: number): void {
    this.resSvc.getResults(requestId).subscribe({
      next: results => {
        this.results = results;
        this.applyResultsToDrafts();
        this.cdr.detectChanges();
      }
    });
  }

  /** Match results to current steps by stepName (resilient when results load before steps). */
  private applyResultsToDrafts(): void {
    if (!this.steps.length || !this.results.length) return;
    for (const r of this.results) {
      const matching = this.steps.find(s => s.name === r.stepName);
      if (!matching) continue;
      const { note, meta } = this.parseStepBody(r.description ?? '');
      this.drafts[matching.id]         = note;
      this.verdicts[matching.id]       = meta.verdicts;
      this.findings[matching.id]       = meta.findings;
      this.recommendations[matching.id] = meta.recommendations;
    }
  }

  // ── Meta (de)serialization ───────────────────────────────────────
  private emptyMeta(): StepMeta {
    return { verdicts: {}, findings: [], recommendations: [] };
  }

  /** Split a stored description into the visible note and the hidden meta block. */
  private parseStepBody(body: string): { note: string; meta: StepMeta } {
    const i = body.indexOf(META_OPEN);
    if (i === -1) return { note: body, meta: this.emptyMeta() };
    const j = body.indexOf(META_CLOSE, i + META_OPEN.length);
    if (j === -1) return { note: body, meta: this.emptyMeta() };

    const json = body.substring(i + META_OPEN.length, j);
    let meta = this.emptyMeta();
    try {
      const parsed = JSON.parse(json) as Partial<StepMeta>;
      meta = {
        verdicts:        parsed.verdicts        ?? {},
        findings:        parsed.findings        ?? [],
        recommendations: parsed.recommendations ?? []
      };
    } catch { /* ignore malformed */ }
    const note = (body.substring(0, i) + body.substring(j + META_CLOSE.length)).trim();
    return { note, meta };
  }

  /** Re-build the description string from the step note + meta state. */
  private composeStepBody(stepId: number): string {
    const note = (this.drafts[stepId] ?? '').trim();
    const meta: StepMeta = {
      verdicts:        this.verdicts[stepId]        ?? {},
      findings:        this.findings[stepId]        ?? [],
      recommendations: this.recommendations[stepId] ?? []
    };
    const hasMeta =
      Object.keys(meta.verdicts).length > 0 ||
      meta.findings.length > 0 ||
      meta.recommendations.length > 0;
    if (!hasMeta) return note;
    return `${META_OPEN}${JSON.stringify(meta)}${META_CLOSE}\n${note}`;
  }

  // ── Verdict helpers ──────────────────────────────────────────────
  verdictFor(stepId: number, fieldId: number): Verdict | null {
    return this.verdicts[stepId]?.[fieldId] ?? null;
  }

  setVerdict(stepId: number, fieldId: number, v: Verdict): void {
    if (this.request?.status === 'COMPLETED') return;
    if (!this.verdicts[stepId]) this.verdicts[stepId] = {};
    if (this.verdicts[stepId][fieldId] === v) {
      delete this.verdicts[stepId][fieldId]; // toggle off
    } else {
      this.verdicts[stepId][fieldId] = v;
    }
    this.cdr.detectChanges();
  }

  /** Roll-up of per-question verdicts into a step-level status. */
  stepRollup(step: WorkspaceStep): StepRollup {
    const map = this.verdicts[step.id] ?? {};
    const vals = Object.values(map);
    if (vals.length === 0) return 'pending';
    if (vals.includes('non-conformity')) return 'non-compliant';
    if (vals.includes('observation'))    return 'partial';
    return 'compliant';
  }

  /** Number of questions in the step that have been verdicted. */
  stepVerdictedCount(step: WorkspaceStep): number {
    const map = this.verdicts[step.id] ?? {};
    return Object.keys(map).length;
  }

  /** Overall progress across all steps (% of steps with any verdict). */
  get overallProgress(): number {
    if (!this.steps.length) return 0;
    const touched = this.steps.filter(s => this.stepRollup(s) !== 'pending').length;
    return Math.round((touched / this.steps.length) * 100);
  }

  /** Counts of findings across all steps. */
  get totalFindings(): number {
    return Object.values(this.findings).reduce((n, arr) => n + arr.length, 0);
  }

  get totalRecommendations(): number {
    return Object.values(this.recommendations).reduce((n, arr) => n + arr.length, 0);
  }

  // ── Findings ─────────────────────────────────────────────────────
  currentFindings(): Finding[] {
    const id = this.currentStep?.id;
    if (id == null) return [];
    return this.findings[id] ?? [];
  }

  addFinding(): void {
    if (!this.currentStep || this.request?.status === 'COMPLETED') return;
    const id = this.currentStep.id;
    if (!this.findings[id]) this.findings[id] = [];
    this.findings[id].push({
      id:          'f_' + Date.now().toString(36),
      severity:    'medium',
      description: ''
    });
    this.cdr.detectChanges();
  }

  removeFinding(findingId: string): void {
    const id = this.currentStep?.id;
    if (id == null) return;
    this.findings[id] = (this.findings[id] ?? []).filter(f => f.id !== findingId);
    this.cdr.detectChanges();
  }

  updateFindingSeverity(findingId: string, severity: Finding['severity']): void {
    const id = this.currentStep?.id;
    if (id == null) return;
    const f = (this.findings[id] ?? []).find(x => x.id === findingId);
    if (f) { f.severity = severity; this.cdr.detectChanges(); }
  }

  updateFindingText(findingId: string, text: string): void {
    const id = this.currentStep?.id;
    if (id == null) return;
    const f = (this.findings[id] ?? []).find(x => x.id === findingId);
    if (f) { f.description = text; }
  }

  // ── Recommendations ──────────────────────────────────────────────
  currentRecs(): Recommendation[] {
    const id = this.currentStep?.id;
    if (id == null) return [];
    return this.recommendations[id] ?? [];
  }

  addRecommendation(): void {
    if (!this.currentStep || this.request?.status === 'COMPLETED') return;
    const id = this.currentStep.id;
    if (!this.recommendations[id]) this.recommendations[id] = [];
    this.recommendations[id].push({
      id:          'r_' + Date.now().toString(36),
      description: ''
    });
    this.cdr.detectChanges();
  }

  removeRecommendation(recId: string): void {
    const id = this.currentStep?.id;
    if (id == null) return;
    this.recommendations[id] = (this.recommendations[id] ?? []).filter(r => r.id !== recId);
    this.cdr.detectChanges();
  }

  updateRecText(recId: string, text: string): void {
    const id = this.currentStep?.id;
    if (id == null) return;
    const r = (this.recommendations[id] ?? []).find(x => x.id === recId);
    if (r) { r.description = text; }
  }

  get currentStep(): WorkspaceStep | null {
    return this.steps[this.activeStep] ?? null;
  }

  get currentResult(): AuditStepResult | null {
    if (!this.currentStep) return null;
    return this.results.find(r => r.stepName === this.currentStep!.name) ?? null;
  }

  get currentDraft(): string {
    if (!this.currentStep) return '';
    return this.drafts[this.currentStep.id] ?? '';
  }

  setDraft(val: string): void {
    if (this.currentStep) this.drafts[this.currentStep.id] = val;
  }

  /** Public helper for templates: true if current step has any saveable content. */
  hasCurrentContent(): boolean {
    return !!this.currentStep && this.hasStepContent(this.currentStep.id);
  }

  get allStepsSaved(): boolean {
    if (this.steps.length === 0) return false;
    return this.steps.every(s =>
      this.results.some(r => r.stepName === s.name && r.status === 'SAVED')
    );
  }

 saveDraft(): void { this.persist('DRAFT'); }
 // saveResult(): void { this.persist('SAVED'); }

  deleteCurrentResult(): void {
    const existing = this.currentResult;
    if (!existing || !this.request) return;

    this.resSvc.delete(this.request.id, existing.id).subscribe({
      next: () => {
        this.results = this.results.filter(r => r.id !== existing.id);
        if (this.currentStep) this.drafts[this.currentStep.id] = '';
        this.flash('Result deleted.', false);
        this.cdr.detectChanges();
      }
    });
  }

  stepStatus(step: WorkspaceStep): 'done' | 'draft' | 'active' | 'pending' {
    const r = this.results.find(res => res.stepName === step.name);
    if (r?.status === 'SAVED')  return 'done';
    if (r?.status === 'DRAFT')  return 'draft';
    if (this.steps[this.activeStep]?.id === step.id) return 'active';
    return 'pending';
  }

  openFileViewer(url: string, name: string): void {
    this.viewingFileName = name;
    this.viewingFileUrl  = null;
    this.isLoadingFile   = true;
    this.showFileViewer  = true;

    const token   = this.tokenSvc.getToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.get(url, { headers, responseType: 'blob' }).subscribe({
      next: blob => {
        const obj = URL.createObjectURL(blob);
        this.viewingFileUrl = this.sanitizer.bypassSecurityTrustResourceUrl(obj);
        this.isLoadingFile  = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.viewingFileUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.isLoadingFile  = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeFileViewer(): void {
    this.showFileViewer  = false;
    this.viewingFileUrl  = null;
    this.viewingFileName = null;
  }

  // ── OCR ────────────────────────────────────────────────────────────
  /** Fetch all OCR rows for the audit and index them by mediaId. */
  loadOcr(auditId: number, scheduleNext = true): void {
    const token   = this.tokenSvc.getToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    const url     = `${environment.apiUrl}/audits/${auditId}/ocr`;

    this.http.get<OcrResult[]>(url, { headers }).subscribe({
      next: rows => {
        const map: Record<number, OcrResult> = {};
        for (const r of rows ?? []) map[r.mediaId] = r;
        this.ocrByMedia = map;
        this.cdr.detectChanges();

        if (scheduleNext) this.scheduleOcrPoll(auditId);
      },
      error: () => {
        if (scheduleNext) this.scheduleOcrPoll(auditId);
      }
    });
  }

  /** Re-poll every 4s while any row is PENDING/RUNNING. Stops once all done/failed. */
  private scheduleOcrPoll(auditId: number): void {
    if (this.ocrPollHandle) {
      clearTimeout(this.ocrPollHandle);
      this.ocrPollHandle = null;
    }
    const pending = Object.values(this.ocrByMedia).some(
      r => r.status === 'PENDING' || r.status === 'RUNNING'
    );
    // Always re-poll at least once after first load — fresh submits may not
    // have created rows yet.
    const empty = Object.keys(this.ocrByMedia).length === 0;
    if (pending || empty) {
      this.ocrPollHandle = setTimeout(
        () => this.loadOcr(auditId, true), 4000);
    }
  }

  ocrFor(mediaId: number | undefined | null): OcrResult | null {
    if (mediaId == null) return null;
    return this.ocrByMedia[mediaId] ?? null;
  }

  ocrLabel(r: OcrResult | null): string {
    if (!r) return 'OCR queued';
    switch (r.status) {
      case 'PENDING': return 'OCR pending';
      case 'RUNNING': return 'OCR running';
      case 'DONE':    return `OCR · ${r.pageCount ?? 0}p`;
      case 'FAILED':  return 'OCR failed';
    }
    return 'OCR';
  }

  ocrClass(r: OcrResult | null): string {
    if (!r) return 'ocr-badge ocr-badge--pending';
    switch (r.status) {
      case 'DONE':    return 'ocr-badge ocr-badge--done';
      case 'FAILED':  return 'ocr-badge ocr-badge--failed';
      default:        return 'ocr-badge ocr-badge--pending';
    }
  }

  openOcrViewer(mediaId: number | undefined | null): void {
    const r = this.ocrFor(mediaId);
    if (!r) return;
    this.viewingOcr   = r;
    this.showOcrViewer = true;
  }

  closeOcrViewer(): void {
    this.showOcrViewer = false;
    this.viewingOcr   = null;
  }

  /** Re-queue OCR for a single evidence file. */
  retryOcr(mediaId: number | undefined | null, ev?: Event): void {
    if (ev) ev.stopPropagation();
    if (mediaId == null || !this.request) return;
    const token   = this.tokenSvc.getToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    const url = `${environment.apiUrl}/audits/${this.request.id}/ocr/media/${mediaId}/retry`;
    this.http.post<OcrResult>(url, {}, { headers }).subscribe({
      next: row => {
        this.ocrByMedia[row.mediaId] = row;
        this.cdr.detectChanges();
        if (this.request) this.scheduleOcrPoll(this.request.id);
      }
    });
  }

  /** True when the badge should act as a Retry button (failed or stuck). */
  isRetryable(r: OcrResult | null): boolean {
    return !!r && r.status === 'FAILED';
  }

  getFileUrl(url: string): string {
    return `${this.serverBase}${url}`;
  }

  isImage(name: string): boolean {
    return /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(name);
  }

  toggleBot(): void { this.botOpen = !this.botOpen; }

  // ── Step-aware AI suggestions (heuristic, client-side stub) ──
  get currentSuggestions(): string[] {
    const step = this.currentStep;
    if (!step) return [];
    const name = (step.name || '').toLowerCase();
    const ansCount = this.request?.answers?.length ?? 0;
    const fileCount = this.evidenceFileCount;

    const stepAnsCount = this.currentStepAnswers.length;
    void ansCount;
    const generic = [
      `Review the ${stepAnsCount} answer${stepAnsCount === 1 ? '' : 's'} mapped to this step for completeness.`,
      fileCount > 0
        ? `Cross-check claims against the ${fileCount} attached evidence file${fileCount === 1 ? '' : 's'}.`
        : 'No evidence files attached — request supporting documents.'
    ];

    if (/risk|hazard|threat/.test(name)) {
      return [
        'Identify primary risk drivers in the submitted answers.',
        'Estimate likelihood × impact for each identified risk.',
        ...generic
      ];
    }
    if (/complian|regulat|legal|gdpr|iso/.test(name)) {
      return [
        'Map answers to applicable regulatory clauses.',
        'Flag any unanswered or "NA" responses for follow-up.',
        ...generic
      ];
    }
    if (/evidence|document|record/.test(name)) {
      return [
        'Verify each evidence file is legible and current.',
        'Confirm signatures, dates, and authorship where required.',
        ...generic
      ];
    }
    if (/conclus|final|decision|recommend/.test(name)) {
      return [
        'Summarise findings across the previous steps.',
        'State the conclusion clearly: pass, conditional, or fail.',
        'List actionable recommendations with owners.'
      ];
    }
    return [
      `Focus on the scope of "${step.name}" before drafting.`,
      ...generic
    ];
  }

  // ── Evidence helpers ──
  /**
   * Answers that belong to the current step.
   * If the step carries `fieldIds` (intake-form-based), filter by exact membership.
   * If `fieldIds` is empty (process-step fallback / default step), show all answers.
   */
  get currentStepAnswers() {
    const answers = this.request?.answers ?? [];
    const step    = this.currentStep;
    if (!step) return [];
    if (!step.fieldIds || step.fieldIds.length === 0) return answers;
    const set = new Set(step.fieldIds);
    return answers.filter(a => set.has(a.fieldId));
  }

  get evidenceFileCount(): number {
    let total = 0;
    for (const a of this.currentStepAnswers) {
      if (a.fileMedia) total++;
      if (a.files?.length) total += a.files.length;
    }
    return total;
  }

  // ── Quick-insert chip → append snippet to current draft ──
  insertSnippet(label: string): void {
    if (!this.currentStep || this.request?.status === 'COMPLETED') return;
    const map: Record<string, string> = {
      'Compliance OK':       '\n\n✓ Compliance assessment: Requirements met. ',
      'Risk identified':     '\n\n⚠ Risk identified: ',
      'Needs more evidence': '\n\n✱ Additional evidence required: ',
      'Recommendation':      '\n\n→ Recommendation: '
    };
    const snippet = map[label] ?? `\n\n${label}: `;
    const current = this.currentDraft || '';
    this.drafts[this.currentStep.id] = (current + snippet).replace(/^\n+/, '');
    this.cdr.detectChanges();
  }

  goBack(): void {
    this.router.navigate(
      this.isAuditorMode ? ['/auditor/audits'] : ['/admin/audits']
    );
  }

  revertToDraft(): void {
    const existing = this.currentResult;
    if (!existing || !this.request) return;
    this.resSvc.update(this.request.id, existing.id, {
      description: existing.description,
      status:      'DRAFT'
    }).subscribe({
      next: result => {
        const idx = this.results.findIndex(r => r.id === result.id);
        if (idx !== -1) this.results[idx] = result;
        this.flash('Reverted to draft.', false);
        this.cdr.detectChanges();
      }
    });
  }

  private flash(msg: string, isError: boolean): void {
    if (isError) {
      this.saveError = msg;
      setTimeout(() => { this.saveError = ''; this.cdr.detectChanges(); }, 3500);
    } else {
      this.saveMsg = msg;
      setTimeout(() => { this.saveMsg = ''; this.cdr.detectChanges(); }, 3500);
    }
  }


  async goToStep(idx: number): Promise<void> {
  if (!this.request || this.request?.status === 'COMPLETED') {
    this.activeStep = idx;
    return;
  }
  // Auto-save current step as DRAFT if there's any content or meta
  if (this.currentStep && this.hasStepContent(this.currentStep.id)) {
    await this.persistAsync('DRAFT');
  }
  this.activeStep = idx;
  this.cdr.detectChanges();
}

/** True if the step has either a note or any verdict/finding/rec. */
private hasStepContent(stepId: number): boolean {
  if ((this.drafts[stepId] ?? '').trim()) return true;
  if (Object.keys(this.verdicts[stepId] ?? {}).length) return true;
  if ((this.findings[stepId] ?? []).length) return true;
  if ((this.recommendations[stepId] ?? []).length) return true;
  return false;
}

private persistAsync(status: 'DRAFT' | 'SAVED'): Promise<void> {
  return new Promise((resolve) => {
    if (!this.request || !this.currentStep) { resolve(); return; }
    const step     = this.currentStep;
    const desc     = this.composeStepBody(step.id);
    const existing = this.currentResult;

    if (!this.hasStepContent(step.id)) { resolve(); return; }

    const payload = {
      processStepId: -1,
      stepName:      step.name,
      description:   desc,
      status
    };

    const obs = existing
      ? this.resSvc.update(this.request.id, existing.id, payload)
      : this.resSvc.saveOrUpdate(this.request.id, payload);

    obs.subscribe({
      next: result => {
        const idx = this.results.findIndex(r => r.id === result.id);
        if (idx !== -1) this.results[idx] = result;
        else this.results.push(result);
        this.cdr.detectChanges();
        resolve();
      },
      error: () => resolve()
    });
  });
}

private persist(status: 'DRAFT' | 'SAVED'): void {
  if (!this.request || !this.currentStep) return;
  this.isSaving = true;
  const step     = this.currentStep;
  const desc     = this.composeStepBody(step.id);
  const existing = this.currentResult;

  const payload = {
    processStepId: -1,
    stepName:      step.name,
    description:   desc,
    status
  };

  const obs = existing
    ? this.resSvc.update(this.request.id, existing.id, payload)
    : this.resSvc.saveOrUpdate(this.request.id, payload);

  obs.subscribe({
    next: result => {
      const idx = this.results.findIndex(r => r.id === result.id);
      if (idx !== -1) this.results[idx] = result;
      else this.results.push(result);
      this.isSaving = false;
      this.flash(status === 'DRAFT' ? 'Draft saved.' : 'Step saved.', false);
      this.cdr.detectChanges();
    },
    error: () => {
      this.isSaving = false;
      this.flash('Failed to save.', true);
    }
  });
}

// ★ Submit: save ALL steps with content, then complete
submitAudit(): void {
  if (!this.request) return;
  this.isCompleting = true;

  // Save all steps that have content as SAVED
  const savePromises = this.steps
    .filter(s => this.hasStepContent(s.id))
    .map(s => new Promise<void>((resolve) => {
      const existing = this.results.find(r => r.stepName === s.name);
      const payload = {
        processStepId: -1,
        stepName:      s.name,
        description:   this.composeStepBody(s.id),
        status:        'SAVED' as const
      };
      const obs = existing
        ? this.resSvc.update(this.request!.id, existing.id, payload)
        : this.resSvc.saveOrUpdate(this.request!.id, payload);
      obs.subscribe({ next: result => {
        const idx = this.results.findIndex(r => r.id === result.id);
        if (idx !== -1) this.results[idx] = result;
        else this.results.push(result);
        resolve();
      }, error: () => resolve() });
    }));

  Promise.all(savePromises).then(() => {
    const complete$ = this.isAuditorMode
      ? this.reqSvc.auditorCompleteAudit(this.request!.id)
      : this.reqSvc.adminCompleteRequest(this.request!.id);

    complete$.subscribe({
      next: updated => {
        this.request      = updated;
        this.isCompleting = false;
        this.flash('Audit completed successfully!', false);
        setTimeout(() => this.goBack(), 1500);
        this.cdr.detectChanges();
      },
      error: () => {
        this.isCompleting = false;
        this.flash('Failed to complete audit.', true);
      }
    });
  });
}
}