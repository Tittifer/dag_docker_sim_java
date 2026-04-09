package com.dagdockersim.simulation.api;

import com.dagdockersim.simulation.api.dto.DeviceRegisterRequest;
import com.dagdockersim.simulation.api.dto.TelemetrySubmitRequest;
import com.dagdockersim.simulation.application.command.SimulationCommandService;
import com.dagdockersim.simulation.application.query.SimulationQueryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

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
    public Map<String, Object> health() {
        return queryService.health();
    }

    @GetMapping("/topology")
    public Map<String, Object> topology() {
        return queryService.topology();
    }

    @GetMapping("/cloud/ledger")
    public Map<String, Object> cloudLedger() {
        return queryService.cloudLedger();
    }

    @GetMapping("/fusions")
    public List<Map<String, Object>> fusions() {
        return queryService.listFusions();
    }

    @GetMapping("/fusions/{terminalId}/ledger")
    public Map<String, Object> fusionLedger(@PathVariable String terminalId) {
        return queryService.fusionLedger(terminalId);
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> devices() {
        return queryService.listDevices();
    }

    @PostMapping("/fusions/{terminalId}/devices/register")
    public Map<String, Object> registerDevice(
        @PathVariable String terminalId,
        @Valid @RequestBody DeviceRegisterRequest request
    ) {
        return commandService.registerDevice(terminalId, request);
    }

    @PostMapping("/fusions/{terminalId}/devices/{deviceId}/telemetry")
    public Map<String, Object> submitTelemetry(
        @PathVariable String terminalId,
        @PathVariable String deviceId,
        @RequestBody(required = false) TelemetrySubmitRequest request
    ) {
        TelemetrySubmitRequest actualRequest = request == null ? new TelemetrySubmitRequest() : request;
        return commandService.submitTelemetry(terminalId, deviceId, actualRequest);
    }
}

