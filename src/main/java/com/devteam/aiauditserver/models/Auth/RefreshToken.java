package com.devteam.aiauditserver.models.Auth;


import com.devteam.aiauditserver.models.User.User;
import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

@Entity
@Table(name = "Refresh_Token")
public class RefreshToken implements Serializable {
    @Id
    @GeneratedValue(strategy =   GenerationType.SEQUENCE)
    @Column(name = "refresh_id"  )
    private Long id;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token" ,nullable = false, unique = true)
    private String token;

    @Column(name = "expiryDate",nullable = false)
    private Instant expiryDate;
    @Column(name = "timestmp")
    private Date timestmp;
    public RefreshToken() {
        this.timestmp= new Date();
    }

    public RefreshToken(User user, String token, Instant expiryDate) {
        this.user = user;
        this.token = token;
        this.expiryDate = expiryDate;
        this.timestmp= new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getTimestmp() {
        return timestmp;
    }

    public void setTimestmp(Date timestmp) {
        this.timestmp = timestmp;
    }
}
