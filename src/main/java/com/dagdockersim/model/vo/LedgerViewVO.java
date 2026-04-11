package com.dagdockersim.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LedgerViewVO {
    @JsonProperty("terminal_id")
    private String terminalId;

    private Map<String, Object> summary;
    private List<Map<String, Object>> transactions;

    @JsonProperty("archive_size")
    private Integer archiveSize;

    private Map<String, Object> storage;

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

    public List<Map<String, Object>> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Map<String, Object>> transactions) {
        this.transactions = transactions;
    }

    public Integer getArchiveSize() {
        return archiveSize;
    }

    public void setArchiveSize(Integer archiveSize) {
        this.archiveSize = archiveSize;
    }

    public Map<String, Object> getStorage() {
        return storage;
    }

    public void setStorage(Map<String, Object> storage) {
        this.storage = storage;
    }
}
