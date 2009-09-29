/**
 * 
 */
package com.mobilesorcery.sdk.internal;

public class ParseException extends Exception {
    private int line;

    public ParseException(String message, int line) {
        super(message);
        this.line = line;
    }

    public int getLineNumber() {
        return line;
    }
}