import {
  Component, OnInit, inject, ChangeDetectorRef
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

@Component({
  selector: 'app-audit-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule, AuditBotComponent],
  templateUrl: './audit-workspace.component.html',
  styleUrl: './audit-workspace.component.scss'
})
export class AuditWorkspaceComponent implements OnInit {

  request:     AuditRequest | null = null;
  steps:       AuditProcessStep[]  = [];
  results:     AuditStepResult[]   = [];
  activeStep   = 0;
  botOpen      = false;
  isSaving     = false;
  isCompleting = false;
  saveMsg      = '';
  saveError    = '';

  drafts: Record<number, string> = {};

  showFileViewer   = false;
  viewingFileUrl:  SafeResourceUrl | null = null;
  viewingFileName: string | null = null;
  isLoadingFile    = false;

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

        // ★ KEY FIX: AuditRequest has no FK to AuditFormTemplate
        // Use auditType to find the template, then load its process steps
        this.loadStepsByAuditType(req.auditType);

        this.loadResults(id);
        this.cdr.detectChanges();
      },
      error: () => this.flash('Failed to load audit request.', true)
    });
  }

  // ★ Uses auditType → getTemplateByType → getSteps
  // This is the correct approach since AuditRequest only stores auditType
  // REPLACE loadStepsByAuditType with this simpler direct call:
private loadStepsByAuditType(auditType: string): void {
  this.reqSvc.getProcessStepsByAuditType(auditType, this.isAuditorMode).subscribe({
    next: steps => {
      const real = steps.filter((s: any) => s.id > 0 && !s.isDefault);
      if (real.length > 0) {
        this.steps = real;
        this.steps.forEach(s => {
          if (this.drafts[s.id] === undefined) this.drafts[s.id] = '';
        });
      } else {
        this.setDefaultStep();
      }
      this.cdr.detectChanges();
    },
    error: () => this.setDefaultStep()
  });
}

  private setDefaultStep(): void {
    this.steps    = [{ id: -1, name: 'Audit Review', stepOrder: 1, isDefault: true }];
    this.drafts[-1] = '';
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
        results.forEach(r => {
          const key = r.processStep?.id ?? -1;
          this.drafts[key] = r.description ?? '';
        });
        this.cdr.detectChanges();
      }
    });
  }

  get currentStep(): AuditProcessStep | null {
    return this.steps[this.activeStep] ?? null;
  }

  get currentResult(): AuditStepResult | null {
    if (!this.currentStep) return null;
    return this.results.find(r =>
      (r.processStep?.id ?? -1) === this.currentStep!.id
    ) ?? null;
  }

  get currentDraft(): string {
    if (!this.currentStep) return '';
    return this.drafts[this.currentStep.id] ?? '';
  }

  setDraft(val: string): void {
    if (this.currentStep) this.drafts[this.currentStep.id] = val;
  }

  get allStepsSaved(): boolean {
    if (this.steps.length === 0) return false;
    return this.steps.every(s =>
      this.results.some(r =>
        (r.processStep?.id ?? -1) === s.id && r.status === 'SAVED'
      )
    );
  }

  saveDraft(): void  { this.persist('DRAFT'); }
  saveResult(): void { this.persist('SAVED'); }

  private persist(status: 'DRAFT' | 'SAVED'): void {
    if (!this.request || !this.currentStep) return;
    this.isSaving = true;

    const desc     = this.currentDraft;
    const existing = this.currentResult;
    const step     = this.currentStep;

    const payload = {
      processStepId: step.id > 0 ? step.id : -1,
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

  submitAudit(): void {
    if (!this.request) return;
    this.isCompleting = true;

    const saveObs = this.currentDraft.trim()
      ? new Promise<void>((resolve) => {
          const step     = this.currentStep!;
          const existing = this.currentResult;
          const payload  = {
            processStepId: step.id > 0 ? step.id : -1,
            stepName:      step.name,
            description:   this.currentDraft,
            status:        'SAVED' as const
          };
          const obs = existing
            ? this.resSvc.update(this.request!.id, existing.id, payload)
            : this.resSvc.saveOrUpdate(this.request!.id, payload);
          obs.subscribe({ next: () => resolve(), error: () => resolve() });
        })
      : Promise.resolve();

    saveObs.then(() => {
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

  stepStatus(step: AuditProcessStep): 'done' | 'draft' | 'active' | 'pending' {
    const r = this.results.find(res =>
      (res.processStep?.id ?? -1) === step.id
    );
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

  getFileUrl(url: string): string {
    return `${this.serverBase}${url}`;
  }

  isImage(name: string): boolean {
    return /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(name);
  }

  toggleBot(): void { this.botOpen = !this.botOpen; }

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
}