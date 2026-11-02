package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.web.bind.annotation.*;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import contain.opensource.ils.bs.receiver.services.PythonClassifierService;
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
public class PythonClassificationController {

    private final Tika tika = new Tika();
    private final PythonClassifierService pythonClassifierService;
    private final TaxonomyServiceClient taxonomyServiceClient;

    @Autowired
    public PythonClassificationController(PythonClassifierService pythonClassifierService,
            TaxonomyServiceClient taxonomyServiceClient) {
        this.pythonClassifierService = pythonClassifierService;
        this.taxonomyServiceClient = taxonomyServiceClient;
    }

    @PostMapping(value = "/classifypythonService", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ClassificationResult> classifyByteArray(
            @RequestBody byte[] content) throws Exception {

        if (content == null || content.length == 0) {
            return ResponseEntity.badRequest().build();
        }

        // 1. Tika verwerkt de byte array rechtstreeks uit het geheugen
        String extractedText = tika.parseToString(new ByteArrayInputStream(content)).trim();

        // 2. Base64 afhandeling
        if (isBase64(extractedText)) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(extractedText.replaceAll("\\s+", ""));
                String decodedText = tika.parseToString(new ByteArrayInputStream(decodedBytes)).trim();

                if (!decodedText.isEmpty()) {
                    extractedText = decodedText;
                }
            } catch (IllegalArgumentException e) {
                // Geen geldige Base64-payload, behoud originele tekst
            }
        }

        System.out.println("Extracted text to be classified : " + extractedText);
        if (extractedText.isBlank()) {
            return ResponseEntity.ok(new ClassificationResult("bart-large-mnli", "Unstructured non text : unable to classify", 0.0, "EMPTY_CONTENT", "UNKNOWN_GUID"));
        }

        // 3. Truncation om te langeketen-problemen te voorkomen
        if (extractedText.length() > 4000) {
            extractedText = extractedText.substring(0, 4000);
        }

        // 4. Haal taxonomie op uit Vocbench
        TaxonomyResponse taxonomy = taxonomyServiceClient.GetTaxonomies("classification", "en");
        List<String> candidateLabels = new java.util.ArrayList<>();

        if (taxonomy != null && taxonomy.options != null) {
            for (var option : taxonomy.options) {
                String cleanLabel = option.label != null ? option.label.replaceFirst("^-\\s*", "").trim() : "";
                if (!cleanLabel.isEmpty()
                        && !cleanLabel.toLowerCase().contains("unable to classify content")
                        && !cleanLabel.toLowerCase().contains("unclassified")) {
                    candidateLabels.add(cleanLabel);
                }
            }
        }

        if (candidateLabels.isEmpty()) {
            candidateLabels = List.of("General");
        }

        // 5. Roep de Python service aan met de tekst en de lijst van kandidaat-labels
        ClassificationResult result = pythonClassifierService.evaluate(extractedText, candidateLabels);

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