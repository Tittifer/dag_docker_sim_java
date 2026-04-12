package com.dagdockersim.service.impl.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class TerminalTaskDispatcher {
    private final Map<String, ExecutorService> executors = new LinkedHashMap<String, ExecutorService>();

    public TerminalTaskDispatcher(List<String> terminalIds) {
        for (String terminalId : terminalIds) {
            executors.put(
                terminalId,
                Executors.newSingleThreadExecutor(namedThreadFactory("terminal-worker-" + terminalId))
            );
        }
    }

    public <T> T call(String terminalId, Callable<T> task) {
        ExecutorService executor = requireExecutor(terminalId);
        return await(executor.submit(task));
    }

    public void run(String terminalId, Runnable task) {
        ExecutorService executor = requireExecutor(terminalId);
        await(executor.submit(task, Boolean.TRUE));
    }

    public void shutdown() {
        for (ExecutorService executor : executors.values()) {
            executor.shutdown();
        }
    }

    private ExecutorService requireExecutor(String terminalId) {
        ExecutorService executor = executors.get(terminalId);
        if (executor == null) {
            throw new IllegalArgumentException("unknown_terminal_executor");
        }
        return executor;
    }

    private <T> T await(Future<T> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("terminal_task_failed", cause == null ? exception : cause);
        }
    }

    private ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
