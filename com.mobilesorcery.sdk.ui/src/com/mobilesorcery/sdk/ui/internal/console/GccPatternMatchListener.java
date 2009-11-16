/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.ui.internal.console;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

import com.mobilesorcery.sdk.core.Util;

public class GccPatternMatchListener implements IPatternMatchListenerDelegate {

    private TextConsole console;

    public void connect(TextConsole console) {
        this.console = console;
    }

    public void disconnect() {
        this.console = null;
    }
 
    public void matchFound(PatternMatchEvent event) {
        try {
            IDocument doc = console.getDocument();
            int newOffset = event.getOffset();

            String unsplitFileAndLine = doc.get(newOffset, event.getLength());
            
            String[] fileAndLine = unsplitFileAndLine.split(":");
            
            if (fileAndLine.length < 3) {
                return;
            }

            // Heuristic: the file is always first, then we'll split and find the first
            // part that is a number; that is the line.
            int lineIndex = 1;
            for (; lineIndex < fileAndLine.length; lineIndex++) {
                if (fileAndLine[lineIndex].length() > 0) {
                    if (Character.isDigit(fileAndLine[lineIndex].charAt(0))) {
                       try {
                           Integer.parseInt(fileAndLine[lineIndex]);
                           break;
                       } catch (NumberFormatException e) {
                           // Not a number then.
                       }
                    }
                }
            }
            
            String file = Util.join(fileAndLine, ":", 0, lineIndex - 1);
            file = file.trim();

            String line = lineIndex < fileAndLine.length ? fileAndLine[lineIndex] : "";
            
            int newLength = file.length() + line.length() + 1; // 1 for the colon.
            
            Path filepath = new Path(file);

            IWorkspace ws = ResourcesPlugin.getWorkspace();
            IFile[] files = ws.getRoot().findFilesForLocation(filepath);

            int lineNo = 1;
            try {
                lineNo = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                return;
            }

            if (files.length > 0) {
                console.addHyperlink(new PathLink(files[0], null, -1, -1, lineNo), newOffset, newLength);
            } else if (filepath.toFile().exists()) {
                console.addHyperlink(new PathLink(filepath, null, -1, -1, lineNo), newOffset, newLength);
            }
        } catch (BadLocationException e) {
            // Ignore.
        }
    }

}
