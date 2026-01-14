
package contain.opensource.ils.bs.receiver.classes.Binding;
import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants;


import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class TestCreateSelfGeneratedCertificate {

    public static void Generate() throws Exception {
        // ✅ Register Bouncy Castle provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // 1. Generate Key Pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // 2. Build X.509 certificate
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        //Until retirement date ;-) 
        Date endDate = new Date(now + 1689L * 24 * 60 * 60 * 1000); // 1 year validity

        X500Name dnName = new X500Name("CN=Test Certificate");
        BigInteger certSerialNumber = BigInteger.valueOf(new SecureRandom().nextInt() & 0x7fffffff);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dnName, certSerialNumber, startDate, endDate, dnName, publicKey);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC") // Provider must be set here too
                .build(privateKey);

        X509CertificateHolder certHolder = certBuilder.build(signer);

        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);

        // 3. Write certificate to disk in DER format
        try (FileOutputStream fos = new FileOutputStream("c:/temp/Containselfsigned_cert.cer")) {
            fos.write(certificate.getEncoded());
        }

        System.out.println("Certificate written to c:/temp/Containselfsigned_cert.cer");

                // 4. Save private key + certificate in PKCS#12 keystore
        KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
        pkcs12.load(null, null); // initialize

        char[] password = AlfrescoConstants.p12PrivateKeyFilePassword.toCharArray(); // choose a secure password
        String alias = AlfrescoConstants.p12PrivateKeyFileAlias;

        pkcs12.setKeyEntry(alias, privateKey, password, new X509Certificate[]{certificate});

        try (FileOutputStream fos = new FileOutputStream("c:/temp/Containselfsigned_cert.p12")) {
            pkcs12.store(fos, password);
        }

        System.out.println("Private key + certificate written to c:/temp/Containselfsigned_cert.p12");
    }
}
