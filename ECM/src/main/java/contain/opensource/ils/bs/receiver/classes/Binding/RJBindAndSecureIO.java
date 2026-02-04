package contain.opensource.ils.bs.receiver.classes.Binding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;  
import java.security.Signature;
import java.util.Base64;
import java.util.List;


public final class RJBindAndSecureIO {


    public static byte[] sha256(byte[] data) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return digest.digest(data);
}

static byte[] canonicalize(SecuredDocument doc) throws Exception {
    StringBuilder sb = new StringBuilder();

    sb.append("objectId=").append(doc.getId()).append("\n");
    sb.append("filename=").append(doc.getFileName()).append("\n");
    sb.append("description=").append(doc.getDescription()).append("\n");
    sb.append("mimeType=").append(doc.getMimeType()).append("\n");
    sb.append("created=").append(doc.getCreated()).append("\n");
    sb.append("lastModified=").append(doc.getLastModified()).append("\n");
    sb.append("marking=").append(doc.getMarking()).append("\n");
    sb.append("label=").append(doc.getLabel()).append("\n");
    sb.append("version=").append(doc.getVersion()).append("\n");

   // canonicalizeList("classification", doc.getClassifications(), sb);
   // canonicalizeList("label", doc.getLabels(), sb);
   // canonicalizeList("marking", doc.getMarkings(), sb);

    byte[] contentHash = sha256(doc.getContent());  // now allowed
    sb.append("contentHash=")
      .append(Base64.getEncoder().encodeToString(contentHash));

    return sb.toString().getBytes(StandardCharsets.UTF_8);
}
/*

    static void canonicalizeList(
            String name,
            List<String> values,
            StringBuilder sb) {
        values.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .forEach(v -> sb.append(name).append("=").append(v).append("\n"));
    }

    */
    public static byte[] sign(SecuredDocument doc, PrivateKey key) throws Exception {

        byte[] canonical = canonicalize(doc);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(hash);
        return sig.sign();
    }

    
    
    static boolean verify(
            SecuredDocument doc,
            byte[] signature,
            PublicKey publicKey) throws Exception {

        byte[] canonical = canonicalize(doc);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(hash);
        return sig.verify(signature);
    }
}
