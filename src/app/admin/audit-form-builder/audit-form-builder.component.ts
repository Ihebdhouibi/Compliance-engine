import {
  Component, OnInit, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuditFormService } from '../../services/audit-form.service';
import {
  AuditFormTemplate, AuditFormStep, AuditFormField,
  AuditType, FieldType, AddFieldOptionPayload
} from '../../models/audit.model';

export const FIELD_TYPES: { value: FieldType; label: string; icon: string }[] = [
  { value: 'TEXT',           label: 'Text Input',   icon: 'T'  },
  { value: 'TEXTAREA',       label: 'Long Text',    icon: '¶'  },
  { value: 'NUMBER',         label: 'Number',       icon: '#'  },
  { value: 'EMAIL',          label: 'Email',        icon: '@'  },
  { value: 'DATE',           label: 'Date',         icon: '▦'  },
  { value: 'DROPDOWN',       label: 'Dropdown',     icon: '▾'  },
  { value: 'RADIO',          label: 'Radio',        icon: '◉'  },
  { value: 'CHECKBOX',       label: 'Checkbox',     icon: '☑'  },
  { value: 'MULTI_CHECKBOX', label: 'Multi-Select', icon: '☑☑' },
  { value: 'FILE',           label: 'File Upload',  icon: '↑'  },
];

const AUDIT_TYPES: { value: AuditType; label: string }[] = [
  { value: 'RICS_AUDIT', label: 'RICS Audit' }
];

@Component({
  selector: 'app-audit-form-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './audit-form-builder.component.html',
  styleUrl: './audit-form-builder.component.scss'
})
export class AuditFormBuilderComponent implements OnInit {

  templates:        AuditFormTemplate[] = [];
  selectedTemplate: AuditFormTemplate | null = null;
  selectedStep:     AuditFormStep | null = null;

  isLoadingTemplates = false;
  isSaving           = false;
  successMsg         = '';
  errorMsg           = '';

  showCreateTemplate  = false;
  createTemplateForm!: FormGroup;

  showAddStep  = false;
  addStepForm!: FormGroup;

  showAddField   = false;
  addFieldForm!: FormGroup;
  fieldOptions:  AddFieldOptionPayload[] = [];
  newOptLabel    = '';
  newOptValue    = '';

  // ── Edit field state
  editingFieldId:   number | null = null;
  editFieldForm!:   FormGroup;
  editFieldOptions: AddFieldOptionPayload[] = [];
  editOptLabel      = '';
  editOptValue      = '';

  // ── Drag state for steps
  dragStepIdx:     number | null = null;
  dragOverStepIdx: number | null = null;

  // ── Drag state for fields
  dragFieldIdx:     number | null = null;
  dragOverFieldIdx: number | null = null;

  readonly fieldTypes = FIELD_TYPES;
  readonly auditTypes = AUDIT_TYPES;

  hasOptions = (t: FieldType) =>
    ['DROPDOWN', 'RADIO', 'CHECKBOX', 'MULTI_CHECKBOX'].includes(t);

  constructor(
    private svc: AuditFormService,
    private fb:  FormBuilder,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.buildForms();
    this.loadTemplates();
  }

  private buildForms(): void {
    this.createTemplateForm = this.fb.group({
      auditType:   ['', Validators.required],
      title:       ['', Validators.required],
      description: ['']
    });
    this.addStepForm = this.fb.group({
      title:       ['', Validators.required],
      description: ['']
    });
    this.addFieldForm = this.fb.group({
      label:       ['', Validators.required],
      placeholder: [''],
      fieldType:   ['TEXT', Validators.required],
      required:    [false]
    });
    this.editFieldForm = this.fb.group({
      label:       ['', Validators.required],
      placeholder: [''],
      fieldType:   ['TEXT', Validators.required],
      required:    [false]
    });
  }

