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

public class EmulatorStacktracePatternMatchListener implements IPatternMatchListenerDelegate {

    private TextConsole console;

    public void connect(TextConsole console) {
        this.console = console;
    }

    public void disconnect() {
        console = null;
    }

    public void matchFound(PatternMatchEvent event) {
        try {
            IDocument doc = console.getDocument();
            String stackentry = doc.get(event.getOffset(), event.getLength());
            String[] splitEntry = stackentry.split(":", 3); // IP:ip: - 2 colons
                                                            // we should ignore
            if (splitEntry.length == 3) {
                // The 3 is because there is 2 colons and one space
                int newLength = event.getLength() - (splitEntry[0].length() + splitEntry[1].length() + 3);              
                int newOffset = event.getOffset() + (event.getLength() - newLength);
                
                String[] fileAndLine = splitEntry[2].split(":");
                String file = Util.join(fileAndLine, ":", 0, fileAndLine.length - 2);
                file = file.trim();
                String line = fileAndLine[fileAndLine.length - 1];
                
                Path filepath = new Path(file);

                IWorkspace ws = ResourcesPlugin.getWorkspace();
                IFile[] files = ws.getRoot().findFilesForLocation(filepath);

                int lineNo = 1;
                try {
                     lineNo = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    // Ignore.
                }

                if (files.length > 0) {                    
                    console.addHyperlink(new PathLink(files[0], null, -1, -1, lineNo), newOffset, newLength);
                } else if (filepath.toFile().exists()) {
                    console.addHyperlink(new PathLink(filepath, null, -1, -1, lineNo), newOffset, newLength);
                }
            }
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
}
