package com.dagdockersim.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class FusionSummaryVO {
    @JsonProperty("terminal_id")
    private String terminalId;

    private Map<String, Object> summary;

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary;
    }
}