  loadTemplates(): void {
    this.isLoadingTemplates = true;
    this.svc.getAllTemplates().subscribe({
      next: t => {
        this.templates = t;
        this.isLoadingTemplates = false;

        // Re-sync selected template and step after reload
        if (this.selectedTemplate) {
          const fresh = t.find(x => x.id === this.selectedTemplate!.id);
          if (fresh) {
            this.selectedTemplate = fresh;
            if (this.selectedStep) {
              const freshStep = fresh.steps.find(s => s.id === this.selectedStep!.id);
              this.selectedStep = freshStep ?? null;
            }
          }
        }
        this.cdr.detectChanges();
      },
      error: () => { this.isLoadingTemplates = false; }
    });
  }

  selectTemplate(t: AuditFormTemplate): void {
    this.selectedTemplate = t;
    this.selectedStep     = null;
    this.showAddStep      = false;
    this.showAddField     = false;
    this.editingFieldId   = null;
  }

  selectStep(s: AuditFormStep): void {
    this.selectedStep   = s;
    this.showAddField   = false;
    this.editingFieldId = null;
  }

  // ════════════════════════════════════════
  // CREATE TEMPLATE
  // ════════════════════════════════════════

  submitCreateTemplate(): void {
    if (this.createTemplateForm.invalid) {
      this.createTemplateForm.markAllAsTouched(); return;
    }
    this.isSaving = true;
    this.svc.createTemplate(this.createTemplateForm.value).subscribe({
      next: t => {
        this.templates.push(t);
        this.showCreateTemplate = false;
        this.createTemplateForm.reset();
        this.isSaving = false;
        this.flash('Template created!');
        this.cdr.detectChanges();
      },
      error: err => {
        this.isSaving = false;
        this.errorMsg = typeof err.error === 'string'
          ? err.error : 'Failed to create template.';
      }
    });
  }

  // ════════════════════════════════════════
  // ADD STEP
  // ════════════════════════════════════════

  submitAddStep(): void {
    if (!this.selectedTemplate || this.addStepForm.invalid) {
      this.addStepForm.markAllAsTouched(); return;
    }
    this.isSaving = true;
    const payload = {
      ...this.addStepForm.value,
      stepOrder: (this.selectedTemplate.steps?.length ?? 0) + 1
    };
    this.svc.addStep(this.selectedTemplate.id, payload).subscribe({
      next: step => {
        this.selectedTemplate!.steps = this.selectedTemplate!.steps ?? [];
        this.selectedTemplate!.steps.push(step);
        this.showAddStep = false;
        this.addStepForm.reset();
        this.isSaving = false;
        this.flash('Step added.');
        this.cdr.detectChanges();
      },
      error: () => { this.isSaving = false; this.errorMsg = 'Failed to add step.'; }
    });
  }

  removeStep(step: AuditFormStep, e: Event): void {
    e.stopPropagation();
    if (!confirm(`Remove step "${step.title}"? This deletes all its fields.`)) return;
    this.svc.removeStep(step.id).subscribe({
      next: () => {
        this.selectedTemplate!.steps =
          this.selectedTemplate!.steps.filter(s => s.id !== step.id);
        if (this.selectedStep?.id === step.id) this.selectedStep = null;
        this.flash('Step removed.');
        this.cdr.detectChanges();
      },
      error: () => { this.errorMsg = 'Failed to remove step.'; }
    });
  }

  // ════════════════════════════════════════
  // ADD FIELD
  // ════════════════════════════════════════

  addOption(): void {
    if (!this.newOptLabel.trim()) return;
    this.fieldOptions.push({
      label:       this.newOptLabel.trim(),
      value:       this.newOptValue.trim() ||
                   this.newOptLabel.trim().toLowerCase().replace(/\s+/g, '_'),
      optionOrder: this.fieldOptions.length
    });
    this.newOptLabel = '';
    this.newOptValue = '';
  }

  removeOption(i: number): void { this.fieldOptions.splice(i, 1); }

