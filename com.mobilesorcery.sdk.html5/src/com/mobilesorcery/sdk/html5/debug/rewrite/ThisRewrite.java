package com.mobilesorcery.sdk.html5.debug.rewrite;

import org.eclipse.wst.jsdt.core.dom.ASTNode;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.Position;


public class ThisRewrite extends NodeRewrite {

	public ThisRewrite(ISourceSupport source, ASTNode node) {
		super(source, node);
	}
	
	public void rewrite(IFilter<String> features, IRewrite rewrite) {
		ASTNode node = getNode();
		Position start = getPosition(node, true);
		rewrite.seek(start);
		// If applicable, prepend this with the special prefix: ____
		if (supports(null, JSODDSupport.EDIT_AND_CONTINUE)) {
			rewrite.insert("____");
		}
	}

}
