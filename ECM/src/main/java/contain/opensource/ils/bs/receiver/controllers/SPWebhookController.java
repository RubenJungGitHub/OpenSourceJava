package contain.opensource.ils.bs.receiver.controllers;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.ils.bs.receiver.classes.Notification;
import contain.opensource.ils.bs.receiver.classes.NotificationRoot;
import contain.opensource.ils.bs.receiver.services.GraphService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api") // class-level base path
public class SPWebhookController {

    private Boolean SPWebHookActive = false;
    private int WaitCounter = 0;
    private int MaxWaitCounter = 10;
    private final GraphService graphService;

    @Autowired
    public SPWebhookController(GraphService graphService) {
        this.graphService = graphService;
    }

    @PostMapping(value = "/WebHookListenerREDUNDANT", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            "application/*+json"
    })
    public ResponseEntity<?> webHookListener(
            @RequestParam(value = "validationtoken", required = false) String validationToken,
            @RequestBody(required = false) NotificationRoot notificationRoot,
            HttpServletRequest request) {

        System.out.println(LocalDate.now() + ": IN JAVA SHAREPOINT WEBHOOKLISTENER : ");
        // Check if not running then wait.
        //thicky semaphore. Future solution is a-la Alfresco. Read  ActveMQ
     /*
        while (SPWebHookActive) {
           System.out.println(LocalDate.now() + ": Waiting for running listener to complete : #" + WaitCounter + " of " + MaxWaitCounter);
            try
            {
                WaitCounter++;
                Thread.sleep(10000);
                if (WaitCounter >= MaxWaitCounter) 
                    {
                    SPWebHookActive = false;
                    }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        SPWebHookActive = true;
*/

        if (validationToken != null && !validationToken.isEmpty()) {
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(validationToken);
        }

        // Log ListItem ID if present
        if (notificationRoot != null
                && notificationRoot.getValue() != null
                && !notificationRoot.getValue().isEmpty()
                && notificationRoot.getValue().get(0).getResourceData() != null) {
            // GraphService GService = new GraphService();
            Notification notification = notificationRoot.getValue().get(0);
            this.graphService.ProcessChangedSharepointItems(notification);
          //  SPWebHookActive = false;
        }
        System.out.println("EXIT LISTENER: ");
        return ResponseEntity.ok().build();
    }
}
