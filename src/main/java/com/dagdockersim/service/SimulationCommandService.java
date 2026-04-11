package com.dagdockersim.service;

import com.dagdockersim.model.request.DeviceRegisterRequest;
import com.dagdockersim.model.request.TelemetrySubmitRequest;
import com.dagdockersim.model.vo.RegisterDeviceVO;
import com.dagdockersim.model.vo.TelemetrySubmitVO;

public interface SimulationCommandService {
    RegisterDeviceVO registerDevice(String terminalId, DeviceRegisterRequest request);

    TelemetrySubmitVO submitTelemetry(String terminalId, String deviceId, TelemetrySubmitRequest request);
}
