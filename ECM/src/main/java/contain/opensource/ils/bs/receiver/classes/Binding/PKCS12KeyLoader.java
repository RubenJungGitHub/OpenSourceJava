package contain.opensource.ils.bs.receiver.classes.Binding;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;
public final class PKCS12KeyLoader {

    public static PrivateKey PK;
    //public static PrivateKey loadPrivateKey(
    public static void  loadPrivateKey() throws Exception {
        String keyStorePath = resolveKeystorePath();
        System.out.println("[PKCS12] Loading keystore from: " + keyStorePath);


        KeyStore keyStore = KeyStore.getInstance("PKCS12");

       try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            keyStore.load(
                fis,
                AlfrescoConstants.p12PrivateKeyFilePassword.toCharArray()
            );
        }
        Key key =  keyStore.getKey(AlfrescoConstants.p12PrivateKeyFileAlias, AlfrescoConstants.p12PrivateKeyFilePassword.toCharArray());
        PK = (PrivateKey) key;
    }

private static String resolveKeystorePath() {
    String env = System.getenv("APP_KEYSTORE_PATH");
    if (env != null && !env.isBlank()) {
        return env;
    }
    return AlfrescoConstants.p12PrivateKeyFile; // dev fallback
}

}