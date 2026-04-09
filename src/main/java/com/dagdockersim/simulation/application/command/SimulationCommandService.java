package com.dagdockersim.simulation.application.command;

import com.dagdockersim.simulation.api.dto.DeviceRegisterRequest;
import com.dagdockersim.simulation.api.dto.TelemetrySubmitRequest;
import com.dagdockersim.simulation.application.runtime.SimulationRuntimeContext;
import com.dagdockersim.simulation.infrastructure.cache.SimulationCacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SimulationCommandService {
    private final SimulationRuntimeContext runtimeContext;

    public SimulationCommandService(SimulationRuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = SimulationCacheNames.SIMULATION_HEALTH, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.SIMULATION_TOPOLOGY, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.CLOUD_LEDGER, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.FUSION_LIST, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.FUSION_LEDGER, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.DEVICE_LIST, allEntries = true)
    })
    public Map<String, Object> registerDevice(String terminalId, DeviceRegisterRequest request) {
        return runtimeContext.registerDevice(terminalId, request);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = SimulationCacheNames.SIMULATION_HEALTH, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.SIMULATION_TOPOLOGY, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.CLOUD_LEDGER, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.FUSION_LIST, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.FUSION_LEDGER, allEntries = true),
        @CacheEvict(cacheNames = SimulationCacheNames.DEVICE_LIST, allEntries = true)
    })
    public Map<String, Object> submitTelemetry(String terminalId, String deviceId, TelemetrySubmitRequest request) {
        return runtimeContext.submitTelemetry(terminalId, deviceId, request);
    }
}
