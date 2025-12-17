package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.ils.bs.receiver.classes.Notification;
import contain.opensource.ils.bs.receiver.classes.NotificationRoot;

import jakarta.servlet.http.HttpServletRequest;
import contain.opensource.ils.bs.receiver.services.GraphService;

@RestController
@RequestMapping("/api") // class-level base path
public class SPWebhookController {
    @PostMapping(value = "/WebHookListener", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            "application/*+json"
    })
    public ResponseEntity<?> webHookListener(
            @RequestParam(value = "validationtoken", required = false) String validationToken,
            @RequestBody(required = false) NotificationRoot notificationRoot,
            HttpServletRequest request) {

        // Validation handshake

        System.out.println("IN JAVA SHAREPOINT WEBHOOKLISTENER : ");
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
            GraphService GService = new GraphService();
            Notification notification = notificationRoot.getValue().get(0);
            GService.ProcessChangedSharepointItems(notification);
        }
        System.out.println("EXIT LISTENER: ");
        // Always return quickly
        return ResponseEntity.ok().build();
    }
}
