package com.devteam.aiauditserver.requests.project;

public class UpdateDashboardDesignRequest {

    private String projectTitle;
    private String sideNavbarColor;
    private String topNavbarColor;
    private String backgroundColor;
    private String accentColor;
    private String addButtonColor;
    private String updateButtonColor;
    private String deleteButtonColor;

    public UpdateDashboardDesignRequest() {}

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getSideNavbarColor() { return sideNavbarColor; }
    public void setSideNavbarColor(String sideNavbarColor) { this.sideNavbarColor = sideNavbarColor; }

    public String getTopNavbarColor() { return topNavbarColor; }
    public void setTopNavbarColor(String topNavbarColor) { this.topNavbarColor = topNavbarColor; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

    public String getAddButtonColor() { return addButtonColor; }
    public void setAddButtonColor(String addButtonColor) { this.addButtonColor = addButtonColor; }

    public String getUpdateButtonColor() { return updateButtonColor; }
    public void setUpdateButtonColor(String updateButtonColor) { this.updateButtonColor = updateButtonColor; }

    public String getDeleteButtonColor() { return deleteButtonColor; }
    public void setDeleteButtonColor(String deleteButtonColor) { this.deleteButtonColor = deleteButtonColor; }
}