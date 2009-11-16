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
package com.mobilesorcery.sdk.ui.editors.resources;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;


public class ResourcesFileScanner extends RuleBasedScanner {

    public static final String[] DIRECTIVES = new String[] { 
    	".eof",
    	".image",
    	".res",
    	".bin",
    	".ubin",
    	".media",
    	".umedia",
    	".sprite",
    	".tileset",
    	".tilemap",
    	".dispose",
    	".placeholder",
    	".skip",
    	".label",
    	".enum",
    	".string",
    	".cstring",
    	".pstring",
    	".fill",
    	".byte",
    	".half",
    	".word",
    	".include",
    	".extension",
    	".varint",
    	".varsint",
    	".end",
    	".eof",
    	".index",
    	".wideindex",
    	".set"
	};

    public static final int COMMENT_SCANNER = 0;
    public static final int CODE_SCANNER = 1;
	
	public static final String COMMENT_COLOR = "comment";
	public static final String DEFAULT_TEXT_COLOR = "text";
	public static final String DIRECTIVE_COLOR = "directive";
	public static final String STRING_COLOR = "string";

	public final static RGB COMMENT_DEFAULT_RGB = new RGB(0x00, 0xaf, 0x00);
	public static final RGB DEFAULT_TEXT_DEFAULT_RGB = new RGB(0x00, 0x00, 0x00);
	public static final RGB STRING_DEFAULT_RGB = new RGB(0x00, 0x00, 0xaf);
	public static final RGB DIRECTIVE_DEFAULT_RGB = new RGB(0xaf, 0x00, 0x00);


	private SyntaxColorPreferenceManager manager;

	private int type;

    public ResourcesFileScanner(SyntaxColorPreferenceManager manager, int type) {    	
    	this.manager = manager;
    	this.type = type;
    	reinit();
    }
    
    public void reinit() {    	
        IToken directiveToken =
            new Token(createTextAttribute(manager, DIRECTIVE_COLOR));
        
        IToken commentToken = new Token(createTextAttribute(manager, COMMENT_COLOR));
        
        IToken defaultToken = new Token(createTextAttribute(manager, DEFAULT_TEXT_COLOR));

        WordRule rule = new WordRule(new DirectiveDetector(), defaultToken);
        for (int i = 0; i < DIRECTIVES.length; i++) {
            rule.addWord(DIRECTIVES[i], directiveToken);
        }
        
        IToken stringToken = new Token(createTextAttribute(manager, STRING_COLOR));
		SingleLineRule string = new SingleLineRule("\"", "\"", stringToken, '\\');

        PatternRule slComment = new PatternRule("//", "\n", commentToken, '\\', true, true);
        MultiLineRule mlComment = new MultiLineRule("/*", "*/", commentToken);
        
        WhitespaceRule ws = new WhitespaceRule(new WhiteSpaceDetector());
        
        setDefaultReturnToken(defaultToken);
        setRules(type == COMMENT_SCANNER ? new IRule[] {mlComment} : new IRule[] { ws, mlComment, slComment, string, rule });
    }

    public static TextAttribute createTextAttribute(SyntaxColorPreferenceManager manager, String prefId) {
    	SyntaxColoringPreference pref = manager.get(prefId);
    	if (pref != null) {
    		Color foreground = manager.getForeground(prefId);    	
    		int style = (pref.isBold() ? SWT.BOLD : 0) | (pref.isItalic() ? SWT.ITALIC : 0) | (pref.isUnderline() ? SWT.UNDERLINE_SINGLE : 0);
    		return new TextAttribute(foreground, null, style);
    	} else {
    		return new TextAttribute(null);
    	}
    }

}
