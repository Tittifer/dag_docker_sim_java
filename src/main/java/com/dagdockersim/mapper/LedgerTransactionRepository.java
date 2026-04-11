package com.dagdockersim.mapper;

import com.dagdockersim.model.entity.LedgerTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransactionEntity, Long> {
    boolean existsByTerminalId(String terminalId);

    List<LedgerTransactionEntity> findByTerminalIdOrderByTimestampAscTxIdAsc(String terminalId);

    void deleteByTerminalId(String terminalId);
}
