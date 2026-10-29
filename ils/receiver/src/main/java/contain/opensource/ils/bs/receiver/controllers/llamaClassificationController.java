package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.web.bind.annotation.*;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import contain.opensource.ils.bs.receiver.services.LocalLlamaService;
import contain.opensource.ils.bs.receiver.classes.Classification.ClassificationResult;
import contain.opensource.ils.bs.receiver.model.TaxonomyResponse;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.Base64;
import java.util.stream.Collectors;
import contain.opensource.ils.bs.receiver.services.TaxonomyServiceClient;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class llamaClassificationController {

    private final Tika tika = new Tika();
    private final LocalLlamaService llamaService;
    private final TaxonomyServiceClient taxonomyServiceClient;

    @Autowired
    public llamaClassificationController(LocalLlamaService llamaService,
            TaxonomyServiceClient taxonomyServiceClient) {
        this.llamaService = llamaService;
        this.taxonomyServiceClient = taxonomyServiceClient;
    }

    @PostMapping(value = "/classifyllamaService", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
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

                if (!decodedText.isEmpty()) {
                    extractedText = decodedText;
                }
            } catch (IllegalArgumentException e) {
                // Was geen geldige Base64-payload, behoud de originele extractedText
            }
        }

        System.out.println("Extracted text to be classified : " + extractedText);
        if (extractedText.isBlank()) {
            return ResponseEntity.ok(new ClassificationResult("llamaclassifier", "Unstructured non text : unable to classify", 0.0, "EMPTY_CONTENT", "UNKNOWN_GUID"));
        }

        // 4. Truncation: voorkom dat hele lange documenten het LLM context window laten vollopen
        if (extractedText.length() > 4000) {
            extractedText = extractedText.substring(0, 4000);
        }

        // 5. Haal taxonomie op en bouw een mapping van Label -> Value (GUID)
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

        // 6. Bouw de prompt dynamisch op
        String prompt = """
                Classificeer de onderstaande documenttekst.
                Kies uitsluitend EÉN van de volgende labels:
                %s
                Geef alleen het exacte label terug als antwoord.

                Tekst:
                %s
                """.formatted(labelsList, extractedText);

        // 7. Query LLM
        List<String> validLabels = Arrays.asList(labelsList.split("\n"));
        String suggestedLabel = llamaService.evaluate(prompt).trim();
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

        // 8. Zoek de bijbehorende GUID (value) op basis van het gekozen label
        String correspondingGuid = labelToValueMap.getOrDefault(suggestedLabel, "UNKNOWN_GUID");

        // 9. Retourneer het resultaat inclusief de GUID in de record constructor
        ClassificationResult result = new ClassificationResult(
            "llamaclassifier", 
            suggestedLabel, 
            0.95, 
            "SUCCESS", 
            correspondingGuid
        );

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