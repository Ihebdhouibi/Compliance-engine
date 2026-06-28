import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';
import { TokenService } from '../shared/token.service';

/**
 * Shared helper for downloading server-generated audit PDF reports.
 * Backend endpoint: GET /api/v1/audits/{id}/report
 */
@Injectable({ providedIn: 'root' })
export class ReportService {
  private http     = inject(HttpClient);
  private tokenSvc = inject(TokenService);
  private base     = environment.apiUrl;

  /** Fetch the report PDF as a blob (auth header attached explicitly). */
  fetchAuditReport(auditId: number): Observable<Blob> {
    const headers = new HttpHeaders({
      Authorization: `Bearer ${this.tokenSvc.getToken()}`
    });
    return this.http.get(`${this.base}/audits/${auditId}/report`, {
      headers,
      responseType: 'blob'
    });
  }

  /**
   * Fetch and trigger a browser download of the audit report.
   * Emits the blob once the download has been kicked off.
   */
  downloadAuditReport(auditId: number): Observable<Blob> {
    return this.fetchAuditReport(auditId).pipe(
      tap(blob => this.triggerDownload(blob, `audit_${auditId}_report.pdf`))
    );
  }

  private triggerDownload(blob: Blob, fileName: string): void {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(objectUrl);
  }
}
