package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.web.bind.annotation.*;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import contain.opensource.ils.bs.receiver.services.GeminiService;
import contain.opensource.ils.bs.receiver.classes.Classification.ClassificationResult;
import contain.opensource.ils.bs.receiver.model.TaxonomyResponse;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import contain.opensource.ils.bs.receiver.services.TaxonomyServiceClient;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
@RestController
@RequestMapping("/api/v1")
public class GeminiClassificationController {

    private final Tika tika = new Tika();
    private final GeminiService geminiReceiverService;
    private final TaxonomyServiceClient taxonomyServiceClient;

    @Autowired
    public GeminiClassificationController(GeminiService geminiReceiverService,
            TaxonomyServiceClient taxonomyServiceClient) {
        this.geminiReceiverService = geminiReceiverService;
        this.taxonomyServiceClient = taxonomyServiceClient;
    }

    @PostMapping(value = "/classifygeminiservice", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ClassificationResult> classifyWithGemini(@RequestBody byte[] content) throws Exception {
        try {
            if (content == null || content.length == 0) {
                return ResponseEntity.badRequest().build();
            }

            String extractedText = tika.parseToString(new ByteArrayInputStream(content)).trim();

            if (extractedText.isBlank()) {
                return ResponseEntity.ok(new ClassificationResult("Gemini", "Unstructured non text : unable to classify", 0.0, "EMPTY_CONTENT","<Unknown>"));
            }

            if (extractedText.length() > 4000) {
                extractedText = extractedText.substring(0, 4000);
            }

            // 1. Haal de taxonomie op via het directe veld 'options'
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

            // 3. Roep Gemini aan
            String suggestedLabel = geminiReceiverService.evaluate(prompt);
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

            if (suggestedLabel != null) {
                suggestedLabel = suggestedLabel.replaceAll("[^a-zA-Z]", "").trim();
            } else {
                suggestedLabel = "No label determined";
            }

            return ResponseEntity.ok(new ClassificationResult("Gemini", suggestedLabel, 0.95, "SUCCESS","ToDo"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new ClassificationResult("Gemini", "Unclassified", 0.0, "ERROR: " + e.getMessage(),"<Unknown>"));
        }
    }
}