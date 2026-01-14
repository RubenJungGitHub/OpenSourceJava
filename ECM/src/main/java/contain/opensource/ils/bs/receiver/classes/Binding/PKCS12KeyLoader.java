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

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (FileInputStream fis = new FileInputStream(AlfrescoConstants.p12PrivateKeyFile)) {
            keyStore.load(fis, AlfrescoConstants.p12PrivateKeyFilePassword.toCharArray());
        }
        Key key =  keyStore.getKey(AlfrescoConstants.p12PrivateKeyFileAlias, AlfrescoConstants.p12PrivateKeyFilePassword.toCharArray());
        PK = (PrivateKey) key;
    }
}