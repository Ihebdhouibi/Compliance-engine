import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import {
  AuditFormTemplate, AuditFormStep, AuditFormField,
  CreateTemplatePayload, AddStepPayload, AddFieldPayload, AuditType
} from '../models/audit.model';

@Injectable({ providedIn: 'root' })
export class AuditFormService {

  private base = `${environment.apiUrl}/admin/audits`;

  constructor(private http: HttpClient) {}

  getAllTemplates(): Observable<AuditFormTemplate[]> {
    return this.http.get<AuditFormTemplate[]>(`${this.base}/templates`);
  }

  getTemplateById(id: number): Observable<AuditFormTemplate> {
    return this.http.get<AuditFormTemplate>(`${this.base}/templates/${id}`);
  }

  getTemplateByType(type: AuditType): Observable<AuditFormTemplate> {
    return this.http.get<AuditFormTemplate>(`${this.base}/templates/type/${type}`);
  }

  createTemplate(payload: CreateTemplatePayload): Observable<AuditFormTemplate> {
    return this.http.post<AuditFormTemplate>(`${this.base}/templates`, payload);
  }

  /** Generates the built-in RICS Responsible AI template from the knowledge base. */
  generateDefaultRicsTemplate(): Observable<AuditFormTemplate> {
    return this.http.post<AuditFormTemplate>(
      `${this.base}/templates/generate-default-rics`, null);
  }

  toggleTemplate(id: number): Observable<AuditFormTemplate> {
    return this.http.patch<AuditFormTemplate>(
      `${this.base}/templates/${id}/toggle`, null);
  }

  addStep(templateId: number, payload: AddStepPayload): Observable<AuditFormStep> {
    return this.http.post<AuditFormStep>(
      `${this.base}/templates/${templateId}/steps`, payload);
  }

  removeStep(stepId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/steps/${stepId}`);
  }

  // Reorder steps — sends array of { id, stepOrder }
  reorderSteps(templateId: number, steps: { id: number; stepOrder: number }[]): Observable<void> {
    return this.http.patch<void>(
      `${this.base}/templates/${templateId}/steps/reorder`, steps);
  }

  addField(stepId: number, payload: AddFieldPayload): Observable<AuditFormField> {
    return this.http.post<AuditFormField>(
      `${this.base}/steps/${stepId}/fields`, payload);
  }

  removeField(fieldId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/fields/${fieldId}`);
  }

  // Reorder fields — sends array of { id, fieldOrder }
  reorderFields(stepId: number, fields: { id: number; fieldOrder: number }[]): Observable<void> {
    return this.http.patch<void>(
      `${this.base}/steps/${stepId}/fields/reorder`, fields);
  }

  updateTemplate(id: number, payload: { title: string; description?: string }): Observable<AuditFormTemplate> {
  return this.http.patch<AuditFormTemplate>(
    `${this.base}/templates/${id}`, payload);
}



}
