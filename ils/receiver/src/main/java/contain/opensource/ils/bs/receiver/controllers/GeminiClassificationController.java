package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.web.bind.annotation.*;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import contain.opensource.ils.bs.receiver.services.GeminiService;
import contain.opensource.ils.bs.receiver.classes.Classification.ClassificationResult;
import java.io.ByteArrayInputStream;
import org.springframework.http.MediaType;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1")
public class GeminiClassificationController {

    private final Tika tika = new Tika();
    private final GeminiService geminiReceiverService;

    public GeminiClassificationController(GeminiService geminiReceiverService) {
        this.geminiReceiverService = geminiReceiverService;
    }

    @PostMapping(value = "/classifygeminiservice", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ClassificationResult> classifyWithGemini(@RequestBody byte[] content) throws Exception {
        if (content == null || content.length == 0) {
            return ResponseEntity.badRequest().build();
        }

        String extractedText = tika.parseToString(new ByteArrayInputStream(content)).trim();
        // Eventuele Base64 check hier...
        
        if (extractedText.isBlank()) {
            return ResponseEntity.ok(new ClassificationResult("Gemini","Unclassified", 0.0, "EMPTY_CONTENT"));
        }

        if (extractedText.length() > 4000) {
            extractedText = extractedText.substring(0, 4000);
        }

        String prompt = """
            Classificeer de onderstaande documenttekst.
            Kies uitsluitend EÉN van de volgende labels:
            - Confidential
            - Internal
            - Public
            - Secret
            - Unclassified
            Geef alleen het exacte label terug als antwoord.
            
            Tekst:
            """ + extractedText;

        // Roep direct de Gemini service aan
        String suggestedLabel = geminiReceiverService.evaluate(prompt);
        
        if (suggestedLabel != null) {
            suggestedLabel = suggestedLabel.replaceAll("[^a-zA-Z]", "").trim();
        } else {
            suggestedLabel = "Unclassified";
        }

        return ResponseEntity.ok(new ClassificationResult("Gemini", suggestedLabel, 0.95, "SUCCESS"));
    }
}