package com.mobilesorcery.sdk.ui.editors.resources;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class WhiteSpaceDetector implements IWhitespaceDetector {

    public boolean isWhitespace(char ch) {
        return Character.isWhitespace(ch);
    }

}