  submitAddField(): void {
    if (!this.selectedStep || this.addFieldForm.invalid) {
      this.addFieldForm.markAllAsTouched(); return;
    }
    const ft = this.addFieldForm.value.fieldType as FieldType;
    if (this.hasOptions(ft) && this.fieldOptions.length === 0) {
      this.errorMsg = 'Please add at least one option for this field type.';
      return;
    }
    this.isSaving = true;
    const payload = {
      ...this.addFieldForm.value,
      fieldOrder: (this.selectedStep.fields?.length ?? 0) + 1,
      options:    this.hasOptions(ft) ? this.fieldOptions : []
    };
    this.svc.addField(this.selectedStep.id, payload).subscribe({
      next: field => {
        this.selectedStep!.fields = this.selectedStep!.fields ?? [];
        this.selectedStep!.fields.push(field);
        this.showAddField = false;
        this.addFieldForm.reset({
          label: '', placeholder: '', fieldType: 'TEXT', required: false
        });
        this.fieldOptions = [];
        this.isSaving = false;
        this.flash('Field added.');
        this.cdr.detectChanges();
      },
      error: () => { this.isSaving = false; this.errorMsg = 'Failed to add field.'; }
    });
  }

  cancelAddField(): void {
    this.showAddField = false;
    this.addFieldForm.reset({
      label: '', placeholder: '', fieldType: 'TEXT', required: false
    });
    this.fieldOptions = [];
    this.errorMsg     = '';
  }

  // ════════════════════════════════════════
  // EDIT FIELD
  // ════════════════════════════════════════

  startEditField(field: AuditFormField, e: Event): void {
    e.stopPropagation();
    this.editingFieldId = field.id;
    this.showAddField   = false;
    this.editFieldForm.patchValue({
      label:       field.label,
      placeholder: field.placeholder ?? '',
      fieldType:   field.fieldType,
      required:    field.required
    });
    this.editFieldOptions = field.options.map(o => ({
      label:       o.label,
      value:       o.value,
      optionOrder: o.optionOrder
    }));
    this.editOptLabel = '';
    this.editOptValue = '';
    this.errorMsg     = '';
  }

  cancelEditField(): void {
    this.editingFieldId   = null;
    this.editFieldOptions = [];
    this.errorMsg         = '';
  }

  addEditOption(): void {
    if (!this.editOptLabel.trim()) return;
    this.editFieldOptions.push({
      label:       this.editOptLabel.trim(),
      value:       this.editOptValue.trim() ||
                   this.editOptLabel.trim().toLowerCase().replace(/\s+/g, '_'),
      optionOrder: this.editFieldOptions.length
    });
    this.editOptLabel = '';
    this.editOptValue = '';
  }

  removeEditOption(i: number): void { this.editFieldOptions.splice(i, 1); }

  submitEditField(): void {
    if (!this.selectedStep || !this.editingFieldId || this.editFieldForm.invalid) {
      this.editFieldForm.markAllAsTouched(); return;
    }
    const ft = this.editFieldForm.value.fieldType as FieldType;
    if (this.hasOptions(ft) && this.editFieldOptions.length === 0) {
      this.errorMsg = 'Please add at least one option.';
      return;
    }

    this.isSaving  = true;
    const fieldId  = this.editingFieldId;
    const fieldIdx = this.selectedStep.fields.findIndex(f => f.id === fieldId);

    // Delete old → re-create at same position (no PATCH endpoint for fields)
    this.svc.removeField(fieldId).subscribe({
      next: () => {
        const payload = {
          ...this.editFieldForm.value,
          fieldOrder: fieldIdx + 1,
          options:    this.hasOptions(ft) ? this.editFieldOptions : []
        };
        this.svc.addField(this.selectedStep!.id, payload).subscribe({
          next: updated => {
            this.selectedStep!.fields.splice(fieldIdx, 1, updated);
            this.editingFieldId   = null;
            this.editFieldOptions = [];
            this.isSaving         = false;
            this.flash('Field updated.');
            this.cdr.detectChanges();
          },
          error: () => {
            this.isSaving = false;
            this.errorMsg = 'Failed to save updated field.';
          }
        });
      },
      error: () => {
        this.isSaving = false;
        this.errorMsg = 'Failed to update field.';
      }
    });
  }

  // ════════════════════════════════════════
  // REMOVE FIELD
  // ════════════════════════════════════════

