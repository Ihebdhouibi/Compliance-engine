# Qdrant Collection Schema — `rics_standards`

## Overview

The `rics_standards` collection is the core knowledge base powering the Compliance Engine platform. It stores all mandatory requirements from the **RICS Professional Standard: Responsible use of artificial intelligence in surveying practice** (1st edition, September 2025, effective 9 March 2026).

Every entry is embedded using **BAAI/bge-small-en-v1.5** (384-dimensional vectors, cosine distance) and enriched with structured metadata payloads that the platform's AI Helper, Evidence Finder, and Report Builder can consume directly.

---

## Collection Statistics

| Property | Value |
|----------|-------|
| Collection name | `rics_standards` |
| Total points | **57** |
| Vector dimensions | 384 |
| Distance metric | Cosine |
| Embedding model | `BAAI/bge-small-en-v1.5` (FastEmbed) |
| Rules | 47 (all mandatory — "must") |
| Definitions | 8 (2 document definitions + 6 glossary terms) |
| Context entries | 2 (scope/applicability) |

---

## Entry Types

The collection contains four categories of entries, distinguished by the `entry_type` payload field:

| `entry_type` | Count | Description |
|--------------|-------|-------------|
| `rule` | 47 | Mandatory RICS requirements extracted from Sections 1–5 |
| `document_definition` | 2 | Formal definitions of RICS document categories (Professional Standards, Practice Information) |
| `glossary` | 6 | Key terms defined in the RICS glossary (AI System, Failure Mode, etc.) |
| `scope_context` | 2 | Scope and applicability context (materiality threshold, global jurisdiction) |

---

## Payload Schema — Rules

Each **rule** entry carries the following payload fields:

| Payload Field | Type | Purpose | Example (RICS-S3.1-06) |
|---------------|------|---------|------------------------|
| `entry_type` | `string` | Categorises the entry | `"rule"` |
| `rule_id` | `string` | Unique rule identifier | `"RICS-S3.1-06"` |
| `section` | `string` | RICS section number | `"3.1"` |
| `section_title` | `string` | Section heading | `"Data governance"` |
| `parent_section` | `string` | Parent section number | `"3"` |
| `parent_section_title` | `string` | Parent section heading | `"Practice management"` |
| `requirement_type` | `string` | Obligation level | `"mandatory"` |
| `obligation_keyword` | `string` | The word used in the RICS text | `"must"` |
| `applies_to` | `string` | Who the rule binds | `"regulated_firms"` |
| `requirement_text` | `string` | Structured, readable version of the requirement for UI display | *"Firms must refrain from uploading private and confidential data to AI systems, except where BOTH: (a) there is express written consent..."* |
| `trigger` | `string` | The condition or event that activates this rule | *"Any proposed upload of private or confidential data to an AI system."* |
| `evidence_implied` | `string` | What an auditor should look for to verify compliance | *"Written consent records from stakeholders; risk assessment for data uploads; upload logs with justification."* |
| `source_text_verbatim` | `string` | **Exact original RICS wording** — citable as regulatory evidence in reports | *"refraining from uploading private and confidential data to AI systems, except where: there is express written consent in advance from affected stakeholders..."* |
| `related_rules` | `string[]` | IDs of related rules for cross-referencing | `["RICS-S3.1-01"]` |
| `related_definitions` | `string[]` | IDs of related glossary/definition entries | `["GLOSS-03"]` |
| `topic_tags` | `string[]` | Searchable topic labels | `["data_governance", "data_upload", "consent", "risk_assessment"]` |
| `parent_rule` | `string` | *(optional)* ID of the parent rule when this is a sub-requirement | `"RICS-S3.1-01"` |

## Payload Schema — Definitions & Glossary

| Payload Field | Type | Purpose |
|---------------|------|---------|
| `entry_type` | `string` | `"document_definition"` or `"glossary"` |
| `rule_id` | `string` | Unique identifier (e.g. `DEF-01`, `GLOSS-03`) |
| `term` | `string` | The defined term |
| `text` | `string` | Full definition text |
| `source` | `string` | *(optional)* External source reference (e.g. OECD, RICS Valuation standards) |
| `topic_tags` | `string[]` | Searchable topic labels |

## Payload Schema — Context Entries

| Payload Field | Type | Purpose |
|---------------|------|---------|
| `entry_type` | `string` | `"scope_context"` |
| `rule_id` | `string` | Unique identifier (e.g. `CONTEXT-01`) |
| `section` | `string` | RICS section number |
| `title` | `string` | Descriptive title |
| `text` | `string` | Full context text |
| `topic_tags` | `string[]` | Searchable topic labels |

---

## Indexed Payload Fields

The following payload fields have **keyword indexes** in Qdrant, enabling fast filtered search without scanning all vectors:

