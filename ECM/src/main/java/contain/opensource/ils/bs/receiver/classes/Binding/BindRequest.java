package contain.opensource.ils.bs.receiver.classes.Binding;

import contain.opensource.ils.bs.receiver.classes.Binding.SecuredDocument;

public class BindRequest {

    public SecuredDocument secureDocument; // the document to sign
    public String privateKeyPem; // send private key as Base64 string

    // Default constructor (required by Jackson)
    public BindRequest() {}

    // Convenience constructor
    public BindRequest(SecuredDocument secureDocument, String privateKeyPem) {
        this.secureDocument = secureDocument;
        this.privateKeyPem = privateKeyPem;
    }

    // Optional: getters and setters
    public SecuredDocument getSecureDocument() { return secureDocument; }
    public void setSecureDocument(SecuredDocument secureDocument) { this.secureDocument = secureDocument; }

    public String getPrivateKeyPem() { return privateKeyPem; }
    public void setPrivateKeyPem(String privateKeyPem) { this.privateKeyPem = privateKeyPem; }
}
