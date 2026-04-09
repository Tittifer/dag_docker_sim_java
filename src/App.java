import com.dagdockersim.demo.LedgerSelfCheck;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication(scanBasePackages = "com.dagdockersim")
public class App {
    public static void main(String[] args) {
        if (args.length > 0 && "--self-check".equals(args[0])) {
            LedgerSelfCheck.main(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        SpringApplication.run(App.class, args);
    }
}
