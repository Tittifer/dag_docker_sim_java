package com.dagdockersim.service.impl.support;

import com.dagdockersim.mapper.LedgerTransactionRepository;
import com.dagdockersim.model.domain.Transaction;
import com.dagdockersim.model.entity.LedgerTransactionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class LedgerStateStore {
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final TransactionPersistenceMapper transactionPersistenceMapper;

    public LedgerStateStore(
        LedgerTransactionRepository ledgerTransactionRepository,
        TransactionPersistenceMapper transactionPersistenceMapper
    ) {
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.transactionPersistenceMapper = transactionPersistenceMapper;
    }

    public boolean hasTerminalSnapshot(String terminalId) {
        return ledgerTransactionRepository.existsByTerminalId(terminalId);
    }

    public List<Transaction> loadLedger(String terminalId) {
        List<LedgerTransactionEntity> entities = ledgerTransactionRepository.findByTerminalIdOrderByTimestampAscTxIdAsc(terminalId);
        List<Transaction> transactions = new ArrayList<Transaction>();
        for (LedgerTransactionEntity entity : entities) {
            transactions.add(transactionPersistenceMapper.toTransaction(entity));
        }
        return transactions;
    }

    @Transactional
    public void replaceLedger(String terminalId, Collection<Transaction> transactions) {
        ledgerTransactionRepository.deleteByTerminalId(terminalId);
        ledgerTransactionRepository.flush();
        List<LedgerTransactionEntity> entities = new ArrayList<LedgerTransactionEntity>();
        for (Transaction transaction : transactions) {
            entities.add(transactionPersistenceMapper.toEntity(terminalId, transaction));
        }
        ledgerTransactionRepository.saveAll(entities);
        ledgerTransactionRepository.flush();
    }
}
