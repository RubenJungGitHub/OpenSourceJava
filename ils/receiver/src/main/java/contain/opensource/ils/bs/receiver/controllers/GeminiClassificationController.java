package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.web.bind.annotation.*;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import contain.opensource.ils.bs.receiver.services.GeminiCassifierService;
import contain.opensource.ils.bs.receiver.classes.Classification.ClassificationResult;
import contain.opensource.ils.bs.receiver.model.TaxonomyResponse;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
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
    private final GeminiCassifierService geminiReceiverService;
    private final TaxonomyServiceClient taxonomyServiceClient;

    @Autowired
    public GeminiClassificationController(GeminiCassifierService geminiReceiverService,
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

            // 1. Tika verwerkt de byte array rechtstreeks uit het geheugen
            String extractedText = tika.parseToString(new ByteArrayInputStream(content)).trim();

            if (extractedText.isBlank()) {
                return ResponseEntity.ok(new ClassificationResult("Gemini", "Unstructured non text : unable to classify", 0.0, "EMPTY_CONTENT", "UNKNOWN_GUID"));
            }

            // 2. Truncation: voorkom dat hele lange documenten het context window laten vollopen
            if (extractedText.length() > 4000) {
                extractedText = extractedText.substring(0, 4000);
            }

            // 3. Haal taxonomie op en bouw een mapping van Label -> Value (GUID)
            TaxonomyResponse taxonomy = taxonomyServiceClient.GetTaxonomies("classification", "en");

            String labelsList = "";
            StringBuilder sb = new StringBuilder();
            Map<String, String> labelToValueMap = new HashMap<>();

            if (taxonomy != null && taxonomy.options != null) {
                for (var option : taxonomy.options) {
                    String cleanLabel = option.label != null ? option.label.replaceFirst("^-\\s*", "").trim() : "";
                    String guidValue = option.value; // De unieke ID/value uit VocBench

                    if (!cleanLabel.isEmpty()) {
                        sb.append("- ").append(cleanLabel).append("\n");
                        if (guidValue != null) {
                            labelToValueMap.put(cleanLabel, guidValue);
                        }
                    }
                }
            }

            labelsList = Arrays.stream(sb.toString().split("\n"))
                    .map(line -> line.trim())
                    .filter(line -> !line.toLowerCase().contains("unable to classify content"))
                    .filter(line -> !line.toLowerCase().contains("unclassified"))
                    .collect(Collectors.joining("\n"));

            // 4. Bouw de prompt dynamisch op
            String prompt = """
                    Classificeer de onderstaande documenttekst.
                    Kies uitsluitend EÉN van de volgende labels:
                    %s
                    Geef alleen het exacte label terug als antwoord.

                    Tekst:
                    %s
                    """.formatted(labelsList, extractedText);

            // 5. Query Gemini en valideer het resultaat
            List<String> validLabels = Arrays.asList(labelsList.split("\n"));
            String rawResponse = geminiReceiverService.evaluate(prompt);
            String suggestedLabel = rawResponse != null ? rawResponse.trim() : "";
            boolean isValid = false;

            for (String validLabel : validLabels) {
                String cleanValidLabel = validLabel.replaceFirst("^-\\s*", "").trim();
                if (cleanValidLabel.equalsIgnoreCase(suggestedLabel)) {
                    suggestedLabel = cleanValidLabel; // Zorg dat de hoofdletters exact matchen met de taxonomie
                    isValid = true;
                    break;
                }
            }

            if (!isValid) { 
                suggestedLabel = "No valid label detected : unable to classify";
            }

            // 6. Zoek de bijbehorende GUID (value) op basis van het gekozen label
            String correspondingGuid = labelToValueMap.getOrDefault(suggestedLabel, "UNKNOWN_GUID");

            // 7. Retourneer het resultaat inclusief de juiste GUID
            ClassificationResult result = new ClassificationResult(
                "Gemini", 
                suggestedLabel, 
                0.95, 
                "SUCCESS", 
                correspondingGuid
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new ClassificationResult("Gemini", "Unclassified", 0.0, "ERROR: " + e.getMessage(), "UNKNOWN_GUID"));
        }
    }
}