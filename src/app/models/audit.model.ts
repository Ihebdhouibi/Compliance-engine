import { MediaModel } from './media.model';
import { User } from './user.model';

export type AuditType =
  | 'AI_READINESS_REVIEW'
  | 'INTERNAL_AI_GOVERNANCE'
  | 'RESPONSIBLE_AI_ASSURANCE'
  | 'RICS_RESPONSIBLE_AI';
export type AuditStatus = 'SUBMITTED' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';
export type FieldType   =
  | 'TEXT' | 'TEXTAREA' | 'NUMBER' | 'EMAIL' | 'DATE'
  | 'DROPDOWN' | 'RADIO' | 'CHECKBOX' | 'MULTI_CHECKBOX' | 'FILE';

export interface AuditFormFieldOption {
  id:           number;
  label:        string;
  value:        string;
  optionOrder:  number;
}

export interface AuditFormField {
  id:             number;
  fieldOrder:     number;
  label:          string;
  placeholder?:   string;
  fieldType:      FieldType;
  required:       boolean;
  multipleFiles?: boolean;
  options:        AuditFormFieldOption[];
}

export interface AuditFormStep {
  id:           number;
  stepOrder:    number;
  title:        string;
  description?: string;
  fields:       AuditFormField[];
}

export interface AuditFormTemplate {
  id:           number;
  auditType:    AuditType;
  title:        string;
  description?: string;
  active:       boolean;
  createdAt:    Date;
  steps:        AuditFormStep[];
  processSteps?: AuditProcessStep[];  // ADD — populated when fetched with process steps
}

// ── NEW: audit process steps defined by admin on the template
export interface AuditProcessStep {
  id:          number;
  name:        string;
  stepOrder:   number;
  isDefault:   boolean;
  createdAt?:  string;
}

// ── NEW: result filled by auditor per step per request
export interface AuditStepResult {
  id:           number;
  stepName:     string;
  processStep?: { id: number; name: string; stepOrder: number };
  description:  string;
  status:       'DRAFT' | 'SAVED';
  filledBy?:    User;
  createdAt:    string;
  updatedAt:    string;
}

// ── NEW: lightweight template ref returned inside AuditRequest
export interface AuditRequestTemplate {
  id:           number;
  title:        string;
  auditType:    AuditType;
  processSteps?: AuditProcessStep[];
}

export interface AuditRequestAnswerFile {
  id:        number;
  media:     MediaModel;
  fileOrder: number;
}

export interface AuditRequestAnswer {
  id:           number;
  fieldId:      number;
  fieldLabel:   string;
  answerValue?: string;
  fileMedia?:   MediaModel;
  files?:       AuditRequestAnswerFile[];
}

export interface AuditRequest {
  id:               number;
  auditType:        AuditType;
  status:           AuditStatus;
  template?:        AuditRequestTemplate;  // ADD — linked template with processSteps
  submittedBy:      User;
  assignedTo?:      User;
  dueDate?:         Date;
  submittedAt:      Date;
  assignedAt?:      Date;
  completedAt?:     Date;
  rejectionReason?: string;
  answers:          AuditRequestAnswer[];
}

// ── Request payloads — all unchanged
export interface SubmitAuditAnswerPayload {
  fieldId:      number;
  fieldLabel:   string;
  answerValue?: string;
}

export interface SubmitAuditPayload {
  auditType: AuditType;
  answers:   SubmitAuditAnswerPayload[];
}

export interface AssignAuditPayload {
  assignedToUserId: number;
  dueDate?:         string;
}

export interface CreateTemplatePayload {
  auditType:    AuditType;
  title:        string;
  description?: string;
}

export interface AddStepPayload {
  title:        string;
  description?: string;
  stepOrder?:   number;
}

export interface AddFieldOptionPayload {
  label:        string;
  value:        string;
  optionOrder?: number;
}

export interface AddFieldPayload {
  label:          string;
  placeholder?:   string;
  fieldType:      FieldType;
  required?:      boolean;
  multipleFiles?: boolean;
  fieldOrder?:    number;
  options?:       AddFieldOptionPayload[];
}

// ── Step result payloads
export interface SaveStepResultPayload {
  processStepId: number;
  stepName:      string;
  description:   string;
  status:        'DRAFT' | 'SAVED';
}

export interface AuditProcessStep {
  id: number;
  name: string;
  stepOrder: number;
  isDefault: boolean;
  createdAt?: string;
}