| Indexed Field | Schema Type | Use Case |
|---------------|-------------|----------|
| `entry_type` | `KEYWORD` | Filter by entry category (rule, glossary, definition, context) |
| `section` | `KEYWORD` | Filter by RICS section (e.g. `"3.1"`, `"4.2"`) |
| `requirement_type` | `KEYWORD` | Filter by obligation level (currently all `"mandatory"`) |
| `applies_to` | `KEYWORD` | Filter by audience (`"members"`, `"regulated_firms"`, `"members_and_firms"`) |
| `obligation_keyword` | `KEYWORD` | Filter by obligation word (`"must"`) |
| `rule_id` | `KEYWORD` | Direct lookup by rule ID |

---

## What the Collection Provides to the Platform

### 1. Evidence Retrieval (Evidence Finder)

When an auditor needs to verify a client's compliance, the platform queries the collection and returns:

- **`source_text_verbatim`** — The exact RICS paragraph that can be cited in audit reports as regulatory evidence. This is the authoritative text, not a paraphrase.
- **`evidence_implied`** — A structured description of what documents, records, or artefacts the auditor should request from the client to demonstrate compliance.

### 2. Requirement Matching (Chat Assistant / AI Helper)

When an auditor asks a natural-language question (e.g. *"What are the data privacy requirements?"*), the platform:

1. Embeds the question using the same model (`bge-small-en-v1.5`)
2. Performs **semantic search** against the collection — finding relevant rules even if the wording differs from the RICS text
3. Optionally applies **payload filters** (by section, applies_to, entry_type) to narrow results
4. Returns `requirement_text` for human-readable display and `trigger` to determine applicability

### 3. Rule Navigation & Cross-Referencing (Document Finder)

The structured relationships between entries enable multi-hop navigation:

- **`related_rules`** — Follow from one rule to related requirements (e.g. S3.2-04 → S3.2-05, S3.2-06, S3.2-07, S3.2-08 for all policy sub-requirements)
- **`parent_rule`** — Navigate from a sub-requirement up to its parent
- **`related_definitions`** — Link rules to their relevant glossary definitions (e.g. S3.1-06 → GLOSS-03 for the definition of "Private and Confidential Information")
- **`topic_tags`** — Discover all rules related to a topic (e.g. all `"risk_register"` rules across sections)

### 4. Report Generation (Report Builder)

The Report Builder can assemble compliance reports by:

- Pulling `source_text_verbatim` as the regulatory citation
- Using `evidence_implied` to structure the evidence checklist
- Grouping rules by `section` / `parent_section` for report structure
- Indicating `applies_to` to determine which rules apply to the entity under audit
- Using `requirement_type` and `obligation_keyword` to distinguish mandatory vs recommended (for future standards that include "should" recommendations)

### 5. Contextual Grounding

Every AI response can be **grounded** — the LLM never invents regulatory requirements. Instead it retrieves them from the collection and cites the exact RICS source text. This ensures:

- **Accuracy** — no hallucinated rules
- **Traceability** — every recommendation links back to a specific `rule_id` and `source_text_verbatim`
- **Auditability** — the platform's own reasoning is auditable against the RICS standard

---

## Embedding Strategy

Each entry's embedding is computed from a **composite text** that maximises retrieval quality:

| Entry Type | Embedded Text Formula |
|------------|----------------------|
| Rule | `Section {section} - {section_title}` \| `requirement_text` \| `Trigger: {trigger}` \| `Evidence: {evidence_implied}` |
| Glossary / Definition | `{term}: {text}` |
| Context | `{title}: {text}` |

This approach ensures that searches match on section context, requirement content, trigger conditions, and evidence descriptions simultaneously.

---

## Search Capabilities

### Semantic Search (unfiltered)
Find rules by meaning, not keywords:
> *"What are the requirements for data privacy?"* → GLOSS-03, S3.1-02, S3.1-03, S3.1-05, S5-06

### Filtered Search (by section)
Narrow to a specific RICS section:
> *"output reliability"* + `section=4.2` → S4.2-02, S4.2-01, S4.2-03, S4.2-04, S4.2-06

### Filtered Search (by audience)
Only rules applicable to firms vs individual members:
> *"risk register"* + `applies_to=regulated_firms` → S3.3-03, S3.3-02, S3.3-01, S3.3-04

### Filtered Search (by entry type)
Retrieve only definitions or only rules:
> *"What is an AI system?"* + `entry_type=glossary` → GLOSS-01 (score 0.88)

### Combined Filters
Chain multiple filters:
> *"supplier due diligence"* + `section=4.1` + `entry_type=rule` → S4.1-04, S4.1-01, S4.1-02, S4.1-03

### Direct Lookup
Fetch a specific rule by ID:
> `rule_id=RICS-S3.1-06` → Full payload with verbatim source text and evidence

---

## Accessing the Collection

- **Dashboard**: `http://localhost:6333/dashboard` → collection `rics_standards`
- **REST API**: `http://localhost:6333/collections/rics_standards`
- **Python client**: `QdrantClient(url="http://localhost:6333")`
- **gRPC**: `localhost:6334`
