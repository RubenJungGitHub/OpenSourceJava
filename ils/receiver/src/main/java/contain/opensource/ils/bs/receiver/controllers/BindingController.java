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
import  contain.opensource.ils.bs.receiver.classes.Binding.RJBindAndSecureIO;

@RestController
@RequestMapping("/api") // class-level base path
public class BindingController {
        public BindingController() {
        }

        @PostMapping(value = "/Bind", consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> bind(@RequestBody BindRequest request) {
                try {
                        String keyBase64 = request.getPrivateKeyPem();
                        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
                        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        PrivateKey privateKey = kf.generatePrivate(spec);
                        byte[] HASH = RJBindAndSecureIO.sign(request.secureDocument, privateKey);
                        StringBuilder hsb = new StringBuilder();
                        for (byte b : HASH) {
                                hsb.append(String.format("%02x", b));
                        }
                        String hashstring = hsb.toString();
                        return ResponseEntity.ok(hashstring);
                } catch (Exception ex) {
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Failed: " + ex.getMessage());
                }
        }
}
