package com.devteam.aiauditserver.models.project;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "DashboardDesignSettings")
public class DashboardDesignSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "favicon_media")
    private MediaModel favIcon;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "logo_media")
    private MediaModel logo;

    @Column(name = "project_title")
    private String projectTitle = "AuditAI";

    @Column(name = "side_navbar_color")
    private String sideNavbarColor = "#071528";

    @Column(name = "top_navbar_color")
    private String topNavbarColor = "#071528";

    @Column(name = "background_color")
    private String backgroundColor = "#020912";

    @Column(name = "accent_color")
    private String accentColor = "#00d4ed";

    @Column(name = "add_button_color")
    private String addButtonColor = "#00d4ed";

    @Column(name = "update_button_color")
    private String updateButtonColor = "#00bcd4";

    @Column(name = "delete_button_color")
    private String deleteButtonColor = "#ff4d6a";

    @Column(name = "timestamp")
    private Date timestamp = new Date();

    // ── Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MediaModel getFavIcon() { return favIcon; }
    public void setFavIcon(MediaModel favIcon) { this.favIcon = favIcon; }

    public MediaModel getLogo() { return logo; }
    public void setLogo(MediaModel logo) { this.logo = logo; }

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

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}