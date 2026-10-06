package contain.opensource.ils.bs.receiver.classes.Classification;
public record ClassificationResult(
    String classifier,
    String label,
    double confidence,
    String status
) {}