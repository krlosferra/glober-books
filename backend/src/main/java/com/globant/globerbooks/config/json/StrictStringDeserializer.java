package com.globant.globerbooks.config.json;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Reads a JSON string into a {@link String} and nothing else. A number, boolean, array or object
 * is a type mismatch (no scalar coercion, SEC-F-122), and a string holding an unpaired surrogate is
 * refused (BR-006 item 1, DEC-027). Messages never carry the offending value.
 */
public class StrictStringDeserializer extends StdDeserializer<String> {

    private static final long serialVersionUID = 1L;

    /** Creates the deserializer for {@link String}. */
    public StrictStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            return (String) context.handleUnexpectedToken(String.class, parser);
        }
        String text = parser.getString();
        if (hasUnpairedSurrogate(text)) {
            context.reportInputMismatch(String.class, "unpaired surrogate");
        }
        return text;
    }

    /**
     * Tells whether a text holds a high surrogate without a following low surrogate, or a low
     * surrogate without a preceding high one.
     *
     * @param text the text
     * @return true when a surrogate is unpaired
     */
    static boolean hasUnpairedSurrogate(String text) {
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (Character.isHighSurrogate(current)
                    && index + 1 < text.length() && Character.isLowSurrogate(text.charAt(index + 1))) {
                index += 2;
            } else if (Character.isSurrogate(current)) {
                return true;
            } else {
                index++;
            }
        }
        return false;
    }
}
