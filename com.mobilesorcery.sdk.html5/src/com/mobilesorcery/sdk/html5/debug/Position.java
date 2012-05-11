package com.mobilesorcery.sdk.html5.debug;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.ASTVisitor;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;

// TODO: Rename to reange, remove 'before' argument, add getEndPosition, getStartPosition
public class Position {
	private int line;
	private int column;
	private int position;
	
	public Position(ASTNode node, LineMap lineMap, boolean before) {
		this(node, lineMap, before, 0);
	}
	
	public Position(ASTNode node, LineMap lineMap, boolean before, int offset) {
		int start = node.getStartPosition();
		int length = node.getLength();
		
		int unOffset = before ? start : start + length;
		position = unOffset + offset;
		
		// Line & Column
		line = lineMap.getLine(position);
		column = lineMap.getColumn(position);
	}

	public int getLine() {
		return line;
	}
	
	public int getColumn() {
		return column; 
	}

	public int getPosition() {
		return position;
	}

	/*public static int[] getUncommentedRange(ASTNode node) {
		int[] result = new int[2];
		result[0] = node.getStartPosition();
		result[1] = node.getLength();
		if (node instanceof BodyDeclaration) {
			BodyDeclaration body = (BodyDeclaration) node;
			JSdoc doc = body.getJavadoc();
			// JSDOC for a node can be located outside the parent's range!!
			// Horrible hack to workaround the AST's lack of info.
			if (doc != null) {
				ASTNode parentLocatedWithin = body.getParent();
				while (parentLocatedWithin != null && parentLocatedWithin.getStartPosition() <= body.getStartPosition()) {
					parentLocatedWithin = parentLocatedWithin.getParent();
				}
				if (parentLocatedWithin != null) {
					int lengthDiff = body.getLength() - parentLocatedWithin.getLength();
					result[0] += lengthDiff;
					result[1] -= lengthDiff;
				}
			}
		}
		return result;
	}*/

}