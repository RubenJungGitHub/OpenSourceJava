package contain.opensource.ils.bs.receiver.controllers;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api") // class-level base path
public class UUIDController {
    public UUIDController() {
   }

    @GetMapping(value = "/GetUUID")
    public String GetUUID() {
        // Generate a random UUID
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}