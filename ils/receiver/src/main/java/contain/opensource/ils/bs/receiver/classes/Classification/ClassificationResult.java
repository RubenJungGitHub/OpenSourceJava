package contain.opensource.ils.bs.receiver.classes.Classification;
public record ClassificationResult(
    String label,
    double confidence,
    String status
) {}