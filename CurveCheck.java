import java.security.*;
import java.security.spec.*;
public class CurveCheck {
  public static void main(String[] args) throws Exception {
    try {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
      kpg.initialize(new ECGenParameterSpec("secp256k1"));
      KeyPair kp = kpg.generateKeyPair();
      System.out.println("secp256k1 supported");
    } catch (Exception e) {
      System.out.println("secp256k1 unsupported: " + e.getClass().getName() + ": " + e.getMessage());
    }
  }
}
