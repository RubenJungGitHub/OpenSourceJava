package contain.opensource.uuidutil.controllers;


import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.shared.constants.AlfrescoConstants;



@RestController
@RequestMapping("/api") // class-level base path
public class UUIDController {
    public UUIDController() {
    }

    // Possibly extend with requesting environment and add as prefix.
    @GetMapping(value = "/GetUUID")
    public String GetUUID(@RequestParam(required = false) String prefix) {
        // Generate a random UUID
        UUID uuid = UUID.randomUUID();
        String returnuuid = uuid.toString();
        if(prefix != null)
             returnuuid = prefix + "-" + returnuuid;


        System.out.println(
                AlfrescoConstants.CYAN + "GUID RETURNED : "
                        + returnuuid + AlfrescoConstants.RESET);
        return returnuuid;
    }
}