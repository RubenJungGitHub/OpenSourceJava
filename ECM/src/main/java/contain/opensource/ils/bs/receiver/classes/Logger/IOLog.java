package contain.opensource.ils.bs.receiver.classes.Logger;

import java.util.Optional;

import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.receiver.constants.AlfrescoConstants.eActionPerformed;
import contain.opensource.ils.bs.receiver.services.IOLogBallenbakService;

@Component
public class IOLog {

    private static IOLogBallenbakService delegate;

    public IOLog(IOLogBallenbakService service) {
        IOLog.delegate = service;
    }

    public static Optional<IOLogBallenbak> GetLog(String uuid) 
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
            String ActionPerformedBy) {

        if (delegate == null) {
            throw new IllegalStateException("IOLog not initialized yet");
        }
        try {
            delegate.log(
                    containIOUUID,
                    PlatformID,
                    IOpath,
                    action,
                    source,
                    destination,
                    pkiHash,
                    reference,
                    additionalInfo,
                    actionPerformed,
                    ActionPerformedBy);
        } catch (Exception ex) {
            System.out.println("Failed to log IO action: " + ex.getMessage());
        }
    }
}