package contain.opensource.ils.bs.receiver.classes.Binding;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import contain.opensource.shared.constants.AlfrescoConstants;

/**
 * Utility class for loading PKCS12 private keys from a keystore file.
 * 
 * This class provides functionality to load a private key from a PKCS12 format
 * keystore,
 * with support for configurable keystore paths through environment variables or
 * default fallback.
 * 
 * p><b>Usage:</b>
 * pre>
 * PKCS12KeyLoader.loadPrivateKey();
 * PrivateKey key = PKCS12KeyLoader.PK;
 * 
 * 
 * p><b>Configuration:</b>
 * The keystore path is resolved in the following order:
 * ol>
 * Environment variable {@code APP_KEYSTORE_PATH}
 * Default path from {@code AlfrescoConstants.p12PrivateKeyFile}
 * 
 * 
 * p><b>Keystore Credentials:</b>
 * The following constants from {@code AlfrescoConstants} are used:
 *
 * {@code p12PrivateKeyFilePassword} - Password for the keystore
 * {@code p12PrivateKeyFileAlias} - Alias of the private key entry
 * {@code p12PrivateKeyFile} - Default keystore file path
 * 
 * 
 * @author [Your Name]
 * @since 1.0
 */
public final class PKCS12KeyLoader {

    public static PrivateKey PK;

    // public static PrivateKey loadPrivateKey(
    public static void loadPrivateKey() throws Exception {
        String keyStorePath = resolveKeystorePath();
        System.out.println("[PKCS12] Loading keystore from: " + keyStorePath);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            keyStore.load(
                    fis,
                    AlfrescoConstants.p12PrivateKeyFilePassword.toCharArray());
        }
        Key key = keyStore.getKey(AlfrescoConstants.p12PrivateKeyFileAlias,
                AlfrescoConstants.p12PrivateKeyFilePassword.toCharArray());
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