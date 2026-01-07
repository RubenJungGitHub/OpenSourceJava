package contain.opensource.ils.bs.receiver.classes.Logger;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.receiver.services.IOLogBallenbakService;

@Component
public class IOLog {

    private static IOLogBallenbakService delegate;

    public IOLog(IOLogBallenbakService service) {
        IOLog.delegate = service;
    }

    public static void log(
            String containIOUUID,
            String ioAction,
            String ioSource,
            String ioDestination,
            String pkiHash,
            String ioReference,
            String additionalInfo) {

        if (delegate == null) {
            throw new IllegalStateException("IOLog not initialized yet");
        }

        delegate.log(
            containIOUUID,
            ioAction,
            ioSource,
            ioDestination,
            pkiHash,
            ioReference,
            additionalInfo
        );
    }
}