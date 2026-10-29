package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.web.bind.annotation.*;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import contain.opensource.ils.bs.receiver.services.ChatGPTService;
import contain.opensource.ils.bs.receiver.classes.Classification.ClassificationResult;
import contain.opensource.ils.bs.receiver.model.TaxonomyResponse;

import java.io.ByteArrayInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.Base64;
import java.util.stream.Collectors;

import contain.opensource.ils.bs.receiver.services.TaxonomyServiceClient;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ChatGPTClassificationController {

    private final Tika tika = new Tika();
    private final ChatGPTService chatgptservice;
    private final TaxonomyServiceClient taxonomyServiceClient;

    @Autowired
    public ChatGPTClassificationController(ChatGPTService chatGPTservice,
            TaxonomyServiceClient taxonomyServiceClient) {
        this.chatgptservice = chatGPTservice;
        this.taxonomyServiceClient = taxonomyServiceClient;
    }

    @PostMapping(value = "/chatgptService", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
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

        System.out.println("Extracted text to be classified : " + extractedText);
        if (extractedText.isBlank()) {
            return ResponseEntity.ok(new ClassificationResult("ChatGPT", "Unstructured non text : unable to classify", 0.0, "EMPTY_CONTENT","<Unknown>"));
        }

        // 4. Truncation: voorkom dat hele lange documenten het LLM context window laten
        // vollopen
        if (extractedText.length() > 4000) {
            extractedText = extractedText.substring(0, 4000);
        }

        // Bouw propmt
        TaxonomyResponse taxonomy = taxonomyServiceClient.GetTaxonomies("classification", "en");

        String labelsList = "";
        StringBuilder sb = new StringBuilder();
        if (taxonomy != null && taxonomy.options != null) {
            for (var option : taxonomy.options) {
                sb.append("- ").append(option.label).append("\n");
            }
        }

        labelsList = Arrays.stream(sb.toString().split("\n"))
                .map(line -> line.trim())
                .filter(line -> !line.toLowerCase().contains("unclassified"))
                .collect(Collectors.joining("\n"));

        // 2. Bouw de prompt dynamisch op met .formatted()
        String prompt = """
                Classificeer de onderstaande documenttekst.
                Kies uitsluitend EÉN van de volgende labels:
                %s
                Geef alleen het exacte label terug als antwoord.

                Tekst:
                %s
                """.formatted(labelsList, extractedText);

        // 6. Query LLM
        String suggestedLabel = chatgptservice.evaluate(prompt);
        List<String> validLabels = Arrays.asList(labelsList.split("\n"));
        boolean isValid = false;

        for (String validLabel : validLabels) {
            String cleanValidLabel = validLabel.replaceFirst("^-\\s*", "").trim();
            if (cleanValidLabel.equalsIgnoreCase(suggestedLabel.trim())) {
                isValid = true;
                break;
            }
        }
        if (!isValid) {
            suggestedLabel = "No valid label detected : unable to classify";
        }
        ClassificationResult result = new ClassificationResult("chatGPTClassifier", suggestedLabel, 0.95, "SUCCESS","<Unknown>");
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