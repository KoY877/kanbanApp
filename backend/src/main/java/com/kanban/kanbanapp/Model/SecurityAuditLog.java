package com.kanban.kanbanapp.Model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "security_audit_logs")
public class SecurityAuditLog {
    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;
    
    private String userId;           // Who performed the action
    private String username;
    private String action;            // LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, etc.
    private String ipAddress;
    private String userAgent;
    private String details;           // JSON with additional context
    
    @CreationTimestamp
    private Instant timestamp;
    
    // Optional: Risk level (LOW, MEDIUM, HIGH)
    private String severity;
}
