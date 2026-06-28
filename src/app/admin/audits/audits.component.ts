import { Component, OnInit, ChangeDetectorRef, Renderer2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuditRequestService } from '../../services/audit-request.service';
import { AdminUsersService } from '../../services/admin-users.service';
import { UserStoreService } from '../../shared/user-store.service';
import { AuditRequest, AuditStatus, AssignAuditPayload } from '../../models/audit.model';
import { User } from '../../models/user.model';
import { TokenService } from '../../shared/token.service';
import { AuditStepResultService, AuditStepResult } from '../../services/audit-step-result.service';
import { ReportService } from '../../services/report.service';

type TabMode = 'all' | 'mine';

@Component({
  selector: 'app-admin-audits',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './audits.component.html',
  styleUrl: './audits.component.scss'
})
export class AuditsComponent implements OnInit {

  tab: TabMode = 'all';

  showResults      = false;
resultsRequest:  AuditRequest | null = null;
stepResults:     AuditStepResult[]   = [];
isLoadingResults = false;

  requests:    AuditRequest[] = [];
  totalItems   = 0;
  totalPages   = 0;
  currentPage  = 0;
  isLoading    = false;
  searchTerm   = '';
  filterStatus: AuditStatus | '' = '';

  myAudits:     AuditRequest[] = [];
  myTotal       = 0;
  myTotalPages  = 0;
  myPage        = 0;
  isLoadingMine = false;

  auditors: User[] = [];

  selectedRequest:  AuditRequest | null = null;
  showDetail        = false;

  showAssign        = false;
  assigningRequest: AuditRequest | null = null;
  assignForm!:      FormGroup;
  isAssigning       = false;
  assignError       = '';

  showReject        = false;
  rejectingRequest: AuditRequest | null = null;
  rejectReason      = '';
  isRejecting       = false;

  showFileViewer    = false;
  viewingFileUrl:   SafeResourceUrl | null = null;
  viewingRawUrl:    string | null = null;
  viewingFileName:  string | null = null;
  isLoadingFile     = false;

  isTakingId: number | null = null;
  isExportingId: number | null = null;
  successMsg = '';

  readonly statuses: AuditStatus[] =
    ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED'];

  constructor(
    public  svc:       AuditRequestService,
    private usersSvc:  AdminUsersService,
    private userStore: UserStoreService,
    private fb:        FormBuilder,
    private cdr:       ChangeDetectorRef,
    private renderer:  Renderer2,
    private sanitizer: DomSanitizer,
    private http:      HttpClient,
    private tokenSvc:  TokenService,
    private router:    Router,
   private resSvc: AuditStepResultService,
   private reportSvc: ReportService
  ) {}

  ngOnInit(): void {
    this.assignForm = this.fb.group({
      assignedToUserId: ['', Validators.required],
      dueDate:          ['']
    });
    this.loadRequests();
    this.loadAuditors();
  }

  get currentAdminId(): number | null {
    return this.userStore.currentUser()?.id ?? null;
  }

  // ── Navigation
  openWorkspace(req: AuditRequest): void {
    this.router.navigate(['/admin/audits/workspace', req.id]);
  }

