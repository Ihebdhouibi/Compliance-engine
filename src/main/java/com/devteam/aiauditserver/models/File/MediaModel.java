package com.devteam.aiauditserver.models.File;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "media")
public class MediaModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;
    @Column(name = "file_name")
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "file_url")
    private String url;
    @Column(name = "file_type")
    private String type;

    @Column(name = "range")
    private Integer range;

    @Column(name = "share")
    private Boolean share;
    @Column(name = "file_size")
    private long size;
    @Column(name = "timestmp")
    private Date timestmp;


    public MediaModel() {
        this.timestmp = new Date();
    }

    public MediaModel(String name, String url, String type, long size) {
        this.name = name;
        this.url = url;
        this.type = type;
        this.size = size;
        this.timestmp = new Date();
    }

    @PreRemove
    public void preRemove() {


    }


    public Integer getRange() {
        return range;
    }

    public void setRange(Integer range) {
        this.range = range;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getShare() {
        return share;
    }

    public void setShare(Boolean share) {
        this.share = share;
    }

    public Date getTimestmp() {
        return timestmp;
    }


    public void setTimestmp(Date timestmp) {
        this.timestmp = timestmp;
    }

}
