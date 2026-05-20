package com.devteam.aiauditserver.models.project.AuditForm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;

/**
 * Shared list of selectable options (dropdowns / radios / multi-checkboxes)
 * referenced by AuditFormField.optionSourceKey. Items are stored as a JSON
 * array string of {@code [{"value":"...","label":"..."}]} to keep the
 * schema simple and avoid an extra join table for the static lists shipped
 * by the L1 seed.
 */
@Entity
@Table(name = "audit_form_option_lists",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_option_list_key",
                columnNames = {"list_key"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditFormOptionList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "list_key", nullable = false, length = 120)
    private String listKey;

    @Column(name = "label")
    private String label;

    /** JSON: [{"value":"...","label":"..."}, ...] */
    @Column(name = "items_json", nullable = false, columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "updated_at")
    private Date updatedAt = new Date();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getListKey() { return listKey; }
    public void setListKey(String listKey) { this.listKey = listKey; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
