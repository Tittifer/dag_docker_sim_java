package com.dagdockersim.api;

import com.dagdockersim.api.dto.DeviceRegisterRequest;
import com.dagdockersim.api.dto.TelemetrySubmitRequest;
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
    private final SimulationRuntimeService runtimeService;

    public SimulationController(SimulationRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return runtimeService.health();
    }

    @GetMapping("/topology")
    public Map<String, Object> topology() {
        return runtimeService.topology();
    }

    @GetMapping("/cloud/ledger")
    public Map<String, Object> cloudLedger() {
        return runtimeService.cloudLedger();
    }

    @GetMapping("/fusions")
    public List<Map<String, Object>> fusions() {
        return runtimeService.listFusions();
    }

    @GetMapping("/fusions/{terminalId}/ledger")
    public Map<String, Object> fusionLedger(@PathVariable String terminalId) {
        return runtimeService.fusionLedger(terminalId);
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> devices() {
        return runtimeService.listDevices();
    }

    @PostMapping("/fusions/{terminalId}/devices/register")
    public Map<String, Object> registerDevice(
        @PathVariable String terminalId,
        @Valid @RequestBody DeviceRegisterRequest request
    ) {
        return runtimeService.registerDevice(terminalId, request);
    }

    @PostMapping("/fusions/{terminalId}/devices/{deviceId}/telemetry")
    public Map<String, Object> submitTelemetry(
        @PathVariable String terminalId,
        @PathVariable String deviceId,
        @RequestBody(required = false) TelemetrySubmitRequest request
    ) {
        TelemetrySubmitRequest actualRequest = request == null ? new TelemetrySubmitRequest() : request;
        return runtimeService.submitTelemetry(terminalId, deviceId, actualRequest);
    }
}
