package com.tracktrove.dto;

import lombok.Data; // For getters, setters, equals, hashCode, toString
import lombok.NoArgsConstructor; // For no-argument constructor
import lombok.AllArgsConstructor; // For all-argument constructor

import jakarta.validation.constraints.DecimalMin; // For validation
import jakarta.validation.constraints.NotBlank; // For validation
import jakarta.validation.constraints.NotNull; // For validation

import java.math.BigDecimal; // For amount
import java.util.UUID; // For vendorId

@Data // Lombok: Generates getters, setters, equals, hashCode, toString
@NoArgsConstructor // Lombok: Generates no-argument constructor
@AllArgsConstructor // Lombok: Generates all-argument constructor
public class TransactionDTO {

    @NotNull(message = "Vendor ID cannot be null")
    private UUID vendorId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00") // As per your concept (₹1–₹500)
    private BigDecimal amount;

    @NotBlank(message = "Currency cannot be blank")
    private String currency; // e.g., "INR", "USD"

    @NotBlank(message = "Channel cannot be blank")
    private String channel; // e.g., "WEB", "MOBILE", "API"

    // Initial payload can be a JSON string from the frontend
    @NotBlank(message = "Initial payload cannot be blank")
    private String initialPayloadJson;

    @NotNull(message = "Simulated success rate cannot be null")
    private Double simulatedSuccessRate; // Probability for simulation (e.g., 0.8 for 80%)

    private String serviceContext;

    // You might add more fields here if your frontend sends more data for a new transaction
}
