package com.druvu.json;

import java.io.IOException;
import java.io.Writer;

public interface JsonBuilder {

    /**
     * Write the JSON to a writer.
     *
     * @param out The output writer.
     * @throws IOException if there was a problem
     */
    void write(Writer out) throws IOException;

    /** @return The serialized JSON as a string. */
    @Override
    String toString();
}
