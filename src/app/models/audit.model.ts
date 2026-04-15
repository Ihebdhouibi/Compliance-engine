import { MediaModel } from './media.model';
import { User } from './user.model';

export type AuditType   = 'RICS_AUDIT';
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
  id:           number;
  fieldOrder:   number;
  label:        string;
  placeholder?: string;
  fieldType:    FieldType;
  required:     boolean;
  options:      AuditFormFieldOption[];
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
}

export interface AuditRequestAnswer {
  id:           number;
  fieldId:      number;
  fieldLabel:   string;
  answerValue?: string;
  fileMedia?:   MediaModel;   // populated only for FILE-type fields
}

export interface AuditRequest {
  id:               number;
  auditType:        AuditType;
  status:           AuditStatus;
  submittedBy:      User;
  assignedTo?:      User;
  dueDate?:         Date;
  submittedAt:      Date;
  assignedAt?:      Date;
  completedAt?:     Date;
  rejectionReason?: string;
  answers:          AuditRequestAnswer[];
}

// ── Request payloads
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
  label:        string;
  placeholder?: string;
  fieldType:    FieldType;
  required?:    boolean;
  fieldOrder?:  number;
  options?:     AddFieldOptionPayload[];
}