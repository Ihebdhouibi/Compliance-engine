import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface AuditStepResult {
  id: number;
  stepName: string;
  processStep?: { id: number; name: string; stepOrder: number };
  description: string;
  status: 'DRAFT' | 'SAVED';
  filledBy?: any;
  createdAt: string;
  updatedAt: string;
}

export interface SaveStepResultPayload {
  processStepId: number;
  stepName: string;
  description: string;
  status: 'DRAFT' | 'SAVED';
}

@Injectable({ providedIn: 'root' })
export class AuditStepResultService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getResults(requestId: number): Observable<AuditStepResult[]> {
    return this.http.get<AuditStepResult[]>(
      `${this.base}/audit-requests/${requestId}/step-results`
    );
  }

  saveOrUpdate(requestId: number, payload: SaveStepResultPayload): Observable<AuditStepResult> {
    return this.http.post<AuditStepResult>(
      `${this.base}/audit-requests/${requestId}/step-results`,
      payload
    );
  }

  update(requestId: number, resultId: number, payload: Partial<SaveStepResultPayload>): Observable<AuditStepResult> {
    return this.http.put<AuditStepResult>(
      `${this.base}/audit-requests/${requestId}/step-results/${resultId}`,
      payload
    );
  }

  delete(requestId: number, resultId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/audit-requests/${requestId}/step-results/${resultId}`
    );
  }
}
