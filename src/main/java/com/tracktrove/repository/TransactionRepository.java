package com.tracktrove.repository;

import com.tracktrove.entity.Trace;
import com.tracktrove.entity.Transaction;
import com.tracktrove.entity.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByCurrentStatus(TransactionStatus currentStatus);
}