  removeField(field: AuditFormField, e: Event): void {
    e.stopPropagation();
    if (!confirm(`Remove field "${field.label}"?`)) return;
    this.svc.removeField(field.id).subscribe({
      next: () => {
        this.selectedStep!.fields =
          this.selectedStep!.fields.filter(f => f.id !== field.id);
        if (this.editingFieldId === field.id) this.editingFieldId = null;
        this.flash('Field removed.');
        this.cdr.detectChanges();
      },
      error: () => { this.errorMsg = 'Failed to remove field.'; }
    });
  }

  // ════════════════════════════════════════
  // DRAG & DROP — STEPS (persisted)
  // ════════════════════════════════════════

  onStepDragStart(idx: number): void { this.dragStepIdx = idx; }

  onStepDragOver(e: DragEvent, idx: number): void {
    e.preventDefault();
    this.dragOverStepIdx = idx;
  }

  onStepDrop(e: DragEvent, toIdx: number): void {
    e.preventDefault();
    if (this.dragStepIdx === null || !this.selectedTemplate) return;

    const steps = [...this.selectedTemplate.steps];
    const [moved] = steps.splice(this.dragStepIdx, 1);
    steps.splice(toIdx, 0, moved);
    steps.forEach((s, i) => s.stepOrder = i + 1);

    this.selectedTemplate.steps = steps;
    this.dragStepIdx             = null;
    this.dragOverStepIdx         = null;
    this.cdr.detectChanges();

    // Persist new order to backend
    const payload = steps.map(s => ({ id: s.id, stepOrder: s.stepOrder }));
    this.svc.reorderSteps(this.selectedTemplate.id, payload).subscribe({
      next: () => this.flash('Step order saved.'),
      error: () => { this.errorMsg = 'Failed to save step order.'; }
    });
  }

  onStepDragEnd(): void {
    this.dragStepIdx     = null;
    this.dragOverStepIdx = null;
  }

  // ════════════════════════════════════════
  // DRAG & DROP — FIELDS (persisted)
  // ════════════════════════════════════════

  onFieldDragStart(idx: number): void { this.dragFieldIdx = idx; }

  onFieldDragOver(e: DragEvent, idx: number): void {
    e.preventDefault();
    this.dragOverFieldIdx = idx;
  }

  onFieldDrop(e: DragEvent, toIdx: number): void {
    e.preventDefault();
    if (this.dragFieldIdx === null || !this.selectedStep) return;

    const fields = [...this.selectedStep.fields];
    const [moved] = fields.splice(this.dragFieldIdx, 1);
    fields.splice(toIdx, 0, moved);
    fields.forEach((f, i) => f.fieldOrder = i + 1);

    this.selectedStep.fields = fields;
    this.dragFieldIdx         = null;
    this.dragOverFieldIdx     = null;
    this.cdr.detectChanges();

    // Persist new order to backend
    const payload = fields.map(f => ({ id: f.id, fieldOrder: f.fieldOrder }));
    this.svc.reorderFields(this.selectedStep.id, payload).subscribe({
      next: () => this.flash('Field order saved.'),
      error: () => { this.errorMsg = 'Failed to save field order.'; }
    });
  }

  onFieldDragEnd(): void {
    this.dragFieldIdx     = null;
    this.dragOverFieldIdx = null;
  }

  // ════════════════════════════════════════
  // HELPERS
  // ════════════════════════════════════════

  private flash(msg: string): void {
    this.successMsg = msg;
    this.errorMsg   = '';
    setTimeout(() => this.successMsg = '', 3500);
  }

  fieldTypeLabel(ft: FieldType): string {
    return this.fieldTypes.find(f => f.value === ft)?.label ?? ft;
  }

  fieldTypeIcon(ft: FieldType): string {
    return this.fieldTypes.find(f => f.value === ft)?.icon ?? '?';
  }

  get currentFieldType(): FieldType {
    return this.addFieldForm.get('fieldType')?.value as FieldType;
  }

  get editFieldType(): FieldType {
    return this.editFieldForm.get('fieldType')?.value as FieldType;
  }

  get cf() { return this.createTemplateForm.controls; }
  get sf() { return this.addStepForm.controls; }
  get ff() { return this.addFieldForm.controls; }
  get ef() { return this.editFieldForm.controls; }
}