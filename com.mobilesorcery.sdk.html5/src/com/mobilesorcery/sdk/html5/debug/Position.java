package com.mobilesorcery.sdk.html5.debug;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

public class Position {
	private final ASTNode node;
	private final JavaScriptUnit unit;
	private final boolean before;
	private int offset;

	public Position(ASTNode node, JavaScriptUnit unit, boolean before) {
		this(node, unit, before, 0);
	}
	
	public Position(ASTNode node, JavaScriptUnit unit, boolean before, int offset) {
		this.node = node;
		this.unit = unit;
		this.before = before;
		this.offset = offset;
	}

	public int getLine() {
		// Special case since JSDT and 'my' positions are offset by 1...
		// TODO: Reconcile these models, but only once everything works
		// to satisfaction.
		int pos = getPosition();
		if (pos >= unit.getLength()) {
			return unit.getLineNumber(unit.getLength() - 1);
		}
		return unit.getLineNumber(pos);
	}

	public int getColumn() {
		int pos = getPosition();
		if (pos >= unit.getLength()) {
			return unit.getColumnNumber(unit.getLength() - 1) + 1;
		}
		return unit.getColumnNumber(pos);
	}

	public int getPosition() {
		int unOffset = before ? node.getStartPosition()
				: (node.getStartPosition() + node.getLength());
		return unOffset + offset;
	}

	public Position copy() {
		return new Position(node, unit, before, offset);
	}
	
	public Position offset(int offset) {
		Position copy = copy();
		copy.offset += offset;
		return copy;
	}
}