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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class ResourceFilePartitionScanner extends RuleBasedPartitionScanner {
	//public final static String SINGLELINE_COMMENT = "res.sl.comment";
	public final static String COMMENT = "res.comment";
	public final static String DECLARATIONS = "res.decl";
	public final static String[] PARTITION_TYPES = new String[] { COMMENT };
	public static final String PARTITIONING = "res.partitioning";

	public ResourceFilePartitionScanner() {
		super();
		IToken comment = new Token(COMMENT);
		//IToken slComment = new Token(SINGLELINE_COMMENT);

		List rules = new ArrayList();
		rules.add(new MultiLineRule("/*", "*/", comment));
		//rules.add(new EndOfLineRule("//", comment));

		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}