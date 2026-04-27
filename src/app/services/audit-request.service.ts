import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import {
  AuditRequest, AuditStatus, AuditType,
  AuditFormTemplate, SubmitAuditPayload,
  AssignAuditPayload, AuditRequestAnswer
} from '../models/audit.model';
import { PageResponse } from '../models/user.model';
import { TokenService } from '../shared/token.service';

@Injectable({ providedIn: 'root' })
export class AuditRequestService {

  private adminBase   = `${environment.apiUrl}/admin/audits`;
  private auditorBase = `${environment.apiUrl}/auditor/audits`;
  private userBase    = `${environment.apiUrl}/user/audits`;
  readonly serverBase = environment.serverBaseUrl;

  constructor(
    private http:     HttpClient,
    private tokenSvc: TokenService
  ) {}

  // ── USER
  getAuditForm(auditType: AuditType): Observable<AuditFormTemplate> {
    return this.http.get<AuditFormTemplate>(`${this.userBase}/form/${auditType}`);
  }

  submitAuditRequest(payload: SubmitAuditPayload): Observable<AuditRequest> {
    return this.http.post<AuditRequest>(`${this.userBase}/submit`, payload);
  }

  getMyRequests(page = 0, size = 10): Observable<PageResponse<AuditRequest>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AuditRequest>>(
      `${this.userBase}/my-requests`, { params });
  }

  uploadAnswerFile(requestId: number, fieldId: number, file: File): Observable<AuditRequest> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.patch<AuditRequest>(
      `${this.userBase}/my-requests/${requestId}/upload/${fieldId}`, fd);
  }

  // ── ADMIN
  adminGetAllRequests(
    page = 0, size = 10,
    status?: AuditStatus,
    search?: string
  ): Observable<PageResponse<AuditRequest>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (search) params = params.set('search', search);
    return this.http.get<PageResponse<AuditRequest>>(
      `${this.adminBase}/requests`, { params });
  }

  adminGetRequest(id: number): Observable<AuditRequest> {
    return this.http.get<AuditRequest>(`${this.adminBase}/requests/${id}`);
  }

  adminAssignRequest(id: number, payload: AssignAuditPayload): Observable<AuditRequest> {
    return this.http.patch<AuditRequest>(
      `${this.adminBase}/requests/${id}/assign`, payload);
  }

  adminRejectRequest(id: number, reason: string): Observable<AuditRequest> {
    const params = new HttpParams().set('reason', reason);
    return this.http.patch<AuditRequest>(
      `${this.adminBase}/requests/${id}/reject`, null, { params });
  }

  adminCompleteRequest(id: number): Observable<AuditRequest> {
    return this.http.patch<AuditRequest>(
      `${this.adminBase}/requests/${id}/complete`, null);
  }

  adminStartAudit(id: number): Observable<AuditRequest> {
    return this.http.patch<AuditRequest>(
      `${this.adminBase}/requests/${id}/start`, null);
  }

  adminGetMyAudits(page = 0, size = 10): Observable<PageResponse<AuditRequest>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AuditRequest>>(
      `${this.adminBase}/my-audits`, { params });
  }

  // ── AUDITOR
  auditorGetMyAudits(page = 0, size = 10): Observable<PageResponse<AuditRequest>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AuditRequest>>(
      `${this.auditorBase}/my-audits`, { params });
  }

  auditorStartAudit(id: number): Observable<AuditRequest> {
    return this.http.patch<AuditRequest>(
      `${this.auditorBase}/my-audits/${id}/start`, null);
  }

  auditorCompleteAudit(id: number): Observable<AuditRequest> {
    return this.http.patch<AuditRequest>(
      `${this.auditorBase}/my-audits/${id}/complete`, null);
  }

  auditorReassignAudit(id: number, payload: AssignAuditPayload): Observable<AuditRequest> {
    return this.http.patch<AuditRequest>(
      `${this.auditorBase}/my-audits/${id}/reassign`, payload);
  }

  auditorGetAuditById(id: number): Observable<AuditRequest> {
  return this.http.get<AuditRequest>(
    `${this.auditorBase}/my-audits/${id}`
  );
}

  // ── File helpers

  getFileUrl(relativePath: string): string {
    return `${this.serverBase}${relativePath}`;
  }

  getAnswerFileUrl(answer: AuditRequestAnswer): string | null {
    return answer.fileMedia?.url
      ? this.getFileUrl(answer.fileMedia.url)
      : null;
  }

  // Download via blob — avoids about:blank#blocked
  downloadFile(relativePath: string, fileName: string): void {
    const url     = this.getFileUrl(relativePath);
    const token   = this.tokenSvc.getToken();

    const headers = new Headers();
    if (token) headers.set('Authorization', `Bearer ${token}`);

    fetch(url, { headers })
      .then(res => {
        if (!res.ok) throw new Error('Network error');
        return res.blob();
      })
      .then(blob => {
        const objectUrl = URL.createObjectURL(blob);
        const a         = document.createElement('a');
        a.href          = objectUrl;
        a.download      = fileName || 'download';
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        setTimeout(() => URL.revokeObjectURL(objectUrl), 2000);
      })
      .catch(() => {
        // Last resort fallback
        window.open(url, '_blank');
      });
  }

  openFile(relativePath: string): void {
    window.open(this.getFileUrl(relativePath), '_blank');
  }

  getProcessStepsByAuditType(auditType: string, isAuditor: boolean): Observable<any[]> {
  const base = isAuditor ? this.auditorBase : this.adminBase;
  return this.http.get<any[]>(`${base}/process-steps/${auditType}`);
}
}