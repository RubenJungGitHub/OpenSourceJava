package contain.opensource.ils.bs.receiver.classes.Binding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * RJBindAndSecureIO provides cryptographic binding and secure I/O operations
 * for secured documents.
 * 
 * This utility class offers methods for computing SHA-256 hashes,
 * canonicalizing document metadata,
 * and performing digital signature operations (signing and verification) using
 * RSA with SHA-256.
 * 
 * 
 * b>Key Operations:</b>
 *
 *
 * {@link #sha256(byte[])} - Computes SHA-256 hash of binary data
 * {@link #canonicalize(SecuredDocument)} - Normalizes document metadata
 * into a canonical byte representation
 * {@link #sign(SecuredDocument, PrivateKey)} - Creates a digital signature
 * for a document using a private key
 * {@link #verify(SecuredDocument, byte[], PublicKey)} - Verifies a
 * document's digital signature using a public key
 *
 * 
 * 
 * b>Canonical Format:</b>
 *
 * The canonicalization process creates a consistent string representation
 * containing:
 * objectId, filename, title, description, mimeType, created, lastModified,
 * marking, label,
 * version, and a Base64-encoded SHA-256 hash of the document content.
 * 
 * 
 * b>Security Notes:</b>
 *
 *
 * Uses SHA-256 with RSA (SHA256withRSA) for digital signatures
 * Suitable for document binding and integrity verification
 * All operations may throw {@link Exception} for cryptographic
 * failures
 *
 * 
 * @since 1.0
 */
public final class RJBindAndSecureIO {
    public static byte[] sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    /**
     * Canonicalizes a SecuredDocument into a standardized byte array
     * representation.
     * 
     * This method creates a canonical form of the document by extracting its
     * metadata
     * and content, formatting them in a consistent manner, and encoding the result
     * as UTF-8 bytes.
     * The canonical form includes the document's ID, filename, title, description,
     * MIME type,
     * creation date, last modification date, marking, label, version, and a SHA-256
     * hash of
     * the document content encoded in Base64.
     * 
     * @param doc the {@link SecuredDocument} to canonicalize
     * @return a byte array containing the UTF-8 encoded canonical representation of
     *         the document
     * @throws Exception if an error occurs during SHA-256 hashing or processing
     */
    static byte[] canonicalize(SecuredDocument doc) throws Exception {
        // Parameters for binding and signing
        StringBuilder sb = new StringBuilder();
        sb.append("objectId=").append(doc.getId()).append("\n");
        sb.append("filename=").append(doc.getFileName()).append("\n");
        sb.append("title=").append(doc.getTitle()).append("\n");
        sb.append("description=").append(doc.getDescription()).append("\n");
        sb.append("mimeType=").append(doc.getMimeType()).append("\n");
        sb.append("created=").append(doc.getCreated()).append("\n");
        sb.append("lastModified=").append(doc.getLastModified()).append("\n");
        sb.append("marking=").append(doc.getMarking()).append("\n");
        sb.append("label=").append(doc.getLabel()).append("\n");
        sb.append("version=").append(doc.getVersion()).append("\n");
        byte[] contentHash = sha256(doc.getContent()); // now allowed
        sb.append("contentHash=")
                .append(Base64.getEncoder().encodeToString(contentHash));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Signs a secured document using the provided private key.
     * 
     * @param doc the SecuredDocument to be signed
     * @param key the PrivateKey used to sign the document
     * @return a byte array containing the digital signature
     * @throws Exception if an error occurs during the signing process,
     *                   including issues with message digest or signature algorithm
     *                   initialization
     */
    public static byte[] sign(SecuredDocument doc, PrivateKey key) throws Exception {
        byte[] canonical = canonicalize(doc);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(hash);
        return sig.sign();
    }

    /**
     * Verifies the digital signature of a secured document using RSA-SHA256.
     *
     * @param doc       the secured document to verify
     * @param signature the digital signature bytes to verify against
     * @param publicKey the public key used to verify the signature
     * @return true if the signature is valid for the given document, false
     *         otherwise
     * @throws Exception if an error occurs during signature verification, message
     *                   digest initialization,
     *                   or if the algorithm is not available
     */
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
