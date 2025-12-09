package contain.opensource.java.helloworld.constants;

public enum NodeType {
    NODEADDED,
    NODEUPDATED,  //unsure if this is correct
    NODEDELETED;

        public static NodeType fromString(String value) {
        if (value == null) return null;

        try {
            return NodeType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null; // not a valid enum
        }
    }
}
