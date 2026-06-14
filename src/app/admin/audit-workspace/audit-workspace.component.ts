import {
  Component, OnInit, OnDestroy, inject, ChangeDetectorRef, ViewChild, ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatComponent } from '../../features/chat/chat.component';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl, SafeHtml } from '@angular/platform-browser';

import { AuditRequestService } from '../../services/audit-request.service';
import { AuditFormService } from '../../services/audit-form.service';
import { AuditProcessStepService, AuditProcessStep } from '../../services/audit-process-step.service';
import { AuditStepResultService, AuditStepResult } from '../../services/audit-step-result.service';
import { TokenService } from '../../shared/token.service';
import { AuditBotComponent } from '../../layout/audit-bot/audit-bot.component';
import { AuditRequest , AuditRequestAnswer } from '../../models/audit.model';
import { environment } from '../../environments/environment';
import { RicsSearchService, RicsRuleResult, RicsEvidenceItem, RelevanceSegment } from '../../services/rics-search.service';
import { log } from '../../core/logging/log';

const LOG = 'ng.audit-workspace';
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

export interface RicsClauseRef {
  ruleId:        string;
  section?:      string;
  shortTitle?:   string;
  requirement?:  string;   // short snippet for tooltip / context
}

export interface Finding {
  id:          string;
  severity:    'low' | 'medium' | 'high' | 'critical';
  description: string;
  clauses?:    RicsClauseRef[];
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
  showChat = false;

  toggleChat(): void {
    this.showChat = !this.showChat;
  }
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

  // ── Phase 3: unified Evidence Drawer ───────────────────────────────
  showDrawer       = false;
  drawerMedia:     { id: number; url: string; name: string } | null = null;
  drawerMediaIndex = 0;
  drawerQuery      = '';   // the audit question this evidence is meant to prove
  drawerTab:       'preview' | 'ocr' = 'preview';
  drawerLoading    = false;
  drawerBlobUrl:   SafeResourceUrl | null = null;
  private drawerObjectUrl: string | null = null;

  // ── Phase 4: RICS clause picker (per-finding) ──────────────────────
  pickerOpenFor: string | null = null;     // finding.id currently picking
  pickerQuery   = '';
  pickerLoading = false;
  pickerResults: RicsRuleResult[] = [];
  private pickerDebounce: any = null;

  // ── Phase 5: dismissed AI suggestions (per-step, session-only) ─────
  dismissedSuggestions: Record<number, Set<string>> = {};

