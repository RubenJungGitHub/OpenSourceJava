package contain.opensource.ils.bs.receiver.classes.Logger;

import java.util.Optional;

import org.springframework.stereotype.Component;

import contain.opensource.shared.constants.AlfrescoConstants.eActionPerformed;
import contain.opensource.ils.bs.receiver.services.IOLogBallenbakServiceMongo;

@Component
public class IOLog {

    private static IOLogBallenbakServiceMongo delegate;

    public IOLog(IOLogBallenbakServiceMongo service) {
        IOLog.delegate = service;
    }

    public static Optional<IOLogBallenbakMongo> GetLog(String uuid) 
    {
        if (delegate == null) {
            throw new IllegalStateException("IOLog not initialized yet");
        }
        try {
            return delegate.GetLog(uuid);
        } catch (Exception ex) {
            System.out.println("Failed to log IO action: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public static void log(
            String containIOUUID,
            String PlatformID,
            String IOpath,
            String action,
            String source,
            String destination,
            String pkiHash,
            String reference,
            String additionalInfo,
            eActionPerformed actionPerformed,
            String ActionPerformedBy,
            String Marking,
            String MarkingID,
            String Classification,
            String ClassificationID,
            String version) {
        if (delegate == null) {
            throw new IllegalStateException("IOLog not initialized yet");
        }
        try {
            delegate.log(
                    containIOUUID.replaceAll("^\"|\"$", ""),
                    PlatformID,
                    IOpath,
                    action,
                    source,
                    destination,
                    pkiHash,
                    reference,
                    additionalInfo,
                    actionPerformed,
                    ActionPerformedBy,
                    Marking,
                    MarkingID,
                    ClassificationID,
                    Classification,
                    version);
                    
        } catch (Exception ex) {
            System.out.println("Failed to log IO action: " + ex.getMessage());
        }
    }
}