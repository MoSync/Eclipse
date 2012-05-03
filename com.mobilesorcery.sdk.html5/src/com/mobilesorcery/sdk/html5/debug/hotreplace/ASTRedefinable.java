package com.mobilesorcery.sdk.html5.debug.hotreplace;

import java.util.List;

import org.eclipse.wst.jsdt.core.dom.ASTNode;

import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;

public abstract class ASTRedefinable extends AbstractRedefinable {

	private ASTNode node;

	public ASTRedefinable(IRedefinable parent, IProvider<String, ASTNode> source, ReloadVirtualMachine vm, ASTNode node) {
		super(parent, source, vm);
		this.node = node;
	}
	
	@Override
	public boolean canRedefine(IRedefinable toBeRedefined, boolean inPlace) {
		boolean canRedefine = super.canRedefine(toBeRedefined, inPlace);
		canRedefine &= !getSource(node).equals(((ASTRedefinable) toBeRedefined).getSource(node));
		if (!inPlace) {
			// If not in-place, we need only redefine the topmost function/var decl.
			List<IRedefinable> ancestors = getAncestors();
			canRedefine &= ancestors.isEmpty();
		}
		return canRedefine;
	}
	
	protected ASTNode getNode() {
		return node;
	}

}
