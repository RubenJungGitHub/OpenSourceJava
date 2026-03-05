package contain.opensource.ils.bs.receiver.classes.Binding;

import contain.opensource.ils.bs.receiver.classes.Binding.SecuredDocument;

/**
 * BindRequest class represents a request object for binding and signing documents.
 * 
 * This class encapsulates the data required to perform document signing operations,
 * including the document to be secured and the private key credentials needed for
 * the signing process.
 * 
  * Usage example:
 
 * SecuredDocument document = new SecuredDocument();
 * String privateKey = "base64EncodedPrivateKey";
 * BindRequest request = new BindRequest(document, privateKey);
 
 * 
 * @author Your Name
 * @version 1.0
 */
public class BindRequest {

    private SecuredDocument secureDocument;

    public BindRequest() {}

    public BindRequest(SecuredDocument secureDocument) {
        this.secureDocument = secureDocument;
    }

    public SecuredDocument getSecureDocument() {
        return secureDocument;
    }

    public void setSecureDocument(SecuredDocument secureDocument) {
        this.secureDocument = secureDocument;
    }
}
