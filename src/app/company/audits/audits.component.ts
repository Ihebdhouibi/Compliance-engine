import {
  Component, OnInit, ChangeDetectorRef, Renderer2
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuditRequestService } from '../../services/audit-request.service';
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
  fileMap:          { [fieldId: number]: File } = {};
  isSubmitting      = false;
  submitSuccess     = false;
  uploadingFieldId: number | null = null;

  // Validation errors per step
  validationErrors: { [fieldId: number]: string } = {};

  successMsg = '';
  errorMsg   = '';

  readonly auditTypes: { value: AuditType; label: string }[] = [
    { value: 'RICS_AUDIT', label: 'RICS Compliance Audit' }
  ];

  constructor(
    public  svc:       AuditRequestService,
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
    this.showNewAudit   = true;
    this.auditForm      = null;
    this.formLoadError  = '';
    this.submitSuccess  = false;
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

  // ── Validate current step required fields
  validateCurrentStep(): boolean {
    this.validationErrors = {};
    if (!this.currentStep) return true;

    let valid = true;
    for (const field of this.currentStep.fields) {
      if (!field.required) continue;

      if (field.fieldType === 'FILE') {
        if (!this.fileMap[field.id]) {
          this.validationErrors[field.id] = 'This file is required.';
          valid = false;
        }
      } else if (field.fieldType === 'MULTI_CHECKBOX' || field.fieldType === 'CHECKBOX') {
        const v = this.getCheckboxValues(field.id);
        if (v.length === 0) {
          this.validationErrors[field.id] = 'Please select at least one option.';
          valid = false;
        }
      } else {
        const v = this.getFieldValue(field.id);
        if (!v || v.trim() === '') {
          this.validationErrors[field.id] = 'This field is required.';
          valid = false;
        }
      }
    }
    this.cdr.detectChanges();
    return valid;
  }

  nextStep(): void {
    if (!this.validateCurrentStep()) return;
    if (!this.isLastStep) this.currentStepIdx++;
    this.validationErrors = {};
  }

  prevStep(): void {
    if (!this.isFirstStep) this.currentStepIdx--;
    this.validationErrors = {};
  }

  // ── Form state
  getFieldValue(fieldId: number): string {
    return (this.formState[fieldId] as string) ?? '';
  }

  setFieldValue(fieldId: number, value: string): void {
    this.formState[fieldId] = value;
    // Clear validation error on change
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

  onFileChange(fieldId: number, e: Event): void {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      this.fileMap[fieldId]   = file;
      this.formState[fieldId] = file.name;
      delete this.validationErrors[fieldId];
    }
  }

  getFileName(fieldId: number): string {
    return this.fileMap[fieldId]?.name ?? '';
  }

  // ── Submit
  async submitAudit(): Promise<void> {
    if (!this.auditForm) return;
    // Validate last step before submitting
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
          this.uploadingFieldId = fieldId;
          try {
            await this.svc.uploadAnswerFile(
              created.id, fieldId, this.fileMap[fieldId]).toPromise();
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

  // ── File viewer — fetch with auth then create blob URL
  openFileViewer(rawUrl: string, name: string): void {
    this.viewingRawUrl   = rawUrl;
    this.viewingFileName = name;
    this.viewingFileUrl  = null;
    this.isLoadingFile   = true;
    this.showFileViewer  = true;
    this.renderer.setStyle(document.body, 'overflow', 'hidden');

    const token = this.tokenSvc.getToken();
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

    this.http.get(rawUrl, { headers, responseType: 'blob' }).subscribe({
      next: blob => {
        const objectUrl = URL.createObjectURL(blob);
        this.viewingFileUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
        this.isLoadingFile  = false;
        this.cdr.detectChanges();
      },
      error: () => {
        // Fallback: try direct URL
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