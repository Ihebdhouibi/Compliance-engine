import { Component, OnInit, ChangeDetectorRef, Renderer2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuditRequestService } from '../../services/audit-request.service';
import { AdminUsersService } from '../../services/admin-users.service';
import { AuditRequest, AuditStatus, AssignAuditPayload } from '../../models/audit.model';
import { User } from '../../models/user.model';
import { TokenService } from '../../shared/token.service';

import { AuditStepResultService, AuditStepResult } from '../../services/audit-step-result.service';

@Component({
  selector: 'app-auditor-audits',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './audits.component.html',
  styleUrl: './audits.component.scss'
})
export class AuditsComponent implements OnInit {

  showResults      = false;
resultsRequest:  AuditRequest | null = null;
stepResults:     AuditStepResult[]   = [];
isLoadingResults = false;

  audits:      AuditRequest[] = [];
  totalItems   = 0;
  totalPages   = 0;
  currentPage  = 0;
  isLoading    = false;

  selectedAudit:    AuditRequest | null = null;
  showDetail        = false;

  showReassign      = false;
  reassignForm!:    FormGroup;
  isReassigning     = false;
  reassignError     = '';
  auditors:         User[] = [];

  showFileViewer    = false;
  viewingFileUrl:   SafeResourceUrl | null = null;
  viewingRawUrl:    string | null = null;
  viewingFileName:  string | null = null;
  isLoadingFile     = false;

  successMsg = '';

  constructor(
    public  svc:       AuditRequestService,
    private usersSvc:  AdminUsersService,
    private fb:        FormBuilder,
    private cdr:       ChangeDetectorRef,
    private renderer:  Renderer2,
    private sanitizer: DomSanitizer,
    private http:      HttpClient,
    private tokenSvc:  TokenService,
    private router:    Router,
    private resSvc: AuditStepResultService

  ) {}

  ngOnInit(): void {
    this.reassignForm = this.fb.group({
      assignedToUserId: ['', Validators.required],
      dueDate:          ['']
    });
    this.loadAudits();
    this.loadAuditors();
  }

  // Auditor always navigates to auditor workspace
  openWorkspace(audit: AuditRequest): void {
    this.router.navigate(['/auditor/audits/workspace', audit.id]);
  }

  // Auditor always navigates to auditor workspace to view results
  openResults(audit: AuditRequest): void {
  this.resultsRequest   = audit;
  this.stepResults      = [];
  this.isLoadingResults = true;
  this.showResults      = true;
  this.renderer.setStyle(document.body, 'overflow', 'hidden');

  this.resSvc.getResults(audit.id).subscribe({
    next: results => {
      this.stepResults      = results.filter(r => r.status === 'SAVED' || r.description?.trim());
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
  this.showResults    = false;
  this.resultsRequest = null;
  this.stepResults    = [];
  this.renderer.removeStyle(document.body, 'overflow');
}

  loadAudits(): void {
    this.isLoading = true;
    this.svc.auditorGetMyAudits(this.currentPage, 10).subscribe({
      next: res => {
        this.audits     = res.result;
        this.totalItems = res.totalItems;
        this.totalPages = res.totalPages;
        this.isLoading  = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoading = false; }
    });
  }

  loadAuditors(): void {
    this.usersSvc.getAuditors({ page: 0, size: 100 }).subscribe({
      next: res => { this.auditors = res.result; }
    });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.currentPage = p;
    this.loadAudits();
  }

  get pages(): number[] {
    return Array.from({ length: Math.min(this.totalPages, 7) }, (_, i) => i);
  }

  openDetail(audit: AuditRequest): void {
    this.selectedAudit = audit;
    this.showDetail    = true;
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  closeDetail(): void {
    this.showDetail    = false;
    this.selectedAudit = null;
    this.renderer.removeStyle(document.body, 'overflow');
  }

  // Kept for direct start from table (not workspace)
  startAudit(audit: AuditRequest): void {
    this.svc.auditorStartAudit(audit.id).subscribe({
      next: u => { this.updateInList(u); this.flash('Audit started.'); }
    });
  }

  completeAudit(audit: AuditRequest): void {
    this.svc.auditorCompleteAudit(audit.id).subscribe({
      next: u => { this.updateInList(u); this.flash('Audit completed.'); }
    });
  }

  openReassign(audit: AuditRequest): void {
    this.selectedAudit  = audit;
    this.reassignForm.reset();
    this.reassignError  = '';
    this.showReassign   = true;
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  closeReassign(): void {
    this.showReassign = false;
    this.renderer.removeStyle(document.body, 'overflow');
  }

  submitReassign(): void {
    if (this.reassignForm.invalid || !this.selectedAudit) {
      this.reassignForm.markAllAsTouched(); return;
    }
    this.isReassigning = true;
    const payload: AssignAuditPayload = {
      assignedToUserId: +this.reassignForm.value.assignedToUserId,
      dueDate:          this.reassignForm.value.dueDate || undefined
    };
    this.svc.auditorReassignAudit(this.selectedAudit.id, payload).subscribe({
      next: u => {
        this.updateInList(u);
        this.isReassigning = false;
        this.closeReassign();
        this.closeDetail();
        this.flash('Audit reassigned.');
      },
      error: err => {
        this.isReassigning = false;
        this.reassignError = typeof err.error === 'string'
          ? err.error : 'Failed to reassign.';
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

  private updateInList(updated: AuditRequest): void {
    const idx = this.audits.findIndex(a => a.id === updated.id);
    if (idx !== -1) this.audits[idx] = { ...updated };
    if (this.selectedAudit?.id === updated.id) {
      this.selectedAudit = { ...updated };
    }
    this.cdr.detectChanges();
  }

  private flash(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => this.successMsg = '', 3500);
  }

  statusClass(s: AuditStatus): string {
    const map: Record<AuditStatus, string> = {
      SUBMITTED:   'st--sub',
      ASSIGNED:    'st--asgn',
      IN_PROGRESS: 'st--prog',
      COMPLETED:   'st--done',
      REJECTED:    'st--rej'
    };
    return map[s] ?? '';
  }

  companyName(r: AuditRequest): string {
    return r.submittedBy.companyInfo?.companyName
      || `${r.submittedBy.firstName} ${r.submittedBy.lastName}`;
  }

  isImage(url: string): boolean {
    return /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(url);
  }
}