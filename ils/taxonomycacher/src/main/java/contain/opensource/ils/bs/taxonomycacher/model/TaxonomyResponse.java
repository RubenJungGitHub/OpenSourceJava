package contain.opensource.ils.bs.taxonomycacher.model;


import java.util.List;

public class TaxonomyResponse {
    public List<Option> options;
    public String classification;
    public String policy;

    public static class Option {
        public String label;
        public String value;
    }
}