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
        if (content == null || content.length == 0) {
            return ResponseEntity.badRequest().build();
        }

        String extractedText = tika.parseToString(new ByteArrayInputStream(content)).trim();

        if (extractedText.isBlank()) {
            return ResponseEntity.ok(new ClassificationResult("Gemini", "Unclassified", 0.0, "EMPTY_CONTENT"));
        }

        if (extractedText.length() > 4000) {
            extractedText = extractedText.substring(0, 4000);
        }

        // 1. Haal de taxonomie op via het directe veld 'options'
        TaxonomyResponse taxonomy = taxonomyServiceClient.GetTaxonomies("classification", "en");

        String labelsList = "Failed to retrieve labels";
        if (taxonomy != null && taxonomy.options != null) {
            StringBuilder sb = new StringBuilder();
            for (var option : taxonomy.options) {
                sb.append("- ").append(option.label).append("\n");
            }
            labelsList = sb.toString().trim();
        }

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

        if (suggestedLabel != null) {
            suggestedLabel = suggestedLabel.replaceAll("[^a-zA-Z]", "").trim();
        } else {
            suggestedLabel = "Unclassified";
        }

        return ResponseEntity.ok(new ClassificationResult("Gemini", suggestedLabel, 0.95, "SUCCESS"));
    }
}