package com.mobilesorcery.sdk.ui.editors.resources;

import org.eclipse.jface.text.rules.IWordDetector;

public class DirectiveDetector implements IWordDetector {

    public boolean isWordPart(char part) {
        return Character.isLetter(part);
    }

    public boolean isWordStart(char start) {
        return start == '.';
    }

}
