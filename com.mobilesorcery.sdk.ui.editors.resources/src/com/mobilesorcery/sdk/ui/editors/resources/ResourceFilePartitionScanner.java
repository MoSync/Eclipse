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