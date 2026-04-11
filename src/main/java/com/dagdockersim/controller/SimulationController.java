package com.dagdockersim.controller;

import com.dagdockersim.common.BaseResponse;
import com.dagdockersim.common.ResultUtils;
import com.dagdockersim.model.request.DeviceRegisterRequest;
import com.dagdockersim.model.request.TelemetrySubmitRequest;
import com.dagdockersim.model.vo.DeviceVO;
import com.dagdockersim.model.vo.FusionSummaryVO;
import com.dagdockersim.model.vo.HealthVO;
import com.dagdockersim.model.vo.LedgerViewVO;
import com.dagdockersim.model.vo.RegisterDeviceVO;
import com.dagdockersim.model.vo.TelemetrySubmitVO;
import com.dagdockersim.model.vo.TopologyVO;
import com.dagdockersim.service.SimulationCommandService;
import com.dagdockersim.service.SimulationQueryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
public class SimulationController {
    private final SimulationQueryService queryService;
    private final SimulationCommandService commandService;

    public SimulationController(
        SimulationQueryService queryService,
        SimulationCommandService commandService
    ) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @GetMapping("/health")
    public BaseResponse<HealthVO> health() {
        return ResultUtils.success(queryService.health());
    }

    @GetMapping("/topology")
    public BaseResponse<TopologyVO> topology() {
        return ResultUtils.success(queryService.topology());
    }

    @GetMapping("/cloud/ledger")
    public BaseResponse<LedgerViewVO> cloudLedger() {
        return ResultUtils.success(queryService.cloudLedger());
    }

    @GetMapping("/fusions")
    public BaseResponse<List<FusionSummaryVO>> fusions() {
        return ResultUtils.success(queryService.listFusions());
    }

    @GetMapping("/fusions/{terminalId}/ledger")
    public BaseResponse<LedgerViewVO> fusionLedger(@PathVariable String terminalId) {
        return ResultUtils.success(queryService.fusionLedger(terminalId));
    }

    @GetMapping("/devices")
    public BaseResponse<List<DeviceVO>> devices() {
        return ResultUtils.success(queryService.listDevices());
    }

    @PostMapping("/fusions/{terminalId}/devices/register")
    public BaseResponse<RegisterDeviceVO> registerDevice(
        @PathVariable String terminalId,
        @Valid @RequestBody DeviceRegisterRequest request
    ) {
        return ResultUtils.success(commandService.registerDevice(terminalId, request));
    }

    @PostMapping("/fusions/{terminalId}/devices/{deviceId}/telemetry")
    public BaseResponse<TelemetrySubmitVO> submitTelemetry(
        @PathVariable String terminalId,
        @PathVariable String deviceId,
        @RequestBody(required = false) TelemetrySubmitRequest request
    ) {
        TelemetrySubmitRequest actualRequest = request == null ? new TelemetrySubmitRequest() : request;
        return ResultUtils.success(commandService.submitTelemetry(terminalId, deviceId, actualRequest));
    }
}

