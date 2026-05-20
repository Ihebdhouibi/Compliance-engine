import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuditRequestService } from '../../services/audit-request.service';
import {
  AuditRequest, AuditRequestAnswer,
  AuditAnswerScoring, ScoreAnswerPayload
} from '../../models/audit.model';

interface Row {
  answer: AuditRequestAnswer;
  scoring: ScoreAnswerPayload;
  saving?: boolean;
  saved?:  boolean;
  error?:  string;
}

/**
 * Auditor maturity-scoring view for an L2 audit request.
 *
 * Renders one row per `AuditRequestAnswer` and exposes:
 *  - a 0-5 numeric maturity score,
 *  - free-text auditor notes,
 *  - an evidence-verdict dropdown,
 *  - an evidence reference (e.g. file ID / doc ref).
 *
 * Scores are persisted individually per row via PUT
 * `/api/v1/two-level/answers/{answerId}/score`.
 */
@Component({
  selector: 'app-auditor-scoring',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auditor-scoring.component.html',
  styleUrl: './auditor-scoring.component.scss'
})
export class AuditorScoringComponent implements OnInit {

  request: AuditRequest | null = null;
  rows: Row[] = [];
  loading = false;
  errorMsg = '';

  readonly evidenceStatuses = [
    { value: 'EVIDENCED', label: 'Evidenced' },
    { value: 'PARTIAL',   label: 'Partially evidenced' },
    { value: 'MISSING',   label: 'Missing' },
    { value: 'NA',        label: 'Not applicable' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private svc: AuditRequestService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.errorMsg = 'Missing audit request id';
      return;
    }
    this.load(id);
  }

  load(id: number): void {
    this.loading = true;
    this.svc.auditorGetAuditById(id).subscribe({
      next: req => {
        this.request = req;
        this.svc.getScorings(id).subscribe({
          next: scorings => {
            this.rows = (req.answers || []).map(a => ({
              answer: a,
              scoring: this.findScoring(a.id, scorings)
            }));
            this.loading = false;
            this.cdr.detectChanges();
          },
          error: () => {
            this.rows = (req.answers || []).map(a => ({
              answer: a,
              scoring: { auditorScore: 0 }
            }));
            this.loading = false;
          }
        });
      },
      error: err => {
        this.errorMsg = err?.error?.message || 'Failed to load audit';
        this.loading = false;
      }
    });
  }

  private findScoring(answerId: number, scorings: AuditAnswerScoring[]): ScoreAnswerPayload {
    const existing = scorings.find(s => (s as any).answer?.id === answerId);
    if (!existing) return { auditorScore: 0 };
    return {
      auditorScore: existing.auditorScore,
      auditorNotes: existing.auditorNotes,
      evidenceProvidedStatus: existing.evidenceProvidedStatus,
      evidenceReference: existing.evidenceReference
    };
  }

  saveRow(row: Row): void {
    row.saving = true;
    row.error  = '';
    row.saved  = false;
    this.svc.scoreAnswer(row.answer.id, row.scoring).subscribe({
      next: () => {
        row.saving = false;
        row.saved  = true;
        this.cdr.detectChanges();
        setTimeout(() => { row.saved = false; this.cdr.detectChanges(); }, 1500);
      },
      error: err => {
        row.saving = false;
        row.error  = err?.error?.message || 'Failed to save';
      }
    });
  }

  back(): void { this.router.navigate(['/auditor/audits']); }
}
