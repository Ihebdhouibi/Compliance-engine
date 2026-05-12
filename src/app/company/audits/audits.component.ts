import {
  Component, OnInit, ChangeDetectorRef, Renderer2,
  ViewChild, ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuditRequestService } from '../../services/audit-request.service';
import { AuditStepResultService, AuditStepResult } from '../../services/audit-step-result.service';
import {
  AuditRequest, AuditStatus, AuditFormTemplate,
  AuditFormField, AuditFormStep,
  SubmitAuditPayload, AuditType
} from '../../models/audit.model';
import { TokenService } from '../../shared/token.service';

interface FormState {
  [fieldId: number]: string | string[];
}

@Component({
  selector: 'app-company-audits',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './audits.component.html',
  styleUrl: './audits.component.scss'
})
export class AuditsComponent implements OnInit {

  requests:    AuditRequest[] = [];
  totalItems   = 0;
  totalPages   = 0;
  currentPage  = 0;
  isLoading    = false;

  selectedRequest:  AuditRequest | null = null;
  showDetail        = false;

  // ── Audit Results Modal
  showResults      = false;
  resultsRequest:  AuditRequest | null = null;
  stepResults:     AuditStepResult[]   = [];
  isLoadingResults = false;

  // File viewer
  viewingFileUrl:   SafeResourceUrl | null = null;
  viewingRawUrl:    string | null = null;
  viewingFileName:  string | null = null;
  showFileViewer    = false;
  isLoadingFile     = false;

  // New audit form
  showNewAudit      = false;
  auditForm:        AuditFormTemplate | null = null;
  isLoadingForm     = false;
  formLoadError     = '';

  currentStepIdx    = 0;
  formState:        FormState = {};
  fileMap:          { [fieldId: number]: File[] } = {};
  isSubmitting      = false;
  submitSuccess     = false;
  uploadingFieldId: number | null = null;

  validationErrors: { [fieldId: number]: string } = {};

  successMsg = '';
  errorMsg   = '';

  readonly auditTypes: { value: AuditType; label: string }[] = [
    { value: 'AI_READINESS_REVIEW',     label: 'AI Readiness Review' },
    { value: 'INTERNAL_AI_GOVERNANCE',  label: 'Internal AI Governance Review' },
    { value: 'RESPONSIBLE_AI_ASSURANCE',label: 'Responsible AI Assurance Review' },
    { value: 'RICS_RESPONSIBLE_AI',     label: 'RICS Responsible AI Review' }
  ];

  constructor(
    public  svc:       AuditRequestService,
    private resSvc:    AuditStepResultService,
    private cdr:       ChangeDetectorRef,
    private renderer:  Renderer2,
    private sanitizer: DomSanitizer,
    private http:      HttpClient,
    private tokenSvc:  TokenService
  ) {}

  ngOnInit(): void { this.loadRequests(); }

  loadRequests(): void {
    this.isLoading = true;
    this.svc.getMyRequests(this.currentPage, 10).subscribe({
      next: res => {
        this.requests   = res.result;
        this.totalItems = res.totalItems;
        this.totalPages = res.totalPages;
        this.isLoading  = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoading = false; }
    });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.currentPage = p;
    this.loadRequests();
  }

  get pages(): number[] {
    return Array.from({ length: Math.min(this.totalPages, 7) }, (_, i) => i);
  }

