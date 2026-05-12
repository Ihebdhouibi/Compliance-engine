package com.devteam.aiauditserver.requests.project;


import com.devteam.aiauditserver.enums.Project.FieldType;

import java.util.List;

public class AddFieldRequest {
    private String label;
    private String placeholder;
    private FieldType fieldType;
    private Boolean required = false;
    private Boolean multipleFiles = false;
    private Integer fieldOrder;
    private List<FieldOptionRequest> options;

    public static class FieldOptionRequest {
        private String label;
        private String value;
        private Integer optionOrder;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public Integer getOptionOrder() { return optionOrder; }
        public void setOptionOrder(Integer optionOrder) { this.optionOrder = optionOrder; }
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }

    public FieldType getFieldType() { return fieldType; }
    public void setFieldType(FieldType fieldType) { this.fieldType = fieldType; }

    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }

    public Boolean getMultipleFiles() { return multipleFiles; }
    public void setMultipleFiles(Boolean multipleFiles) { this.multipleFiles = multipleFiles; }

    public Integer getFieldOrder() { return fieldOrder; }
    public void setFieldOrder(Integer fieldOrder) { this.fieldOrder = fieldOrder; }

    public List<FieldOptionRequest> getOptions() { return options; }
    public void setOptions(List<FieldOptionRequest> options) { this.options = options; }
}
