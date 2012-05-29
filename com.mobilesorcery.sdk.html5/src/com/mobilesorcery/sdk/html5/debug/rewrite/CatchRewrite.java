package com.mobilesorcery.sdk.html5.debug.rewrite;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.CatchClause;
import org.eclipse.wst.jsdt.core.dom.TryStatement;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.html5.debug.Position;

public class CatchRewrite extends NodeRewrite {

	public CatchRewrite(ISourceSupport source, ASTNode node) {
		super(source, node);
	}
	
	@Override
	public void rewrite(IFilter<String> features, IRewrite rewrite) {
		CatchClause catchClause = (CatchClause) getNode();
		String exceptionVar = catchClause.getException().getName().getIdentifier();
		Block body = catchClause.getBody();
		Position bodyStart = getPosition(body, true);
		String source = getSource(body);
		int insideBodyStart = source.indexOf("{");
		if (insideBodyStart != -1) {
			insideBodyStart += bodyStart.getPosition() + 1;
		}
		
		rewrite.seek(insideBodyStart);
		rewrite.insert("if (" + exceptionVar + ".dropToFrame) { throw " + exceptionVar + "; }");
	
		defaultRewrite(features, rewrite);
	}

}
