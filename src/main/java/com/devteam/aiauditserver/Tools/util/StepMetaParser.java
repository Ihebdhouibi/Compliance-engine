package com.devteam.aiauditserver.Tools.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-side parser for the step-result {@code description} envelope produced by
 * the audit workspace:
 *
 * <pre>&lt;!--AUDIT_META_V1:{json}:END--&gt;\n{note}</pre>
 *
 * where {@code json} is
 * {@code { verdicts: {fieldId: verdict}, findings: Finding[], recommendations: Recommendation[] }}.
 *
 * Mirrors the TypeScript {@code composeStepBody}/parser in
 * {@code audit-workspace.component.ts} so the backend can diff changes
 * authoritatively, independent of the client.
 */
public final class StepMetaParser {

    public static final String META_OPEN  = "<!--AUDIT_META_V1:";
    public static final String META_CLOSE = ":END-->";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StepMetaParser() {}

    /** Flattened, comparable view of a step result's metadata. */
    public static final class ParsedStep {
        /** Plain text note (the part outside the meta envelope). */
        public String note = "";
        /** fieldId -> verdict value. */
        public final Map<String, String> verdicts = new LinkedHashMap<>();
        /** finding id -> "severity | description". */
        public final Map<String, String> findings = new LinkedHashMap<>();
        /** recommendation id -> description. */
        public final Map<String, String> recommendations = new LinkedHashMap<>();
    }

    public static ParsedStep parse(String description) {
        ParsedStep p = new ParsedStep();
        if (description == null) return p;

        int i = description.indexOf(META_OPEN);
        int j = i >= 0 ? description.indexOf(META_CLOSE, i) : -1;

        if (i >= 0 && j > i) {
            String json = description.substring(i + META_OPEN.length(), j);
            parseJson(json, p);
            p.note = (description.substring(0, i)
                    + description.substring(j + META_CLOSE.length())).trim();
        } else {
            p.note = description.trim();
        }
        return p;
    }

    private static void parseJson(String json, ParsedStep p) {
        try {
            JsonNode root = MAPPER.readTree(json);

            JsonNode verdicts = root.get("verdicts");
            if (verdicts != null && verdicts.isObject()) {
                verdicts.fields().forEachRemaining(e ->
                        p.verdicts.put(e.getKey(), e.getValue().asText()));
            }

            JsonNode findings = root.get("findings");
            if (findings != null && findings.isArray()) {
                int idx = 0;
                for (JsonNode f : findings) {
                    String id = text(f, "id", "f" + idx++);
                    p.findings.put(id,
                            text(f, "severity", "") + " | " + text(f, "description", ""));
                }
            }

            JsonNode recs = root.get("recommendations");
            if (recs != null && recs.isArray()) {
                int idx = 0;
                for (JsonNode r : recs) {
                    String id = text(r, "id", "r" + idx++);
                    p.recommendations.put(id, text(r, "description", ""));
                }
            }
        } catch (Exception ignore) {
            // Malformed metadata: treat as no structured content.
        }
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? fallback : v.asText();
    }
}
