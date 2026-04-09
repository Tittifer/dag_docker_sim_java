package com.dagdockersim.simulation.application.query;

import com.dagdockersim.simulation.application.runtime.SimulationRuntimeContext;
import com.dagdockersim.simulation.infrastructure.cache.SimulationCacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SimulationQueryService {
    private final SimulationRuntimeContext runtimeContext;

    public SimulationQueryService(SimulationRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    @Cacheable(cacheNames = SimulationCacheNames.SIMULATION_HEALTH)
    public Map<String, Object> health() {
        return runtimeContext.health();
    }

    @Cacheable(cacheNames = SimulationCacheNames.SIMULATION_TOPOLOGY)
    public Map<String, Object> topology() {
        return runtimeContext.topology();
    }

    @Cacheable(cacheNames = SimulationCacheNames.CLOUD_LEDGER)
    public Map<String, Object> cloudLedger() {
        return runtimeContext.cloudLedger();
    }

    @Cacheable(cacheNames = SimulationCacheNames.FUSION_LIST)
    public List<Map<String, Object>> listFusions() {
        return runtimeContext.listFusions();
    }

    @Cacheable(cacheNames = SimulationCacheNames.FUSION_LEDGER, key = "#terminalId")
    public Map<String, Object> fusionLedger(String terminalId) {
        return runtimeContext.fusionLedger(terminalId);
    }

    @Cacheable(cacheNames = SimulationCacheNames.DEVICE_LIST)
    public List<Map<String, Object>> listDevices() {
        return runtimeContext.listDevices();
    }
}