  // Admin always navigates to admin workspace to view results
 openResults(req: AuditRequest): void {
  this.resultsRequest  = req;
  this.stepResults     = [];
  this.isLoadingResults = true;
  this.showResults     = true;
  this.renderer.setStyle(document.body, 'overflow', 'hidden');

  this.resSvc.getResults(req.id).subscribe({
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
  this.showResults     = false;
  this.resultsRequest  = null;
  this.stepResults     = [];
  this.renderer.removeStyle(document.body, 'overflow');
}

  // ── Export report (completed audits)
  exportReport(req: AuditRequest): void {
    if (this.isExportingId) return;
    this.isExportingId = req.id;
    this.reportSvc.downloadAuditReport(req.id).subscribe({
      next: () => {
        this.isExportingId = null;
        this.flash('Report downloaded.');
        this.cdr.detectChanges();
      },
      error: () => {
        this.isExportingId = null;
        this.flash('Failed to generate report.');
        this.cdr.detectChanges();
      }
    });
  }

  // ── Data loading
  loadRequests(): void {
    this.isLoading = true;
    this.svc.adminGetAllRequests(
      this.currentPage, 10,
      this.filterStatus || undefined,
      this.searchTerm   || undefined
    ).subscribe({
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

  loadMyAudits(): void {
    this.isLoadingMine = true;
    this.svc.adminGetMyAudits(this.myPage, 10).subscribe({
      next: res => {
        this.myAudits      = res.result;
        this.myTotal       = res.totalItems;
        this.myTotalPages  = res.totalPages;
        this.isLoadingMine = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoadingMine = false; }
    });
  }

  loadAuditors(): void {
    this.usersSvc.getAuditors({ page: 0, size: 100 }).subscribe({
      next: res => { this.auditors = res.result; }
    });
  }

  // ── Tabs
  setTab(t: TabMode): void {
    this.tab = t;
    if (t === 'mine' && this.myAudits.length === 0) this.loadMyAudits();
  }

  // ── Search & filter
  onSearch(): void { this.currentPage = 0; this.loadRequests(); }
  onFilter(): void { this.currentPage = 0; this.loadRequests(); }

  clearFilters(): void {
    this.searchTerm   = '';
    this.filterStatus = '';
    this.currentPage  = 0;
    this.loadRequests();
  }

  // ── Pagination
  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.currentPage = p;
    this.loadRequests();
  }

  goToMyPage(p: number): void {
    if (p < 0 || p >= this.myTotalPages) return;
    this.myPage = p;
    this.loadMyAudits();
  }

  get pages(): number[] {
    return Array.from({ length: Math.min(this.totalPages, 7) }, (_, i) => i);
  }

  get myPages(): number[] {
    return Array.from({ length: Math.min(this.myTotalPages, 7) }, (_, i) => i);
  }

  // ── Detail modal
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

  // ── Assign modal
  openAssign(req: AuditRequest): void {
    this.assigningRequest = req;
    this.assignForm.reset();
    if (req.assignedTo) {
      this.assignForm.patchValue({ assignedToUserId: req.assignedTo.id });
    }
    this.assignError = '';
    this.showAssign  = true;
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  closeAssign(): void {
    this.showAssign       = false;
    this.assigningRequest = null;
    this.renderer.removeStyle(document.body, 'overflow');
  }

  submitAssign(): void {
    if (this.assignForm.invalid || !this.assigningRequest) {
      this.assignForm.markAllAsTouched(); return;
    }
    this.isAssigning = true;
    const payload: AssignAuditPayload = {
      assignedToUserId: +this.assignForm.value.assignedToUserId,
      dueDate:          this.assignForm.value.dueDate || undefined
    };
    this.svc.adminAssignRequest(this.assigningRequest.id, payload).subscribe({
      next: updated => {
        this.updateInList(updated);
        this.isAssigning = false;
        this.closeAssign();
        this.flash('Audit assigned successfully.');
      },
      error: err => {
        this.isAssigning = false;
        this.assignError = typeof err.error === 'string'
          ? err.error : 'Failed to assign.';
      }
    });
  }

  // ── Take audit
  takeAudit(req: AuditRequest): void {
    const adminId = this.currentAdminId;
    if (!adminId) return;
    this.isTakingId = req.id;
    const payload: AssignAuditPayload = { assignedToUserId: adminId };
    this.svc.adminAssignRequest(req.id, payload).subscribe({
      next: updated => {
        this.updateInList(updated);
        this.isTakingId = null;
        this.flash('Audit assigned to you.');
        this.svc.adminGetMyAudits(0, 10).subscribe({
          next: res => {
            this.myAudits     = res.result;
            this.myTotal      = res.totalItems;
            this.myTotalPages = res.totalPages;
          }
        });
        this.cdr.detectChanges();
      },
      error: () => { this.isTakingId = null; }
    });
  }

  // ── Reject modal
  openReject(req: AuditRequest): void {
    this.rejectingRequest = req;
    this.rejectReason     = '';
    this.showReject       = true;
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  closeReject(): void {
    this.showReject       = false;
    this.rejectingRequest = null;
    this.renderer.removeStyle(document.body, 'overflow');
  }

  submitReject(): void {
    if (!this.rejectingRequest) return;
    this.isRejecting = true;
    this.svc.adminRejectRequest(this.rejectingRequest.id, this.rejectReason).subscribe({
      next: updated => {
        this.updateInList(updated);
        this.isRejecting = false;
        this.closeReject();
        this.flash('Request rejected.');
      },
      error: () => { this.isRejecting = false; }
    });
  }

  // ── Actions (kept for direct table actions outside workspace)
  startAudit(req: AuditRequest): void {
    this.svc.adminStartAudit(req.id).subscribe({
      next: updated => { this.updateInList(updated); this.flash('Audit started.'); }
    });
  }

  completeAudit(req: AuditRequest): void {
    this.svc.adminCompleteRequest(req.id).subscribe({
      next: updated => { this.updateInList(updated); this.flash('Audit completed.'); }
    });
  }

  // ── File viewer
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

  // ── Helpers
  private updateInList(updated: AuditRequest): void {
    const idx = this.requests.findIndex(r => r.id === updated.id);
    if (idx !== -1) this.requests[idx] = { ...updated };
    const mi  = this.myAudits.findIndex(r => r.id === updated.id);
    if (mi  !== -1) this.myAudits[mi]  = { ...updated };
    if (this.selectedRequest?.id === updated.id) {
      this.selectedRequest = { ...updated };
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

  auditorName(u?: User): string {
    if (!u) return '—';
    return `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || u.email;
  }

  isImage(url: string): boolean {
    return /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(url);
  }

  isAssignedToMe(req: AuditRequest): boolean {
    return req.assignedTo?.id === this.currentAdminId;
  }


}
