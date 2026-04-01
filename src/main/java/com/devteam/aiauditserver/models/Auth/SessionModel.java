package com.devteam.aiauditserver.models.Auth;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "SessionModel")
public class SessionModel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "session_id" )
    private Long id;
    @Column(name = "session_token")
    private String token;
    @Column(name = "session_ip")
    private String ip;
    @Column(name = "session_email")
    private String email;
    @Column(name = "session_user_id")
    private Long userid;
    @Column(name = "session_username")
    private String username;
    @Column(name = "timestmp")
    private Date timestmp;

    public SessionModel() {
        this.timestmp= new Date();
    }

    public SessionModel(String token, String ip, String email, String username,Long userid) {
        this.token = token;
        this.ip = ip;
        this.email = email;
        this.username = username;
        this.userid=userid;
        this.timestmp= new Date();

    }

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getTimestmp() {
        return timestmp;
    }

    public void setTimestmp(Date timestmp) {
        this.timestmp = timestmp;
    }
    
}
