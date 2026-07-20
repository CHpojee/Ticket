package com.standardinsurance.itsupport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    /** Marker stored in {@link #approver} for a system approver. */
    public static final String APPROVER_FLAG = "Y";

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    /** 'Y' when the user is a system approver; otherwise NULL. */
    @Column(length = 1)
    private String approver;

    /** Approval stage this approver acts at: 1 = first approval, 2 = second approval. */
    @Column(name = "approver_level")
    private Integer approverLevel;

    @Column(name = "email_address")
    private String emailAddress;

    protected User() {
        // JPA
    }

    public User(String userId, String password, String name) {
        this(userId, password, name, null, null, null);
    }

    public User(String userId, String password, String name, String approver,
                String emailAddress) {
        this(userId, password, name, approver, null, emailAddress);
    }

    public User(String userId, String password, String name, String approver,
                Integer approverLevel, String emailAddress) {
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.approver = approver;
        this.approverLevel = approverLevel;
        this.emailAddress = emailAddress;
    }

    /** A user is a system approver if and only if {@code approver == 'Y'}. */
    public boolean isApprover() {
        return APPROVER_FLAG.equals(approver);
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public Integer getApproverLevel() {
        return approverLevel;
    }

    public void setApproverLevel(Integer approverLevel) {
        this.approverLevel = approverLevel;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
