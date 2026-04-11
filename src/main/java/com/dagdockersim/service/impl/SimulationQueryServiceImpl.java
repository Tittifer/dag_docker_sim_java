package com.dagdockersim.service.impl;

import com.dagdockersim.constant.CacheConstant;
import com.dagdockersim.model.vo.DeviceVO;
import com.dagdockersim.model.vo.FusionSummaryVO;
import com.dagdockersim.model.vo.HealthVO;
import com.dagdockersim.model.vo.LedgerViewVO;
import com.dagdockersim.model.vo.TopologyVO;
import com.dagdockersim.service.SimulationQueryService;
import com.dagdockersim.service.impl.support.SimulationVoAssembler;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimulationQueryServiceImpl implements SimulationQueryService {
    private final SimulationRuntimeContext runtimeContext;
    private final SimulationVoAssembler simulationVoAssembler;

    public SimulationQueryServiceImpl(
        SimulationRuntimeContext runtimeContext,
        SimulationVoAssembler simulationVoAssembler
    ) {
        this.runtimeContext = runtimeContext;
        this.simulationVoAssembler = simulationVoAssembler;
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.SIMULATION_HEALTH)
    public HealthVO health() {
        return simulationVoAssembler.toHealthVO(runtimeContext.health());
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.SIMULATION_TOPOLOGY)
    public TopologyVO topology() {
        return simulationVoAssembler.toTopologyVO(runtimeContext.topology());
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.CLOUD_LEDGER)
    public LedgerViewVO cloudLedger() {
        return simulationVoAssembler.toLedgerViewVO(runtimeContext.cloudLedger());
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.FUSION_LIST)
    public List<FusionSummaryVO> listFusions() {
        return simulationVoAssembler.toFusionSummaryList(runtimeContext.listFusions());
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.FUSION_LEDGER, key = "#terminalId")
    public LedgerViewVO fusionLedger(String terminalId) {
        return simulationVoAssembler.toLedgerViewVO(runtimeContext.fusionLedger(terminalId));
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.DEVICE_LIST)
    public List<DeviceVO> listDevices() {
        return simulationVoAssembler.toDeviceList(runtimeContext.listDevices());
    }
}
