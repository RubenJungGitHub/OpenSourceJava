package contain.opensource.ils.bs.receiver.classes.Binding;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import contain.opensource.shared.constants.AlfrescoConstants;
import contain.opensource.shared.configurationproperties.AlfrescoProperties;
import  contain.opensource.shared.configurationproperties.ILSRestProperties;
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

@Component
public class PKCS12KeyLoader {
    private String keyStorePath;
    private PrivateKey cachedKey;

    @Autowired
  public PKCS12KeyLoader(ILSRestProperties ilsproperties) {
        this.keyStorePath = ilsproperties.getkeystorepath();
  }

    //Hard coded. this should be derived from propertiess
    public PKCS12KeyLoader() {
    }

    public synchronized PrivateKey getPrivateKey() throws Exception {
        if (cachedKey == null) {
            cachedKey = loadPrivateKeyInternal();
            System.out.println("[PKCS12] Private key loaded and cached");
        }
        return cachedKey;
    }

    // <<< Add this getter
    public String getKeyStorePath() {
        return keyStorePath;
    }

    private PrivateKey loadPrivateKeyInternal() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            keyStore.load(fis, AlfrescoConstants.p12PrivateKeyFilePassword.toCharArray());
        }
        Key key = keyStore.getKey(
                AlfrescoConstants.p12PrivateKeyFileAlias,
                AlfrescoConstants.p12PrivateKeyFilePassword.toCharArray());
        return (PrivateKey) key;
    }
}