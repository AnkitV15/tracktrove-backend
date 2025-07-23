package com.tracktrove.entity;

import com.tracktrove.entity.enums.LedgerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    private LedgerType type;

    private Double amount;
    private LocalDateTime entryTimestamp;
    private String description;
}
