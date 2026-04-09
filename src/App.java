import com.dagdockersim.demo.LedgerSelfCheck;
import com.dagdockersim.model.LifecycleAction;
import com.dagdockersim.model.Transaction;
import com.dagdockersim.service.CloudStation;
import com.dagdockersim.service.DeviceSimulator;
import com.dagdockersim.service.FusionTerminal;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        if (args.length > 0 && "--self-check".equals(args[0])) {
            LedgerSelfCheck.main(Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        CloudStation cloud = new CloudStation(30, 5, true);
        FusionTerminal fusion1 = new FusionTerminal("fusion1", cloud);
        FusionTerminal fusion2 = new FusionTerminal("fusion2", cloud);
        FusionTerminal fusion3 = new FusionTerminal("fusion3", cloud);

        cloud.attachFusion(fusion1);
        cloud.attachFusion(fusion2);
        cloud.attachFusion(fusion3);

        DeviceSimulator device = DeviceSimulator.newDynamic("device41");
        Transaction registerTx = device.registerAt(fusion1);
        cloud.getLedger().confirmRegister(registerTx.getTxId());
        LifecycleAction confirmRegister = new LifecycleAction("confirm_register", registerTx.getTxId());
        fusion1.applyConfirmation(confirmRegister);
        fusion2.applyConfirmation(confirmRegister);
        fusion3.applyConfirmation(confirmRegister);

        Map<String, Object> telemetry = new LinkedHashMap<String, Object>();
        telemetry.put("sequence", 1);
        telemetry.put("device_name", device.getDeviceName());
        telemetry.put("captured_at", System.currentTimeMillis() / 1000.0);
        telemetry.put("metrics", metrics());

        Transaction businessTx = device.submitTelemetry(fusion1, telemetry);

        System.out.println("Java DAG simulation demo");
        System.out.println("registered device id: " + device.getDeviceId());
        System.out.println("register tx id: " + registerTx.getTxId());
        System.out.println("business tx id: " + businessTx.getTxId());
        System.out.println();
        System.out.println("cloud summary: " + cloud.getLedger().summary());
        System.out.println("fusion1 summary: " + fusion1.getLedger().summary());
        System.out.println("fusion2 summary: " + fusion2.getLedger().summary());
        System.out.println("fusion3 summary: " + fusion3.getLedger().summary());
        System.out.println();
        System.out.println("Run `java -cp bin App --self-check` for a behavior self-check suite.");
    }

    private static Map<String, Object> metrics() {
        Map<String, Object> metrics = new LinkedHashMap<String, Object>();
        metrics.put("voltage_v", 228.4);
        metrics.put("current_a", 16.2);
        metrics.put("temperature_c", 33.8);
        metrics.put("active_power_kw", 3.701);
        return metrics;
    }
}