  // ── Grounded AI suggestions fetched from the RAG /rules/evidence-check ──
  private aiSuggestions: Record<number, string[]> = {};
  private aiSuggestRequested = new Set<number>();
  aiSuggestLoading: Record<number, boolean> = {};

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
  private ricsSvc   = inject(RicsSearchService);

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
    this.releaseDrawerBlob();
  }

  loadRequest(id: number): void {
    const request$ = this.isAuditorMode
      ? this.reqSvc.auditorGetAuditById(id)
      : this.reqSvc.adminGetRequest(id);

    log.info(LOG, 'open audit', { id, mode: this.isAuditorMode ? 'auditor' : 'admin' });
    request$.subscribe({
      next: req => {
        this.request = req;
        log.info(LOG, 'audit loaded', { id, type: req.auditType, status: req.status });

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
          // Only surface steps that actually carry a mapped customer response —
          // the auditor/admin workspace shouldn't list empty intake sections.
          this.steps = formSteps
            .map(s => ({
              id:        s.id,
              name:      s.title,
              stepOrder: s.stepOrder,
              fieldIds:  (s.fields ?? []).map(f => f.id)
            }))
            .filter(s => this.answersForStep(s).length > 0);

          if (this.steps.length === 0) {
            // Template exists but nothing mapped — fall back so the workspace
            // isn't left empty.
            this.loadStepsByAuditType(auditType);
            return;
          }
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
        this.ensureAiSuggestions(this.currentStep);
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
          this.ensureAiSuggestions(this.currentStep);
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
      log.info(LOG, 'verdict cleared', { stepId, fieldId });
    } else {
      this.verdicts[stepId][fieldId] = v;
      log.info(LOG, 'verdict set', { stepId, fieldId, verdict: v });
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

  /** Friendly evidence label: temp UUID filenames become "Evidence N". */
  displayName(name: string | undefined | null, index = 0): string {
    if (!name) return `Evidence ${index}`;
    // UUID pattern: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (filenames are prefixed with one)
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i.test(name);
    return isUuid ? `Evidence ${index}` : name;
  }

  ocrLabel(r: OcrResult | null): string {
    if (!r) return 'Text queued';
    switch (r.status) {
      case 'PENDING': return 'Extracting…';
      case 'RUNNING': return 'Extracting…';
      case 'DONE':    return `Extracted text · ${r.pageCount ?? 0}p`;
      case 'FAILED':  return 'Extraction failed';
    }
    return 'Extracted text';
  }

  /** Fuller tooltip for the evidence text badge. */
  ocrTitle(r: OcrResult | null): string {
    if (!r) return 'Text extraction is queued for this document';
    switch (r.status) {
      case 'PENDING':
      case 'RUNNING': return 'Reading the document text…';
      case 'DONE':    return `Click to read the text extracted from this document (${r.pageCount ?? 0} page${r.pageCount === 1 ? '' : 's'})`;
      case 'FAILED':  return 'Text extraction failed — click Retry';
    }
    return 'View extracted text';
  }

  // ── Evidence relevance highlight (OCR text tab) ──────────────────
  /** Toggle for shading question-relevant lines in the extracted text. */
  ocrHighlightOn = true;
  ocrRelevanceLoading = false;
  ocrRelevanceFailed  = false;   // drives keyword fallback
  private _ocrHlCache: { key: string; html: SafeHtml } | null = null;
  private ocrRelevance: Record<string, RelevanceSegment[]> = {};
  private ocrRelevanceTruncated: Record<string, boolean> = {};
  private ocrRelevanceRequested = new Set<string>();
  @ViewChild('ocrTextEl') private ocrTextEl?: ElementRef<HTMLElement>;

  private relevanceKey(mediaId: number, query: string): string {
    return `${mediaId}|${query}`;
  }

  /** The relevance target for the open evidence: the answered question. */
  private get drawerRelevanceQuery(): string {
    return (this.drawerQuery || this.currentStep?.name || '').trim();
  }

  /**
   * Fetch sentence-level semantic relevance for the open evidence against its
   * audit question. Idempotent per (file, question); on failure the renderer
   * silently falls back to keyword highlighting.
   */
  ensureRelevance(): void {
    const media = this.drawerMedia;
    const o = this.drawerOcr();
    const query = this.drawerRelevanceQuery;
    if (!this.ocrHighlightOn || !media || !o?.rawText || !query) return;

    const key = this.relevanceKey(media.id, query);
    if (this.ocrRelevanceRequested.has(key)) return;
    this.ocrRelevanceRequested.add(key);
    this.ocrRelevanceLoading = true;
    this.ocrRelevanceFailed  = false;
    log.info(LOG, 'score relevance', { mediaId: media.id, textChars: o.rawText.length });
    this.ricsSvc.relevanceHighlight(o.rawText, query).subscribe({
      next: res => {
        this.ocrRelevance[key] = res?.segments ?? [];
        this.ocrRelevanceTruncated[key] = !!res?.truncated;
        this.ocrRelevanceLoading = false;
        this._ocrHlCache = null;            // bust render cache
        log.info(LOG, 'relevance scored', { segments: res?.segments?.length ?? 0, truncated: !!res?.truncated });
        this.cdr.detectChanges();
      },
      error: () => {
        this.ocrRelevanceLoading = false;
        this.ocrRelevanceFailed  = true;    // renderer uses keyword fallback
        log.warn(LOG, 'relevance unavailable — using keyword fallback', { mediaId: media.id });
        this.ocrRelevanceRequested.delete(key);
        this._ocrHlCache = null;
        this.cdr.detectChanges();
      }
    });
  }

  /** True once semantic segments are available for the open evidence. */
  get hasSemanticRelevance(): boolean {
    const media = this.drawerMedia;
    if (!media) return false;
    return (this.ocrRelevance[this.relevanceKey(media.id, this.drawerRelevanceQuery)]?.length ?? 0) > 0;
  }

  /** Number of "most relevant" (strong) sentences in the open evidence. */
  get strongRelevanceCount(): number {
    const media = this.drawerMedia;
    if (!media) return 0;
    const segs = this.ocrRelevance[this.relevanceKey(media.id, this.drawerRelevanceQuery)] ?? [];
    return segs.filter(s => s.level >= 2).length;
  }

  /** True when the document was too long and was scored only up to the cap. */
  get relevanceTruncated(): boolean {
    const media = this.drawerMedia;
    return !!media && !!this.ocrRelevanceTruncated[this.relevanceKey(media.id, this.drawerRelevanceQuery)];
  }

  /** Scroll the extracted-text pane to the first strongly-relevant line. */
  jumpToMostRelevant(): void {
    const el = this.ocrTextEl?.nativeElement?.querySelector('.ocr-hl--2');
    el?.scrollIntoView({ block: 'center', behavior: 'smooth' });
  }

  /**
   * Render the OCR text with the most question-relevant sentences shaded.
   * Prefers semantic segments (offsets from the RAG service); falls back to
   * keyword marking while loading or if the relevance service is unreachable.
   */
  highlightedOcrText(text: string): SafeHtml {
    const media = this.drawerMedia;
    const query = this.drawerRelevanceQuery;
    const segs = media ? this.ocrRelevance[this.relevanceKey(media.id, query)] : undefined;
    const mode = segs && segs.length ? 'sem' : 'kw';
    const key = `${media?.id ?? ''}|${mode}|${text.length}|${query}`;
    if (this._ocrHlCache?.key === key) return this._ocrHlCache.html;

    const html = segs && segs.length
      ? this.buildSemanticHtml(text, segs)
      : this.buildKeywordHtml(text);
    const safe = this.sanitizer.bypassSecurityTrustHtml(html);
    this._ocrHlCache = { key, html: safe };
    return safe;
  }

  /** Wrap the highest-scoring sentence spans (by char offset) in <mark>. */
  private buildSemanticHtml(text: string, segs: RelevanceSegment[]): string {
    const marks = segs.filter(s => s.level > 0).sort((a, b) => a.start - b.start);
    let out = '';
    let cursor = 0;
    for (const s of marks) {
      const start = Math.max(s.start, cursor);
      if (start > cursor) out += this.escapeHtml(text.slice(cursor, start));
      if (s.end > start) {
        out += `<mark class="ocr-hl ocr-hl--${s.level}">${this.escapeHtml(text.slice(start, s.end))}</mark>`;
        cursor = s.end;
      }
    }
    if (cursor < text.length) out += this.escapeHtml(text.slice(cursor));
    return out;
  }

  /** Offline fallback: mark question keywords in the text. */
  private buildKeywordHtml(text: string): string {
    const keywords = this.relevanceKeywords();
    let html = this.escapeHtml(text);
    if (keywords.length) {
      const pattern = keywords
        .slice()
        .sort((a, b) => b.length - a.length)
        .map(k => k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
        .join('|');
      const re = new RegExp(`\\b(${pattern})\\b`, 'gi');
      html = html.replace(re, '<mark class="ocr-hl ocr-hl--1">$1</mark>');
    }
    return html;
  }

  /** Meaningful terms from the step name + its question labels. */
  private relevanceKeywords(): string[] {
    const stop = new Set([
      'the','and','for','that','with','this','have','are','was','from','your','firm',
      'must','any','all','has','been','which','their','they','will','into','such','when',
      'where','what','does','did','not','but','can','may','should','about','these','those',
      'each','per','via','also','use','used','using','staff','firms','members','responsible'
    ]);
    const src = [
      this.currentStep?.name ?? '',
      ...this.currentStepAnswers.map((a: any) => a?.fieldLabel ?? '')
    ].join(' ').toLowerCase();
    const words = src.match(/[a-z][a-z-]{3,}/g) ?? [];
    return Array.from(new Set(words.filter(w => !stop.has(w))));
  }

  private escapeHtml(s: string): string {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
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

  // ── Phase 3: Evidence Drawer ──────────────────────────────────────
  /**
   * Open the unified evidence drawer for a file: shows preview (image / PDF /
   * generic) + OCR text + one-click "Link to finding". Replaces the older
   * separate file viewer + OCR modal flows for evidence chips.
   */
  openEvidence(media: { id: number; url: string; name: string },
               index = 0,
               query = '',
               tab: 'preview' | 'ocr' = 'preview'): void {
    log.info(LOG, 'open evidence', { name: this.displayName(media.name, index), tab });
    this.releaseDrawerBlob();
    this.drawerMedia      = media;
    this.drawerMediaIndex = index;
    this.drawerQuery      = query;
    this.drawerTab     = tab;
    this.showDrawer    = true;
    this.drawerLoading = true;
    this.drawerBlobUrl = null;
    if (tab === 'ocr') this.ensureRelevance();

    const token   = this.tokenSvc.getToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    const full    = this.getFileUrl(media.url);
    console.log('Fetching file:', full);

    this.http.get(full, { headers, responseType: 'blob' }).subscribe({
      next: blob => {
        console.log('Blob received, type:', blob.type, 'size:', blob.size);
        const obj = URL.createObjectURL(blob);
        this.drawerObjectUrl = obj;
        this.drawerBlobUrl   = this.sanitizer.bypassSecurityTrustResourceUrl(obj);
        this.drawerLoading   = false;
        this.cdr.detectChanges();
      },
      error: () => {
        console.error('Failed to fetch file:', Error);

        this.drawerBlobUrl = this.sanitizer.bypassSecurityTrustResourceUrl(full);
        this.drawerLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeDrawer(): void {
    this.showDrawer = false;
    this.drawerMedia = null;
    this.drawerBlobUrl = null;
    this.releaseDrawerBlob();
  }

  setDrawerTab(t: 'preview' | 'ocr'): void {
    this.drawerTab = t;
    if (t === 'ocr') this.ensureRelevance();
  }

  /** Toggle relevance shading; fetch semantic scores the first time it's on. */
  toggleOcrHighlight(): void {
    this.ocrHighlightOn = !this.ocrHighlightOn;
    this._ocrHlCache = null;
    if (this.ocrHighlightOn) this.ensureRelevance();
  }

  private releaseDrawerBlob(): void {
    if (this.drawerObjectUrl) {
      try { URL.revokeObjectURL(this.drawerObjectUrl); } catch { /* noop */ }
      this.drawerObjectUrl = null;
    }
  }

  drawerOcr(): OcrResult | null {
    return this.drawerMedia ? this.ocrFor(this.drawerMedia.id) : null;
  }

  /** Seed a new Finding on the current step pre-filled with evidence + OCR snippet. */
  linkEvidenceToFinding(): void {
    if (!this.drawerMedia || !this.currentStep) return;
    if (this.request?.status === 'COMPLETED') return;

    const ocr     = this.drawerOcr();
    const snippet = (ocr?.rawText || '').trim().slice(0, 280);
    const tail    = snippet && (ocr?.rawText || '').length > 280 ? '…' : '';

    this.addFinding();
    const list = this.findings[this.currentStep.id] ?? [];
    const last = list[list.length - 1];
    if (last) {
      last.description = `Evidence: ${this.displayName(this.drawerMedia.name, this.drawerMediaIndex)}` +
        (snippet ? `\n\nOCR excerpt:\n${snippet}${tail}` : '');
    }
    this.flash('Finding seeded from evidence.', false);
    this.cdr.detectChanges();
  }

  // ── Phase 4: RICS clause picker ──────────────────────────────────
  openClausePicker(findingId: string): void {
    if (this.request?.status === 'COMPLETED') return;
    this.pickerOpenFor = findingId;
    this.pickerQuery   = '';
    this.pickerResults = [];
    // Pre-seed with the finding's own description or the step name for context.
    const stepId = this.currentStep?.id;
    if (stepId != null) {
      const f = (this.findings[stepId] ?? []).find(x => x.id === findingId);
      const seed = (f?.description || this.currentStep?.name || '').slice(0, 200);
      if (seed.trim().length >= 3) {
        this.pickerQuery = seed;
        this.runClauseSearch();
      }
    }
  }

  closeClausePicker(): void {
    this.pickerOpenFor = null;
    this.pickerQuery   = '';
    this.pickerResults = [];
    if (this.pickerDebounce) { clearTimeout(this.pickerDebounce); this.pickerDebounce = null; }
  }

  onPickerQueryChange(): void {
    if (this.pickerDebounce) clearTimeout(this.pickerDebounce);
    this.pickerDebounce = setTimeout(() => this.runClauseSearch(), 280);
  }

  runClauseSearch(): void {
    const q = this.pickerQuery.trim();
    if (q.length < 3) { this.pickerResults = []; return; }
    this.pickerLoading = true;
    this.ricsSvc.search(q, 8).subscribe({
      next: res => {
        this.pickerResults = res?.results ?? [];
        this.pickerLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.pickerResults = [];
        this.pickerLoading = false;
        this.flash('RICS search unavailable.', true);
        this.cdr.detectChanges();
      }
    });
  }

  attachClause(findingId: string, hit: RicsRuleResult): void {
    const stepId = this.currentStep?.id;
    if (stepId == null) return;
    const f = (this.findings[stepId] ?? []).find(x => x.id === findingId);
    if (!f) return;
    if (!f.clauses) f.clauses = [];
    if (f.clauses.some(c => c.ruleId === hit.rule_id)) return; // dedupe
    f.clauses.push({
      ruleId:      hit.rule_id,
      section:     hit.payload?.section,
      shortTitle:  hit.payload?.short_title,
      requirement: (hit.payload?.requirement_text || '').slice(0, 200),
    });
    this.cdr.detectChanges();
  }

  detachClause(findingId: string, ruleId: string): void {
    const stepId = this.currentStep?.id;
    if (stepId == null) return;
    const f = (this.findings[stepId] ?? []).find(x => x.id === findingId);
    if (!f?.clauses) return;
    f.clauses = f.clauses.filter(c => c.ruleId !== ruleId);
    this.cdr.detectChanges();
  }

  // ── Phase 5: AI suggestion actions ────────────────────────────────
  suggestionToFinding(text: string): void {
    if (!this.currentStep || this.request?.status === 'COMPLETED') return;
    this.addFinding();
    const list = this.findings[this.currentStep.id] ?? [];
    const last = list[list.length - 1];
    if (last) last.description = text;
    this.dismissSuggestion(text);
    this.flash('Suggestion converted to finding.', false);
    this.cdr.detectChanges();
  }

  suggestionToRecommendation(text: string): void {
    if (!this.currentStep || this.request?.status === 'COMPLETED') return;
    this.addRecommendation();
    const list = this.recommendations[this.currentStep.id] ?? [];
    const last = list[list.length - 1];
    if (last) last.description = text;
    this.dismissSuggestion(text);
    this.flash('Suggestion converted to recommendation.', false);
    this.cdr.detectChanges();
  }

  dismissSuggestion(text: string): void {
    const id = this.currentStep?.id;
    if (id == null) return;
    if (!this.dismissedSuggestions[id]) this.dismissedSuggestions[id] = new Set();
    this.dismissedSuggestions[id].add(text);
    this.cdr.detectChanges();
  }

  isDismissed(text: string): boolean {
    const id = this.currentStep?.id;
    if (id == null) return false;
    return !!this.dismissedSuggestions[id]?.has(text);
  }

  getFileUrl(url: string): string {
    return `${this.serverBase}${url}`;
  }

  isImage(name: string): boolean {
    return /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(name);
  }

  toggleBot(): void { this.botOpen = !this.botOpen; }

  /**
   * Assemble a compact, text-only snapshot of the audit under review for the
   * AI assistant: the current step, its customer answers, and any
   * OCR-extracted evidence text. Passed to <app-audit-bot> so the assistant
   * can summarise evidence and reason about compliance for THIS audit rather
   * than answering generically.
   */
  buildAiContext(): string {
    const r = this.request;
    const step = this.currentStep;
    const lines: string[] = [];

    if (r) {
      const company =
        r.submittedBy?.companyInfo?.companyName ||
        [r.submittedBy?.firstName, r.submittedBy?.lastName].filter(Boolean).join(' ') ||
        'Unknown company';
      lines.push(`Audit #${r.id} — ${company} — type ${r.auditType ?? 'N/A'} — status ${r.status ?? 'N/A'}.`);
    }
    if (step) lines.push(`Current step under review: ${step.name}.`);

    const answers = this.currentStepAnswers;
    if (!answers.length) {
      lines.push('No customer answers are mapped to this step.');
      return lines.join('\n');
    }

    answers.forEach((a, i) => {
      const q = (a.fieldLabel || `Question ${i + 1}`).trim();
      const ans = (a.answerValue || '(no answer provided)').toString().trim();
      lines.push(`\nQ${i + 1}. ${q}\nAnswer: ${ans}`);

      const media = [a.fileMedia, ...(a.files ?? [])].filter(Boolean);
      media.forEach((m: any, mi: number) => {
        const name = this.displayName(m?.name, mi + 1);
        const ocr = this.ocrFor(m?.id);
        if (ocr?.status === 'DONE' && ocr.rawText?.trim()) {
          lines.push(`Evidence "${name}" — extracted text:\n${ocr.rawText.trim().slice(0, 3000)}`);
        } else if (ocr) {
          lines.push(`Evidence "${name}": OCR ${ocr.status.toLowerCase()}, no text available yet.`);
        } else {
          lines.push(`Evidence "${name}": attached (no extracted text).`);
        }
      });
    });

    const ctx = lines.join('\n');
    return ctx.length > 20000 ? ctx.slice(0, 20000) + '\n…(truncated)' : ctx;
  }

  /** Bound provider handed to the assistant so it always reads fresh state. */
  aiContextProvider = (): string => this.buildAiContext();

  /** Push an assistant reply into the current step as a finding, recommendation, or note. */
  onAiInsert(e: { target: 'finding' | 'recommendation' | 'note'; text: string }): void {
    const text = (e?.text || '').trim();
    if (!text || !this.currentStep || this.request?.status === 'COMPLETED') return;
    const stepId = this.currentStep.id;
    log.info(LOG, 'insert from AI assistant', { target: e.target, chars: text.length });

    if (e.target === 'finding') {
      this.addFinding();
      const list = this.findings[stepId] ?? [];
      const last = list[list.length - 1];
      if (last) last.description = text;
      this.flash('Added to findings from AI assistant.', false);
    } else if (e.target === 'recommendation') {
      this.addRecommendation();
      const list = this.recommendations[stepId] ?? [];
      const last = list[list.length - 1];
      if (last) last.description = text;
      this.flash('Added to recommendations from AI assistant.', false);
    } else {
      const current = this.drafts[stepId] || '';
      this.drafts[stepId] = (current ? `${current}\n\n${text}` : text).trim();
      this.flash('Inserted into step note.', false);
    }
    this.cdr.detectChanges();
  }

  // ── Step-aware AI suggestions (heuristic, client-side stub) ──
  /**
   * Suggestions shown in the "AI Suggestions" box. Prefers grounded hints
   * fetched from the RAG knowledge base (RICS evidence-check, cited to clause
   * IDs); falls back to lightweight heuristics while loading or if the RAG
   * service is unreachable.
   */
  get currentSuggestions(): string[] {
    const step = this.currentStep;
    if (!step) return [];
    const fetched = this.aiSuggestions[step.id];
    if (fetched && fetched.length) return fetched;
    return this.heuristicSuggestions(step);
  }

  get suggestionsLoading(): boolean {
    const step = this.currentStep;
    return !!step && !!this.aiSuggestLoading[step.id] && !this.aiSuggestions[step.id]?.length;
  }

  /**
   * Fetch grounded, clause-cited suggestions for a step from the RAG service.
   * Idempotent per step; a failed/empty fetch silently keeps the heuristics.
   */
  private ensureAiSuggestions(step: WorkspaceStep | null): void {
    if (!step || step.id == null || this.aiSuggestRequested.has(step.id)) return;
    this.aiSuggestRequested.add(step.id);
    this.aiSuggestLoading[step.id] = true;
    this.ricsSvc.evidenceCheck(step.name, 4).subscribe({
      next: res => {
        const sugg = (res?.evidence ?? [])
          .map(e => this.formatSuggestion(e))
          .filter((s): s is string => !!s);
        if (sugg.length) this.aiSuggestions[step.id] = sugg;
        this.aiSuggestLoading[step.id] = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.aiSuggestLoading[step.id] = false;
        this.cdr.detectChanges();
      }
    });
  }

  private formatSuggestion(e: RicsEvidenceItem): string {
    const what = (e.evidence_implied || e.requirement_text || '').trim();
    if (!what) return '';
    const cite = e.rule_id
      ? ` (${e.rule_id}${e.section ? ' · §' + e.section : ''})`
      : '';
    return `${what}${cite}`;
  }

  private heuristicSuggestions(step: WorkspaceStep): string[] {
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
    return this.currentStep ? this.answersForStep(this.currentStep) : [];
  }

  /**
   * Resolve the customer answers that belong to a given step, pairing main
   * answers with their supporting notes and evidence files. Returns an empty
   * array when nothing is mapped — used both to render a step and to decide
   * whether the step should appear in the workspace at all.
   */
  answersForStep(step: WorkspaceStep) {
  const answers = this.request?.answers ?? [];
  if (!step) return [];
  if (!step.fieldIds || step.fieldIds.length === 0) return [];

  // Index answers by fieldId so we can walk the step's fields in TEMPLATE
  // order. Each L2 question is laid out as consecutive fields — the radio
  // question, its supporting-note textarea, then its evidence FILE field — so
  // grouping by that order attaches each note/evidence to the question it
  // actually belongs to. The previous logic filtered the answers into three
  // arrays and zipped them by index, which shifted a file onto the wrong
  // question whenever an earlier question had no evidence (e.g. Q3's file
  // showing under Q1).
  const byField = new Map<number, any>();
  for (const a of answers) byField.set(a.fieldId, a);

  const classify = (ans: any): 'evidence' | 'note' | 'main' => {
    const label = (ans.fieldLabel || '').toLowerCase();
    if (label === 'evidence files') return 'evidence';
    if (label.includes('response') || label.includes('supporting')) return 'note';
    return 'main';
  };
  const mergeFiles = (target: any, src: any) => {
    if (src.fileMedia && !target.fileMedia) target.fileMedia = src.fileMedia;
    if (src.files?.length) target.files = [...(target.files || []), ...src.files];
  };

  const result: any[] = [];
  let current: any = null;

  for (const fieldId of step.fieldIds) {
    const ans = byField.get(fieldId);
    if (!ans) continue;
    const kind = classify(ans);
    if (kind === 'main' || !current) {
      // Start a new question block. (A leading note/evidence with no preceding
      // main is kept as its own block rather than silently dropped.)
      current = { ...ans };
      result.push(current);
    } else if (kind === 'note') {
      if (ans.answerValue && !current.answerValue?.includes(ans.answerValue)) {
        current.answerValue = `${current.answerValue || ''}\n\n**Supporting note:** ${ans.answerValue}`;
      }
      mergeFiles(current, ans);
    } else {
      mergeFiles(current, ans);
    }
  }

  return result;
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
    this.ensureAiSuggestions(this.currentStep);
    return;
  }
  // Auto-save current step as DRAFT if there's any content or meta
  if (this.currentStep && this.hasStepContent(this.currentStep.id)) {
    await this.persistAsync('DRAFT');
  }
  this.activeStep = idx;
  this.ensureAiSuggestions(this.currentStep);
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
  log.info(LOG, 'submit audit', { id: this.request.id, steps: this.steps.length });
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
