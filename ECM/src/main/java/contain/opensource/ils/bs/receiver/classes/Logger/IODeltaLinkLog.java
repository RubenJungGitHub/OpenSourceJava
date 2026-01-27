package contain.opensource.ils.bs.receiver.classes.Logger;

import java.util.Optional;

import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.receiver.services.IODeltalinkService;

@Component
public class IODeltaLinkLog {

    private static IODeltalinkService delegate;

    public IODeltaLinkLog(IODeltalinkService service) {
        IODeltaLinkLog.delegate = service;
    }

    public static Optional<IOLogDeltaLink> GetLog(String SourceID) 
    {
        if (delegate == null) {
            throw new IllegalStateException("IOLogDeltalink not initialized yet");
        }
        try {
            return delegate.GetLog(SourceID);
        } catch (Exception ex) {
            System.out.println("Failed to log Deltalink : " + ex.getMessage());
            return Optional.empty();
        }
    }

    public static void log(
            String SourceID,
            String TokenID
            ) {
        if (delegate == null) {
            throw new IllegalStateException("IODeltaLinkLog not initialized yet");
        }
        try {
            delegate.log(
                    SourceID,
                    TokenID
);
                    
        } catch (Exception ex) {
            System.out.println("Failed to log new deltalink : " + ex.getMessage());
        }
    }
}