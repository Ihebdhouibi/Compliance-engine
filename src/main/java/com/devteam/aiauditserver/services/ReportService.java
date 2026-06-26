package com.devteam.aiauditserver.services;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswer;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswerFile;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditStepResult;
import com.devteam.aiauditserver.repositories.project.AuditRequestRepository;
import com.devteam.aiauditserver.repositories.project.AuditRequestAnswerRepository;
import com.devteam.aiauditserver.repositories.project.AuditStepResultRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Chunk;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final String META_OPEN = "<!--AUDIT_META_V1:";
    private static final String META_CLOSE = ":END-->";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AuditRequestRepository auditRequestRepository;

    @Autowired
    private AuditRequestAnswerRepository answerRepository;

    @Autowired
    private AuditStepResultRepository stepResultRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ByteArrayInputStream generateAuditReport(Long auditId) {
        try {
            AuditRequest request = auditRequestRepository.findById(auditId)
                    .orElseThrow(() -> new RuntimeException("Audit not found"));

            List<AuditStepResult> stepResults = stepResultRepository.findByAuditRequestId(auditId);

            // ─── Fetch answers with media eagerly ────────────────────────
            List<AuditRequestAnswer> answers = fetchAnswersWithMedia(auditId);

            // ─── 1. Build step → field IDs from template ───────────────
            Map<String, List<Long>> stepFieldIds = buildStepFieldIds(request.getAuditType().name());

            // ─── 2. Sort and deduplicate steps ───────────────────────────
            stepResults.sort(Comparator.comparing(
                AuditStepResult::getStepName,
                Comparator.nullsLast(Comparator.naturalOrder())
            ));

            Set<String> seenSteps = new HashSet<>();
            List<AuditStepResult> uniqueSteps = new ArrayList<>();
            for (AuditStepResult sr : stepResults) {
                String name = sr.getStepName();
                if (name == null) {
                    if (seenSteps.add("__NULL__")) uniqueSteps.add(sr);
                } else if (seenSteps.add(name)) {
                    uniqueSteps.add(sr);
                }
            }
            stepResults = uniqueSteps;

            // ─── 3. Build answer index by fieldId ────────────────────────
            Map<Long, AuditRequestAnswer> answersByFieldId = new HashMap<>();
            for (AuditRequestAnswer ans : answers) {
                answersByFieldId.put(ans.getFieldId(), ans);
            }

            // ─── 4. PDF creation ─────────────────────────────────────────
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Audit Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);

            String company = "N/A";
            if (request.getSubmittedBy() != null && request.getSubmittedBy().getCompanyInfo() != null) {
                company = request.getSubmittedBy().getCompanyInfo().getCompanyName();
            }
            document.add(new Paragraph("Audit ID: " + request.getId(), normalFont));
            document.add(new Paragraph("Company: " + company, normalFont));
            document.add(new Paragraph("Type: " + request.getAuditType(), normalFont));
            document.add(new Paragraph("Status: " + request.getStatus(), normalFont));
            document.add(new Paragraph("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), normalFont));
            document.add(new Paragraph(" "));
            document.add(new Chunk("--------------------------------------------------"));

            // ─── 5. Loop over each step ───────────────────────────────────
            for (AuditStepResult stepResult : stepResults) {
                document.add(new Paragraph(" "));
                Font stepFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                Paragraph stepTitle = new Paragraph("Step: " + stepResult.getStepName(), stepFont);
                document.add(stepTitle);

                String rawDescription = stepResult.getDescription() != null ? stepResult.getDescription() : "";
                String description = cleanDescription(rawDescription);
                StepMeta meta = parseStepMeta(description);
                String note = extractNote(description);
                // Also clean the note itself
                if (note != null) note = cleanDescription(note);

                if (note != null && !note.isEmpty()) {
                    document.add(new Paragraph("Note: " + note, normalFont));
                }

                // ─── Get all field IDs for this step from template ────────
                List<Long> fieldIds = stepFieldIds.getOrDefault(stepResult.getStepName(), new ArrayList<>());
                if (fieldIds.isEmpty()) {
                    fieldIds = new ArrayList<>(meta.verdicts.keySet());
                }
                Collections.sort(fieldIds);

                if (fieldIds.isEmpty()) {
                    document.add(new Paragraph("No questions mapped to this step.", italicFont));
                    document.add(new Paragraph(" "));
                    document.add(new Chunk("---"));
                    continue;
                }

                document.add(new Paragraph("Questions & Answers", boldFont));
                document.add(new Paragraph(" "));

                for (Long fieldId : fieldIds) {
                    AuditRequestAnswer answer = answersByFieldId.get(fieldId);
                    if (answer == null) continue;

                    String questionLabel = answer.getFieldLabel() != null ? answer.getFieldLabel() : "Question " + fieldId;
                    String answerValue = answer.getAnswerValue() != null ? answer.getAnswerValue() : "—";
                    String verdict = meta.verdicts.getOrDefault(fieldId, "N/A");

                    document.add(new Paragraph(questionLabel, boldFont));
                    document.add(new Paragraph("Answer: " + answerValue, normalFont));
                    document.add(new Paragraph("Verdict: " + verdict, normalFont));

                    // ─── Evidence with file names ──────────────────────────
                    List<MediaModel> evidences = getEvidenceForAnswer(answer);
                    if (!evidences.isEmpty()) {
                        document.add(new Paragraph("Evidence:", italicFont));
                        for (MediaModel ev : evidences) {
                            String fileName = ev.getName();
                            if (fileName == null || fileName.trim().isEmpty()) {
                                fileName = "Evidence #" + ev.getId();
                            }
                            document.add(new Paragraph("  - " + fileName, normalFont));
                        }
                    }
                    document.add(new Paragraph(" "));
                }

                // ─── Findings ──────────────────────────────────────────────
                if (!meta.findings.isEmpty()) {
                    document.add(new Paragraph("Findings", boldFont));
                    for (Finding f : meta.findings) {
                        document.add(new Paragraph("• Severity: " + f.severity, normalFont));
                        document.add(new Paragraph("  " + f.description, normalFont));
                        if (f.note != null && !f.note.isEmpty()) {
                            document.add(new Paragraph("  Note: " + f.note, italicFont));
                        }
                        document.add(new Paragraph(" "));
                    }
                }

                // ─── Recommendations ──────────────────────────────────────
                if (!meta.recommendations.isEmpty()) {
                    document.add(new Paragraph("Recommendations", boldFont));
                    for (Recommendation r : meta.recommendations) {
                        document.add(new Paragraph("• " + r.description, normalFont));
                        document.add(new Paragraph(" "));
                    }
                }

                document.add(new Paragraph(" "));
                document.add(new Chunk("---"));
            }

            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("End of report - Generated by Compliance Engine", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            logger.error("Error generating PDF report for audit " + auditId, e);
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
        }
    }

    // ─── Fetch answers with media eagerly ──────────────────────────────
    private List<AuditRequestAnswer> fetchAnswersWithMedia(Long auditId) {
        String jpql = "SELECT a FROM AuditRequestAnswer a " +
                      "LEFT JOIN FETCH a.fileMedia " +
                      "LEFT JOIN FETCH a.files f " +
                      "LEFT JOIN FETCH f.media " +
                      "WHERE a.auditRequest.id = :auditId";
        return entityManager.createQuery(jpql, AuditRequestAnswer.class)
                .setParameter("auditId", auditId)
                .getResultList();
    }

    // ─── Build step → field IDs from template using EntityManager ────
    private Map<String, List<Long>> buildStepFieldIds(String auditType) {
        Map<String, List<Long>> map = new LinkedHashMap<>();
        try {
            String jpql = "SELECT t FROM AuditFormTemplate t WHERE t.auditType = :auditType";
            List<AuditFormTemplate> templates = entityManager.createQuery(jpql, AuditFormTemplate.class)
                    .setParameter("auditType", auditType)
                    .getResultList();

            if (!templates.isEmpty()) {
                AuditFormTemplate template = templates.get(0);
                if (template.getSteps() != null) {
                    for (var step : template.getSteps()) {
                        String stepName = step.getTitle();
                        List<Long> fieldIds = step.getFields() != null
                                ? step.getFields().stream().map(f -> f.getId()).collect(Collectors.toList())
                                : new ArrayList<>();
                        map.put(stepName, fieldIds);
                    }
                }
            } else {
                logger.warn("No template found for audit type '{}' – using verdict keys as fallback", auditType);
            }
        } catch (Exception e) {
            logger.warn("Could not retrieve template for audit type '{}' – using verdict keys as fallback", auditType, e);
        }
        return map;
    }

    // ─── Clean OCR artifacts (removes lines with text[[...]]) ────────
    private String cleanDescription(String description) {
        if (description == null) return "";
        return description.replaceAll("(?m)^.*text\\[\\[.*?\\]\\].*$", "").trim();
    }

    // ===== INNER CLASSES =====
    private static class StepMeta {
        Map<Long, String> verdicts = new HashMap<>();
        List<Finding> findings = new ArrayList<>();
        List<Recommendation> recommendations = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    private static class Finding {
        String id;
        String severity;
        String description;
        String note;
    }

    @SuppressWarnings("unused")
    private static class Recommendation {
        String id;
        String description;
    }

    // ===== PARSING =====
    private StepMeta parseStepMeta(String description) {
        StepMeta meta = new StepMeta();
        int openIdx = description.indexOf(META_OPEN);
        if (openIdx == -1) return meta;
        int closeIdx = description.indexOf(META_CLOSE, openIdx + META_OPEN.length());
        if (closeIdx == -1) return meta;

        String json = description.substring(openIdx + META_OPEN.length(), closeIdx);
        try {
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            Object verdictsObj = root.get("verdicts");
            if (verdictsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> vMap = (Map<String, String>) verdictsObj;
                for (Map.Entry<String, String> entry : vMap.entrySet()) {
                    try {
                        meta.verdicts.put(Long.parseLong(entry.getKey()), entry.getValue());
                    } catch (NumberFormatException e) {
                        logger.warn("Skipping verdict with non-numeric key: {}", entry.getKey());
                    }
                }
            }

            Object findingsObj = root.get("findings");
            if (findingsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> fList = (List<Map<String, Object>>) findingsObj;
                for (Map<String, Object> fMap : fList) {
                    Finding f = new Finding();
                    f.id = (String) fMap.get("id");
                    f.severity = (String) fMap.get("severity");
                    f.description = (String) fMap.get("description");
                    f.note = (String) fMap.get("note");
                    meta.findings.add(f);
                }
            }

            Object recsObj = root.get("recommendations");
            if (recsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rList = (List<Map<String, Object>>) recsObj;
                for (Map<String, Object> rMap : rList) {
                    Recommendation r = new Recommendation();
                    r.id = (String) rMap.get("id");
                    r.description = (String) rMap.get("description");
                    meta.recommendations.add(r);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse step meta: " + e.getMessage());
        }
        return meta;
    }

    private String extractNote(String description) {
        int openIdx = description.indexOf(META_OPEN);
        if (openIdx == -1) return description.trim();
        int closeIdx = description.indexOf(META_CLOSE, openIdx + META_OPEN.length());
        if (closeIdx == -1) return description.trim();
        String note = (description.substring(0, openIdx) + description.substring(closeIdx + META_CLOSE.length())).trim();
        return note.isEmpty() ? null : note;
    }

    private List<MediaModel> getEvidenceForAnswer(AuditRequestAnswer answer) {
        List<MediaModel> evidences = new ArrayList<>();
        if (answer.getFileMedia() != null) {
            evidences.add(answer.getFileMedia());
        }
        if (answer.getFiles() != null) {
            for (AuditRequestAnswerFile file : answer.getFiles()) {
                if (file.getMedia() != null) {
                    evidences.add(file.getMedia());
                }
            }
        }
        return evidences;
    }
}
