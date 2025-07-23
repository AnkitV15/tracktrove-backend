package com.tracktrove.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

// Import for Hibernate Types JSON mapping
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID transactionId;
    private String stepName;

    // Apply @JdbcTypeCode(SqlTypes.JSON) to JSONB fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dto_before", columnDefinition = "jsonb")
    private String dtoBefore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dto_after", columnDefinition = "jsonb")
    private String dtoAfter;

    @Column(columnDefinition = "text")
    private String errorStack;

    private Integer retryCount;

    private Instant traceTime;
}
