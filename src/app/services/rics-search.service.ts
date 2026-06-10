import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

/** Payload returned by Qdrant for a RICS rule. Shape mirrors the Python schema. */
export interface RicsRulePayload {
  section?:               string;
  section_title?:         string;
  rule_id?:               string;
  short_title?:           string;
  requirement_text?:      string;
  source_text_verbatim?:  string;
  evidence_implied?:      string;
  applies_to?:            string;
  entry_type?:            string;
  [key: string]: any;
}

export interface RicsRuleResult {
  rule_id:    string;
  score:      number;
  entry_type: string;
  payload:    RicsRulePayload;
}

export interface RicsSearchResponse {
  query:   string;
  results: RicsRuleResult[];
  total:   number;
}

/** One evidence hint returned by /rules/evidence-check. */
export interface RicsEvidenceItem {
  rule_id:               string;
  section:               string;
  requirement_text:      string;
  source_text_verbatim:  string;
  evidence_implied:      string;
  score:                 number;
}

export interface RicsEvidenceResponse {
  query:    string;
  evidence: RicsEvidenceItem[];
}

/**
 * Calls the FastAPI RAG service (port 8000) for semantic search over the
 * RICS knowledge base. Independent of the Spring backend (port 8080).
 */
@Injectable({ providedIn: 'root' })
export class RicsSearchService {

  private http = inject(HttpClient);
  private base = `${environment.ragApiUrl}/rules`;

  search(query: string, limit = 6, section?: string): Observable<RicsSearchResponse> {
    return this.http.post<RicsSearchResponse>(`${this.base}/search`, {
      query,
      limit,
      section: section ?? null,
      applies_to: null,
      entry_type: 'rule',
    });
  }

  /** Grounded evidence hints for a compliance area (e.g. an audit step name). */
  evidenceCheck(query: string, limit = 5): Observable<RicsEvidenceResponse> {
    return this.http.post<RicsEvidenceResponse>(`${this.base}/evidence-check`, { query, limit });
  }
}
