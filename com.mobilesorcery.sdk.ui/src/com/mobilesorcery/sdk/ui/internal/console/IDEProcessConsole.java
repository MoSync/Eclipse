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

import org.eclipse.cdt.internal.ui.preferences.BuildConsolePreferencePage;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.mobilesorcery.sdk.core.AbstractProcessConsole;

public class IDEProcessConsole extends AbstractProcessConsole {

    private MessageConsole console;
    private MessageConsoleStream error;
    private MessageConsoleStream input;
    private MessageConsoleStream message;
    
    private Color errorColor;
    private Color inputColor;
    private Color messageColor;
    private Color bkgColor;

    public IDEProcessConsole(String name) {
        console = findConsole(name);
        initStreams();
    }

    private void initStreams() {
        if (error == null) error = console.newMessageStream();
        if (input == null) input = console.newMessageStream();
        if (message == null) message = console.newMessageStream();

        setStreamColorsFromPrefs();
        setTabWidthFromPrefs();
    }

    private void setStreamColorsFromPrefs() {
    	final Display d = Display.getDefault();
        d.asyncExec(new Runnable() {
			public void run() {
            	// These colors correspond roughly to 
            	// error, output, and info colors of
            	// the CDT console colors
            	disposeColor(errorColor);
            	disposeColor(inputColor);
            	disposeColor(messageColor);            	
            	disposeColor(bkgColor);            	
            	RGB errorRGB = PreferenceConverter.getColor(CUIPlugin.getDefault().getPreferenceStore(), BuildConsolePreferencePage.PREF_BUILDCONSOLE_ERROR_COLOR);
            	RGB inputRGB = PreferenceConverter.getColor(CUIPlugin.getDefault().getPreferenceStore(), BuildConsolePreferencePage.PREF_BUILDCONSOLE_OUTPUT_COLOR);
            	RGB messageRGB = PreferenceConverter.getColor(CUIPlugin.getDefault().getPreferenceStore(), BuildConsolePreferencePage.PREF_BUILDCONSOLE_INFO_COLOR);
            	RGB bkgRGB = PreferenceConverter.getColor(CUIPlugin.getDefault().getPreferenceStore(), BuildConsolePreferencePage.PREF_BUILDCONSOLE_BACKGROUND_COLOR);            	
            	errorColor = new Color(d, errorRGB);
            	inputColor = new Color(d, inputRGB);
            	messageColor = new Color(d, messageRGB);
            	bkgColor = new Color(d, bkgRGB);
                error.setColor(errorColor);
                input.setColor(inputColor);
                message.setColor(messageColor);
                console.setBackground(bkgColor);
            }
        });
	}
    
    private void setTabWidthFromPrefs() {
    	console.setTabWidth(CUIPlugin.getDefault().getPreferenceStore().getInt(BuildConsolePreferencePage.PREF_BUILDCONSOLE_TAB_WIDTH));
    }

	private void disposeColor(Color c) {
		if (c != null) {
			c.dispose();
		}
	}

	protected void writeLine(int type, String line) {
    	switch (type) {
    		case OUT:
    			writeLine(input, line);
    			break;
    		case ERR:
    			writeLine(error, line);
    			break;
			default:
				writeLine(message, line);
    	}
    }
    
    private void writeLine(MessageConsoleStream stream, String line) {
        try {
            stream.write(line);
            stream.write('\n');
            console.activate();
        } catch (Exception e) {
        }
    }

    private MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsole[] existing = plugin.getConsoleManager().getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (name.equals(existing[i].getName()) && existing[i] instanceof MessageConsole) {
                return (MessageConsole) existing[i];
            }
        }

        MessageConsole myConsole = new MessageConsole(name, null);
        
        // Ehm... this isn't exactly what we want... but we'll keep it this way for a while
        if (BuildConsolePreferencePage.isAutoOpenConsole() || BuildConsolePreferencePage.isConsoleOnTop()) {
        	myConsole.activate();
        }
        plugin.getConsoleManager().addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }

    /**
     * Returns the underlying Eclipse <code>IConsole</code>
     * of this <code>IDEProcessConsole</code>.
     * @return
     */
    public IConsole getConsole() {
        return console;
    }

    /* (non-Javadoc)
	 * @see com.mobilesorcery.sdk.core.api.IProcessConsole#clear()
	 */
    public void clear() {
        if (console != null) {
            console.clearConsole();
        }
    }

	public void prepare() {
		if (BuildConsolePreferencePage.isClearBuildConsole()) {
			clear();	
		}		
	}
    
}
