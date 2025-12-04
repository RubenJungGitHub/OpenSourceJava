package contain.opensource.java.helloworld.services;

import com.microsoft.aad.msal4j.*;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class GraphTokenService {

    // Use your own tenant, clientId, clientSecret
    private final String tenantId = "9a1b5f77-1f1a-40ac-b1a1-38617300f02a";
    private final String clientId = "f590b477-5bd7-47d6-8bda-36f77fa10afd";
    private final String clientSecret = "pE.8Q~ZQRGngJ1YliTP4EDC5bejaEl72LlBAzb50";

public String getClientToken() {
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                    clientId,
                    ClientCredentialFactory.createFromSecret(clientSecret))
                .authority("https://login.microsoftonline.com/" + tenantId)
                .build();

            // Request token for Microsoft Graph
            ClientCredentialParameters parameters = ClientCredentialParameters.builder(
                    Collections.singleton("https://graph.microsoft.com/.default"))
                    .build();

            CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);
            IAuthenticationResult result = future.get();

            return result.accessToken();

        } catch (MalformedURLException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
