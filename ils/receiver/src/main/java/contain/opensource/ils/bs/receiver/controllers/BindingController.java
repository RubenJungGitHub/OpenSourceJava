package contain.opensource.ils.bs.receiver.controllers;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import contain.opensource.ils.bs.receiver.classes.Binding.BindRequest;
import contain.opensource.ils.bs.receiver.classes.Binding.PKCS12KeyLoader;
import  contain.opensource.ils.bs.receiver.classes.Binding.RJBindAndSecureIO;

@RestController
@RequestMapping("/api")
public class BindingController {

    private final PKCS12KeyLoader keyLoader;

    public BindingController(PKCS12KeyLoader keyLoader) {
        this.keyLoader = keyLoader;
    }

    @PostMapping(value = "/Bind", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> bind(@RequestBody BindRequest request) {
        try {
            PrivateKey privateKey = keyLoader.getPrivateKey();

            byte[] HASH = RJBindAndSecureIO.sign(
                    request.getSecureDocument(),
                    privateKey);

            StringBuilder hsb = new StringBuilder();
            for (byte b : HASH) {
                hsb.append(String.format("%02x", b));
            }

            return ResponseEntity.ok(hsb.toString());

        } catch (Exception ex) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed: " + ex.getMessage());
        }
    }
}
