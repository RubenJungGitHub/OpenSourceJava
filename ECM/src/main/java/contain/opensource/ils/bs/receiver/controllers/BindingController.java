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
public class BindingController {
    public BindingController(GraphService graphService) {
   }

    @PostMapping(value = "/Bind", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            "application/*+json"
    })
    public ResponseEntity<?> Bind(
            @RequestParam(value = "validationtoken", required = false) String validationToken,
            @RequestBody(required = false) NotificationRoot notificationRoot,
            HttpServletRequest request) {

           // Always return quickly
           return ResponseEntity.ok().build();
    }
}
