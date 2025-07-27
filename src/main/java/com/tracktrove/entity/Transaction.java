package com.tracktrove.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tracktrove.entity.enums.TransactionStatus;

// Import for Hibernate Types JSON mapping
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private TransactionStatus currentStatus;

    // Use @JdbcTypeCode(SqlTypes.JSON) for JSONB columns
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "initial_payload", columnDefinition = "jsonb") // Keep columnDefinition for schema generation
    private String initialPayload; // Still store as String, but Hibernate will handle JSON conversion

    private String serviceContext;
    private Double simulatedSuccessRate;

    private UUID vendorId;
    private String channel;

    private Integer retryCount;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if(this.retryCount == null) {
            this.retryCount = 0;
        }
    }

    @jakarta.persistence.PreUpdate
    protected void voidOnUpdate() { // Changed method name to avoid conflict if any, or ensure it's unique
        this.updatedAt = Instant.now();
    }
}
