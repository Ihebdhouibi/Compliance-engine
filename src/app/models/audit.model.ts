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

// ── NEW: two-level types
export type AuditLevel = 'LEVEL_1' | 'LEVEL_2';
export type AuditPhase =
  | 'DRAFT_L1' | 'L1_SUBMITTED' | 'ROUTED' | 'QUOTE_ACCEPTED'
  | 'DRAFT_L2' | 'L2_SUBMITTED' | 'ASSIGNED' | 'IN_PROGRESS'
  | 'COMPLETED' | 'LEGACY';
export type Pathway = 'PATHWAY_1' | 'PATHWAY_2' | 'PATHWAY_3' | 'PATHWAY_4';

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
  // ── NEW: two-level metadata (all optional, backward compatible)
  fieldKey?:           string;
  optionSourceKey?:    string;
  visibilityRule?:     string;
  routingTags?:        string;
  ricsClause?:         string;
  module?:             string;
  category?:           string;
  applicabilityTrigger?: string;
  evidenceDepth?:      string;
  priority?:           string;
  expectedEvidence?:   string;
  rationale?:          string;
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
  // ── NEW: two-level metadata
  level?:           AuditLevel;
  templateVersion?: number;
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
  fieldKey?:    string;
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
  // ── NEW: two-level fields
  phase?:            AuditPhase;
  level1TemplateId?: number;
  level2TemplateId?: number;
  routingProfile?:   RoutingProfile;
}

// ── NEW two-level shapes ─────────────────────────────────────────────

export interface OptionListItem {
  value: string;
  label: string;
}

export interface OptionList {
  key:   string;
  label: string;
  items: OptionListItem[];
}

export interface IndicativePrice {
  min:      number;
  max:      number;
  currency: string;   // e.g. "GBP"
  status:   string;   // e.g. "INDICATIVE"
}

export interface RoutingProfile {
  id:                   number;
  pathway:              Pathway;
  recommendedPackage?:  string;
  activeModules?:       string;     // comma-separated
  evidenceDepth?:       string;
  indicativePriceMin?:  number;
  indicativePriceMax?:  number;
  priceCurrency?:       string;
  priceStatus?:         string;
  rationale?:           string;
  firmSizeCategory?:    string;
  sectorCategory?:      string;
  aiAdoptionCategory?:  string;
  aiImpactCategory?:    string;
  dataSensitivityCategory?:    string;
  regulatoryExposureCategory?: string;
  computedAt?:          string;
  confirmedAt?:         string;
}

export interface SubmitTwoLevelAnswer {
  fieldId?:    number;
  fieldKey?:   string;
  fieldLabel?: string;
  answerValue?: string;
}

export interface SubmitTwoLevelPayload {
  auditType?: AuditType;
  answers:    SubmitTwoLevelAnswer[];
}

export interface ScoreAnswerPayload {
  auditorScore:           number;        // 0-5
  auditorNotes?:          string;
  evidenceProvidedStatus?: string;
  evidenceReference?:     string;
}

export interface AuditAnswerScoring {
  id:                      number;
  auditorScore:            number;
  auditorNotes?:           string;
  evidenceProvidedStatus?: string;
  evidenceReference?:      string;
  scoredAt?:               string;
  scoredBy?:               User;
  answer?:                 AuditRequestAnswer;
}

// ── Audit change journal (history of edits to completed audits)
export type AuditJournalChangeType = 'VERDICT' | 'FINDING' | 'RECOMMENDATION' | 'NOTE';
export type AuditJournalAction     = 'ADDED' | 'MODIFIED' | 'REMOVED';

export interface AuditJournalEntry {
  id:         number;
  changedBy?: User;
  changedAt:  string;
  stepName?:  string;
  changeType: AuditJournalChangeType;
  action:     AuditJournalAction;
  fieldRef?:  string;
  oldValue?:  string;
  newValue?:  string;
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
