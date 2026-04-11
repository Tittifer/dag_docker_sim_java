package com.dagdockersim.service.impl.support;

import com.dagdockersim.model.vo.DeviceVO;
import com.dagdockersim.model.vo.FusionSummaryVO;
import com.dagdockersim.model.vo.HealthVO;
import com.dagdockersim.model.vo.LedgerViewVO;
import com.dagdockersim.model.vo.RegisterDeviceVO;
import com.dagdockersim.model.vo.TelemetrySubmitVO;
import com.dagdockersim.model.vo.TopologyVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SimulationVoAssembler {
    public HealthVO toHealthVO(Map<String, Object> source) {
        HealthVO vo = new HealthVO();
        vo.setService(stringValue(source.get("service")));
        vo.setMode(stringValue(source.get("mode")));
        vo.setStorage(mapValue(source.get("storage")));
        vo.setCloud(mapValue(source.get("cloud")));
        vo.setFusions(toFusionSummaryList(listValue(source.get("fusions"))));
        vo.setSimulatedDeviceCount(intValue(source.get("simulated_device_count")));
        return vo;
    }

    public TopologyVO toTopologyVO(Map<String, Object> source) {
        TopologyVO vo = new TopologyVO();
        vo.setStorage(mapValue(source.get("storage")));
        vo.setCloud(mapValue(source.get("cloud")));
        vo.setFusions(toFusionSummaryList(listValue(source.get("fusions"))));
        vo.setDevices(toDeviceList(listValue(source.get("devices"))));
        return vo;
    }

    public LedgerViewVO toLedgerViewVO(Map<String, Object> source) {
        LedgerViewVO vo = new LedgerViewVO();
        vo.setTerminalId(stringValue(source.get("terminal_id")));
        vo.setSummary(mapValue(source.get("summary")));
        vo.setTransactions(listValue(source.get("transactions")));
        vo.setArchiveSize(intValue(source.get("archive_size")));
        vo.setStorage(mapValue(source.get("storage")));
        return vo;
    }

    public List<FusionSummaryVO> toFusionSummaryList(List<Map<String, Object>> source) {
        List<FusionSummaryVO> result = new ArrayList<FusionSummaryVO>();
        if (source == null) {
            return result;
        }
        for (Map<String, Object> item : source) {
            FusionSummaryVO vo = new FusionSummaryVO();
            vo.setTerminalId(stringValue(item.get("terminal_id")));
            vo.setSummary(mapValue(item.get("summary")));
            result.add(vo);
        }
        return result;
    }

    public List<DeviceVO> toDeviceList(List<Map<String, Object>> source) {
        List<DeviceVO> result = new ArrayList<DeviceVO>();
        if (source == null) {
            return result;
        }
        for (Map<String, Object> item : source) {
            DeviceVO vo = new DeviceVO();
            vo.setDeviceId(stringValue(item.get("device_id")));
            vo.setDeviceName(stringValue(item.get("device_name")));
            vo.setTerminalId(stringValue(item.get("terminal_id")));
            vo.setSignPubkey(stringValue(item.get("sign_pubkey")));
            vo.setBootstrapIdentity(booleanValue(item.get("bootstrap_identity")));
            result.add(vo);
        }
        return result;
    }

    public RegisterDeviceVO toRegisterDeviceVO(Map<String, Object> source) {
        RegisterDeviceVO vo = new RegisterDeviceVO();
        vo.setAccepted(booleanValue(source.get("accepted")));
        vo.setDeviceName(stringValue(source.get("device_name")));
        vo.setDeviceId(stringValue(source.get("device_id")));
        vo.setTerminalId(stringValue(source.get("terminal_id")));
        vo.setRegisterTx(mapValue(source.get("register_tx")));
        vo.setCloudSummary(mapValue(source.get("cloud_summary")));
        vo.setFusionSummary(mapValue(source.get("fusion_summary")));
        vo.setStorage(mapValue(source.get("storage")));
        return vo;
    }

    public TelemetrySubmitVO toTelemetrySubmitVO(Map<String, Object> source) {
        TelemetrySubmitVO vo = new TelemetrySubmitVO();
        vo.setAccepted(booleanValue(source.get("accepted")));
        vo.setDeviceId(stringValue(source.get("device_id")));
        vo.setTerminalId(stringValue(source.get("terminal_id")));
        vo.setBusinessTx(mapValue(source.get("business_tx")));
        vo.setCloudSummary(mapValue(source.get("cloud_summary")));
        vo.setFusionSummary(mapValue(source.get("fusion_summary")));
        vo.setStorage(mapValue(source.get("storage")));
        return vo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listValue(Object value) {
        return value instanceof List<?> ? (List<Map<String, Object>>) value : null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Object value) {
        return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
    }

    private Boolean booleanValue(Object value) {
        return value instanceof Boolean ? (Boolean) value : null;
    }
}
