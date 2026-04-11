package com.dagdockersim.service.impl;

import com.dagdockersim.constant.CacheConstant;
import com.dagdockersim.model.request.DeviceRegisterRequest;
import com.dagdockersim.model.request.TelemetrySubmitRequest;
import com.dagdockersim.model.vo.RegisterDeviceVO;
import com.dagdockersim.model.vo.TelemetrySubmitVO;
import com.dagdockersim.service.SimulationCommandService;
import com.dagdockersim.service.impl.support.SimulationVoAssembler;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
public class SimulationCommandServiceImpl implements SimulationCommandService {
    private final SimulationRuntimeContext runtimeContext;
    private final SimulationVoAssembler simulationVoAssembler;

    public SimulationCommandServiceImpl(
        SimulationRuntimeContext runtimeContext,
        SimulationVoAssembler simulationVoAssembler
    ) {
        this.runtimeContext = runtimeContext;
        this.simulationVoAssembler = simulationVoAssembler;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConstant.SIMULATION_HEALTH, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.SIMULATION_TOPOLOGY, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.CLOUD_LEDGER, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.FUSION_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.FUSION_LEDGER, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.DEVICE_LIST, allEntries = true)
    })
    public RegisterDeviceVO registerDevice(String terminalId, DeviceRegisterRequest request) {
        return simulationVoAssembler.toRegisterDeviceVO(runtimeContext.registerDevice(terminalId, request));
    }

    @Override
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheConstant.SIMULATION_HEALTH, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.SIMULATION_TOPOLOGY, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.CLOUD_LEDGER, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.FUSION_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.FUSION_LEDGER, allEntries = true),
        @CacheEvict(cacheNames = CacheConstant.DEVICE_LIST, allEntries = true)
    })
    public TelemetrySubmitVO submitTelemetry(String terminalId, String deviceId, TelemetrySubmitRequest request) {
        return simulationVoAssembler.toTelemetrySubmitVO(runtimeContext.submitTelemetry(terminalId, deviceId, request));
    }
}
