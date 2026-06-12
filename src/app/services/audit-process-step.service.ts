import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface AuditProcessStep {
  id: number;
  name: string;
  stepOrder: number;
  isDefault: boolean;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditProcessStepService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getSteps(templateId: number): Observable<AuditProcessStep[]> {
    return this.http.get<AuditProcessStep[]>(
      `${this.base}/audit-templates/${templateId}/process-steps`
    );
  }

  addStep(templateId: number, name: string, stepOrder: number): Observable<AuditProcessStep> {
    return this.http.post<AuditProcessStep>(
      `${this.base}/audit-templates/${templateId}/process-steps`,
      { name, stepOrder }
    );
  }

  updateStep(templateId: number, stepId: number, name: string, stepOrder?: number): Observable<AuditProcessStep> {
    return this.http.put<AuditProcessStep>(
      `${this.base}/audit-templates/${templateId}/process-steps/${stepId}`,
      { name, stepOrder }
    );
  }

  deleteStep(templateId: number, stepId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/audit-templates/${templateId}/process-steps/${stepId}`
    );
  }
}
