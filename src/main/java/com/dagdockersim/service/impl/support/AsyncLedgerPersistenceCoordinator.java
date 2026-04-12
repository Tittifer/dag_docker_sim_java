package com.dagdockersim.service.impl.support;

import com.dagdockersim.model.domain.Transaction;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AsyncLedgerPersistenceCoordinator {
    private static final Logger LOGGER = Logger.getLogger(AsyncLedgerPersistenceCoordinator.class.getName());

    private final LedgerStateStore ledgerStateStore;
    private final ExecutorService executor;
    private final AtomicLong submittedBatchCount = new AtomicLong();
    private final AtomicLong completedBatchCount = new AtomicLong();
    private final AtomicReference<String> lastError = new AtomicReference<String>();
    private final AtomicReference<Future<?>> latestTask = new AtomicReference<Future<?>>();

    public AsyncLedgerPersistenceCoordinator(LedgerStateStore ledgerStateStore) {
        this.ledgerStateStore = ledgerStateStore;
        this.executor = Executors.newSingleThreadExecutor(namedThreadFactory("ledger-persistence"));
    }

    public void persistLedgers(Map<String, List<Transaction>> ledgerSnapshots) {
        final Map<String, List<Transaction>> immutableBatch = new LinkedHashMap<String, List<Transaction>>(ledgerSnapshots);
        final long batchId = submittedBatchCount.incrementAndGet();

        Future<?> future = executor.submit(() -> {
            try {
                for (Map.Entry<String, List<Transaction>> entry : immutableBatch.entrySet()) {
                    ledgerStateStore.replaceLedger(entry.getKey(), entry.getValue());
                }
                completedBatchCount.incrementAndGet();
                lastError.set(null);
            } catch (Exception exception) {
                lastError.set(exception.getMessage());
                LOGGER.log(Level.SEVERE, "ledger_async_persistence_failed batchId=" + batchId, exception);
                throw exception;
            }
        });
        latestTask.set(future);
    }

    public void persistLedgersAndWait(Map<String, List<Transaction>> ledgerSnapshots) {
        persistLedgers(ledgerSnapshots);
        flushLatestTask();
    }

    public Map<String, Object> summary() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("mode", "async_mysql_snapshot");
        response.put("submitted_batches", Long.valueOf(submittedBatchCount.get()));
        response.put("completed_batches", Long.valueOf(completedBatchCount.get()));
        response.put("last_error", lastError.get());
        return response;
    }

    @PreDestroy
    public void shutdown() {
        flushLatestTask();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private void flushLatestTask() {
        Future<?> future = latestTask.get();
        if (future == null) {
            return;
        }
        try {
            future.get();
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "ledger_async_persistence_flush_failed", exception);
        }
    }

    private ThreadFactory namedThreadFactory(String prefix) {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix);
            thread.setDaemon(true);
            return thread;
        };
    }
}
