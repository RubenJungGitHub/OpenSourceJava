package contain.opensource.ils.bs.receiver.controllers;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
public class UUIDController {
    public UUIDController(GraphService graphService) {
   }

    @GetMapping(value = "/GetUUID")
    public String GetUUID() {
        // Generate a random UUID
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}