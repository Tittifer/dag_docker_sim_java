package com.dagdockersim.simulation.api.dto;

import java.util.Map;

public class TelemetrySubmitRequest {
    private Map<String, Object> dataPayload;

    public Map<String, Object> getDataPayload() {
        return dataPayload;
    }

    public void setDataPayload(Map<String, Object> dataPayload) {
        this.dataPayload = dataPayload;
    }
}

