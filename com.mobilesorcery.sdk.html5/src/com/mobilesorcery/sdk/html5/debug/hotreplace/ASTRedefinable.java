package com.mobilesorcery.sdk.html5.debug.hotreplace;

import org.eclipse.wst.jsdt.core.dom.ASTNode;

import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.Position;
import com.mobilesorcery.sdk.html5.debug.rewrite.ISourceSupport;

public abstract class ASTRedefinable extends AbstractRedefinable {

	private ASTNode node;

	public ASTRedefinable(IRedefinable parent, ISourceSupport source, ASTNode node) {
		super(parent, source);
		this.node = node;
	}
	
	protected ASTNode getNode() {
		return node;
	}
	
	public boolean isLineInSourceRange(int line) {
		Position start = source.getPosition(node, true);
		Position end = source.getPosition(node, false);
		return line >= start.getLine() && line <= end.getLine();
	}

}