  openDetail(req: AuditRequest): void {
    this.selectedRequest = req;
    this.showDetail      = true;
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  closeDetail(): void {
    this.showDetail      = false;
    this.selectedRequest = null;
    this.renderer.removeStyle(document.body, 'overflow');
  }

  // ── View audit results (for completed audits)
  openResults(req: AuditRequest): void {
    this.resultsRequest   = req;
    this.stepResults      = [];
    this.isLoadingResults = true;
    this.showResults      = true;
    this.renderer.setStyle(document.body, 'overflow', 'hidden');

    this.resSvc.getResults(req.id).subscribe({
      next: results => {
        this.stepResults      = results.filter(
          r => r.status === 'SAVED' || (r.description && r.description.trim().length > 0)
        );
        this.isLoadingResults = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingResults = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeResults(): void {
    this.showResults     = false;
    this.resultsRequest  = null;
    this.stepResults     = [];
    this.renderer.removeStyle(document.body, 'overflow');
  }

  // ── New audit
  startNewAudit(type: AuditType): void {
    this.isLoadingForm = true;
    this.formLoadError = '';
    this.svc.getAuditForm(type).subscribe({
      next: form => {
        this.auditForm      = form;
        this.isLoadingForm  = false;
        this.currentStepIdx = 0;
        this.formState      = {};
        this.fileMap        = {};
        this.submitSuccess  = false;
        this.validationErrors = {};
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingForm = false;
        this.formLoadError = 'Failed to load audit form. Please try again.';
      }
    });
  }

  openNewAudit(): void {
    this.showNewAudit     = true;
    this.auditForm        = null;
    this.formLoadError    = '';
    this.submitSuccess    = false;
    this.validationErrors = {};
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  closeNewAudit(): void {
    this.showNewAudit = false;
    this.auditForm    = null;
    this.renderer.removeStyle(document.body, 'overflow');
  }

  get currentStep(): AuditFormStep | null {
    return this.auditForm?.steps?.[this.currentStepIdx] ?? null;
  }

  get isFirstStep(): boolean { return this.currentStepIdx === 0; }
  get isLastStep():  boolean {
    return this.currentStepIdx === (this.auditForm?.steps?.length ?? 1) - 1;
  }
  get totalSteps(): number { return this.auditForm?.steps?.length ?? 0; }

  private findFieldById(fieldId: number): AuditFormField | null {
    if (!this.auditForm) return null;
    for (const step of this.auditForm.steps) {
      const f = step.fields.find(x => x.id === fieldId);
      if (f) return f;
    }
    return null;
  }

  validateCurrentStep(): boolean {
    this.validationErrors = {};
    if (!this.currentStep) return true;

    let valid = true;
    let firstInvalidId: number | null = null;
    for (const field of this.currentStep.fields) {
      if (!field.required) continue;

      if (field.fieldType === 'FILE') {
        const files = this.fileMap[field.id];
        if (!files || files.length === 0) {
          this.validationErrors[field.id] = 'This file is required.';
          valid = false;
          if (firstInvalidId === null) firstInvalidId = field.id;
        }
      } else if (field.fieldType === 'MULTI_CHECKBOX' || field.fieldType === 'CHECKBOX') {
        const v = this.getCheckboxValues(field.id);
        if (v.length === 0) {
          this.validationErrors[field.id] = 'Please select at least one option.';
          valid = false;
          if (firstInvalidId === null) firstInvalidId = field.id;
        }
      } else {
        const v = this.getFieldValue(field.id);
        if (!v || v.trim() === '') {
          this.validationErrors[field.id] = 'This field is required.';
          valid = false;
          if (firstInvalidId === null) firstInvalidId = field.id;
        }
      }
    }
    this.cdr.detectChanges();
    if (!valid && firstInvalidId !== null) {
      this.scrollToField(firstInvalidId);
    }
    return valid;
  }

  private scrollToField(fieldId: number): void {
    setTimeout(() => {
      const el = document.getElementById('field-' + fieldId);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }, 0);
  }

  nextStep(): void {
    if (!this.validateCurrentStep()) return;
    if (!this.isLastStep) this.currentStepIdx++;
    this.validationErrors = {};
    this.scrollWizardToTop();
  }

  prevStep(): void {
    if (!this.isFirstStep) this.currentStepIdx--;
    this.validationErrors = {};
    this.scrollWizardToTop();
  }

  @ViewChild('wizardBody') wizardBody?: ElementRef<HTMLElement>;

  private scrollWizardToTop(): void {
    setTimeout(() => {
      const el = this.wizardBody?.nativeElement;
      if (el) el.scrollTo({ top: 0, behavior: 'smooth' });
    }, 0);
  }

  getFieldValue(fieldId: number): string {
    return (this.formState[fieldId] as string) ?? '';
  }

  setFieldValue(fieldId: number, value: string): void {
    this.formState[fieldId] = value;
    delete this.validationErrors[fieldId];
  }

  getCheckboxValues(fieldId: number): string[] {
    const v = this.formState[fieldId];
    return Array.isArray(v) ? v : [];
  }

  toggleCheckbox(fieldId: number, value: string): void {
    const current = this.getCheckboxValues(fieldId);
    const idx     = current.indexOf(value);
    this.formState[fieldId] = idx === -1
      ? [...current, value]
      : current.filter(v => v !== value);
    delete this.validationErrors[fieldId];
  }

  isChecked(fieldId: number, value: string): boolean {
    return this.getCheckboxValues(fieldId).includes(value);
  }

  onFileChange(fieldId: number, e: Event, multiple = false): void {
    const input = e.target as HTMLInputElement;
    const list  = input.files;
    if (!list || list.length === 0) return;
    const incoming = Array.from(list);

    if (multiple) {
      const existing = this.fileMap[fieldId] ?? [];
      this.fileMap[fieldId] = [...existing, ...incoming];
    } else {
      this.fileMap[fieldId] = [incoming[0]];
    }
    this.formState[fieldId] = this.fileMap[fieldId].map(f => f.name).join(', ');
    delete this.validationErrors[fieldId];
    // Allow re-selecting the same file later
    input.value = '';
  }

  removePendingFile(fieldId: number, index: number): void {
    const files = this.fileMap[fieldId];
    if (!files) return;
    files.splice(index, 1);
    if (files.length === 0) {
      delete this.fileMap[fieldId];
      delete this.formState[fieldId];
    } else {
      this.formState[fieldId] = files.map(f => f.name).join(', ');
    }
  }

  getPendingFiles(fieldId: number): File[] {
    return this.fileMap[fieldId] ?? [];
  }

  getFileName(fieldId: number): string {
    const files = this.fileMap[fieldId];
    if (!files || files.length === 0) return '';
    return files.length === 1 ? files[0].name : `${files.length} files selected`;
  }

  async submitAudit(): Promise<void> {
    if (!this.auditForm) return;
    if (!this.validateCurrentStep()) return;

    this.isSubmitting = true;
    this.errorMsg     = '';

    const answers: { fieldId: number; fieldLabel: string; answerValue?: string }[] = [];

    for (const step of this.auditForm.steps) {
      for (const field of step.fields) {
        if (field.fieldType === 'FILE') continue;
        const v = this.formState[field.id];
        answers.push({
          fieldId:     field.id,
          fieldLabel:  field.label,
          answerValue: Array.isArray(v) ? v.join(',') : (v ?? '')
        });
      }
    }

    const payload: SubmitAuditPayload = {
      auditType: this.auditForm.auditType,
      answers
    };

    this.svc.submitAuditRequest(payload).subscribe({
      next: async (created) => {
        const fileFieldIds = Object.keys(this.fileMap).map(Number);
        for (const fieldId of fileFieldIds) {
          const files = this.fileMap[fieldId];
          if (!files || files.length === 0) continue;
          this.uploadingFieldId = fieldId;
          try {
            const field = this.findFieldById(fieldId);
            if (field?.multipleFiles) {
              await this.svc.uploadAnswerFiles(created.id, fieldId, files).toPromise();
            } else {
              await this.svc.uploadAnswerFile(created.id, fieldId, files[0]).toPromise();
            }
          } catch {}
        }
        this.uploadingFieldId = null;
        this.isSubmitting     = false;
        this.submitSuccess    = true;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.closeNewAudit();
          this.loadRequests();
        }, 2500);
      },
      error: err => {
        this.isSubmitting = false;
        this.errorMsg     = typeof err.error === 'string'
          ? err.error : 'Failed to submit. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  openFileViewer(rawUrl: string, name: string): void {
    this.viewingRawUrl   = rawUrl;
    this.viewingFileName = name;
    this.viewingFileUrl  = null;
    this.isLoadingFile   = true;
    this.showFileViewer  = true;
    this.renderer.setStyle(document.body, 'overflow', 'hidden');

    const token   = this.tokenSvc.getToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.get(rawUrl, { headers, responseType: 'blob' }).subscribe({
      next: blob => {
        const objectUrl     = URL.createObjectURL(blob);
        this.viewingFileUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
        this.isLoadingFile  = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.viewingFileUrl = this.sanitizer.bypassSecurityTrustResourceUrl(rawUrl);
        this.isLoadingFile  = false;
        this.cdr.detectChanges();
      }
    });
  }

  closeFileViewer(): void {
    this.showFileViewer  = false;
    this.viewingFileUrl  = null;
    this.viewingRawUrl   = null;
    this.viewingFileName = null;
    this.renderer.removeStyle(document.body, 'overflow');
  }

  downloadFile(relativePath: string, name: string): void {
    this.svc.downloadFile(relativePath, name);
  }

  statusClass(s: AuditStatus): string {
    const map: Record<AuditStatus, string> = {
      SUBMITTED: 'st--sub', ASSIGNED: 'st--asgn',
      IN_PROGRESS: 'st--prog', COMPLETED: 'st--done', REJECTED: 'st--rej'
    };
    return map[s] ?? '';
  }

  isImage(url: string): boolean {
    return /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(url);
  }

  isPdf(url: string): boolean {
    return /\.pdf$/i.test(url);
  }

  fieldHasValue(field: AuditFormField): boolean {
    const v = this.formState[field.id];
    if (Array.isArray(v)) return v.length > 0;
    return !!v;
  }
}