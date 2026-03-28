package com.kanban.kanbanapp.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @EqualsAndHashCode.Include
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    // Relation to User (many-to-one)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(unique = true, nullable = false, length = 512)
    @NaturalId
    private String token;

    // Links rotated tokens together
    @Column(nullable = false, length = 36)
    private String tokenFamily;  

    // Mark as revoked when rotated
    @Column(nullable = false)
    private boolean revoked = false;  

    // When it was rotated/revoked
    private Instant revokedAt;  

    // Expiry date of the refresh token
    @Column(nullable = false)
    private Instant expiryDate;

    // timestamps
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdDate;
}