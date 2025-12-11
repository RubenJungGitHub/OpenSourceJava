package contain.opensource.java.ils.bs.receiver.constants;

public class AlfrescoConstants {

    public final static  String RED = "\u001B[31m";
    public final static String  RESET = "\u001B[0m";
    public final static String YELLOW = "\u001B[33m";
    public static final String CYAN    = "\u001B[36m";
    public static final String MAGENTA  = "\u001B[35m";  

    public enum NodeTypeFields {
        UUID,
        Title
    }

    public enum NodeType {
        NODEADDED,
        NODEUPDATED, // unsure if this is correct
        NODEDELETED;

        public static NodeType fromString(String value) {
            if (value == null)
                return null;

            try {
                return NodeType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null; // not a valid enum
            }
        }
    }
}
