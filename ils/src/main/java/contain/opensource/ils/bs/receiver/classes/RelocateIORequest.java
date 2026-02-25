package contain.opensource.ils.bs.receiver.classes;

import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.ContainPlatforms;

public class RelocateIORequest {
    private String ioId;
    private ContainPlatforms source;
    private ContainPlatforms destination;
    // getters & setters

    public String getIOID() {
        return ioId;
    }

    public void setIOID(String IOID) {
        this.ioId = IOID;
    }

    public ContainPlatforms getSource() {
        return source;
    }

    public void setSource(ContainPlatforms source) {
        this.source = source;
    }

    public ContainPlatforms getDestination() {
        return destination;
    }

    public void setDestination(ContainPlatforms destination) {
        this.destination = destination;
    }
}