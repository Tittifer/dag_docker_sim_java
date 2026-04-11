package com.dagdockersim.service;

import com.dagdockersim.model.vo.DeviceVO;
import com.dagdockersim.model.vo.FusionSummaryVO;
import com.dagdockersim.model.vo.HealthVO;
import com.dagdockersim.model.vo.LedgerViewVO;
import com.dagdockersim.model.vo.TopologyVO;

import java.util.List;

public interface SimulationQueryService {
    HealthVO health();

    TopologyVO topology();

    LedgerViewVO cloudLedger();

    List<FusionSummaryVO> listFusions();

    LedgerViewVO fusionLedger(String terminalId);

    List<DeviceVO> listDevices();
}
