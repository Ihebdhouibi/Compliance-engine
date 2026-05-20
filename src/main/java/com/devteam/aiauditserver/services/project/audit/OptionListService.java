package com.devteam.aiauditserver.services.project.audit;

import com.devteam.aiauditserver.models.project.AuditForm.AuditFormOptionList;
import com.devteam.aiauditserver.repositories.project.AuditFormOptionListRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OptionListService {

    private final AuditFormOptionListRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    public OptionListService(AuditFormOptionListRepository repo) {
        this.repo = repo;
    }

    public List<AuditFormOptionList> getAll() {
        return repo.findAll();
    }

    public Map<String, Object> getByKey(String key) {
        AuditFormOptionList list = repo.findByListKey(key).orElse(null);
        if (list == null) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("key", list.getListKey());
        out.put("label", list.getLabel());
        try {
            JsonNode items = mapper.readTree(list.getItemsJson());
            out.put("items", mapper.convertValue(items, List.class));
        } catch (Exception e) {
            out.put("items", Collections.emptyList());
        }
        return out;
    }
}
