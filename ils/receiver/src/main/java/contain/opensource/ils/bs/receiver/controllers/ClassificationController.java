package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.web.bind.annotation.*;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import contain.opensource.ils.bs.receiver.services.LocalLlamaService;
import contain.opensource.ils.bs.receiver.classes.Classification.ClassificationResult;
import java.io.ByteArrayInputStream;
import org.springframework.http.MediaType;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1")
public class ClassificationController {

    private final Tika tika = new Tika();
    private final LocalLlamaService llamaService;

    public ClassificationController(LocalLlamaService llamaService) {
        this.llamaService = llamaService;
    }

    @PostMapping(value = "/classify", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ClassificationResult> classifyByteArray(
            @RequestBody byte[] content) throws Exception {

       if (content == null || content.length == 0) {
            return ResponseEntity.badRequest().build();
        }


        // 1. Tika verwerkt de byte array rechtstreeks uit het geheugen
        String extractedText = tika.parseToString(new ByteArrayInputStream(content)).trim();

        // 2. Base64 afhandeling met try-catch en controle op lege inhoud
        if (isBase64(extractedText)) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(extractedText.replaceAll("\\s+", ""));
                String decodedText = tika.parseToString(new ByteArrayInputStream(decodedBytes)).trim();
                
                // Alleen overschrijven als er daadwerkelijk tekst is gedecodeerd
                if (!decodedText.isEmpty()) {
                    extractedText = decodedText;
                }
            } catch (IllegalArgumentException e) {
                // Was geen geldige Base64-payload, behoud de originele extractedText
            }
        }

        // 3. Controleer of er daadwerkelijk bruikbare tekst is overgebleven
            System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BRIGHT_CYAN
                    + ("Extracted text to be classified : " + extractedText)
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);

        System.out.println("Extracted text to be classified : " + extractedText) ;
        if (extractedText.isBlank()) {
            return ResponseEntity.ok(new ClassificationResult("Unclassified", 0.0, "EMPTY_CONTENT"));
        }

        // 4. Truncation: voorkom dat hele lange documenten het LLM context window laten vollopen
        if (extractedText.length() > 4000) {
            extractedText = extractedText.substring(0, 4000);
        }

        // 5. Bouw prompt met expliciete labels en veilige concatenatie/ Labels moeten komen uit VocBench!!!!! of andere MMS
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

        // 6. Query LLM
        String suggestedLabel = llamaService.evaluate(prompt);
        ClassificationResult result = new ClassificationResult(suggestedLabel, 0.95, "SUCCESS");

        return ResponseEntity.ok(result);
    }

    private boolean isBase64(String str) {
        if (str == null || str.isEmpty())
            return false;
        String cleanStr = str.replaceAll("\\s+", "");
        if (cleanStr.length() % 4 != 0)
            return false;
        return cleanStr.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
    }
}