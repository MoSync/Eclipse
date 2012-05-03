package com.mobilesorcery.sdk.html5.debug.hotreplace;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.jsdt.core.dom.ASTNode;

import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;

public abstract class AbstractRedefinable implements IRedefinable {

	protected ReloadVirtualMachine vm;
	private IRedefinable parent;
	private List<IRedefinable> children;
	private IProvider<String, ASTNode> source;

	protected AbstractRedefinable(IRedefinable parent, IProvider<String, ASTNode> source, ReloadVirtualMachine vm) {
		this.parent = parent;
		this.source = source;
		if (parent != null) {
			parent.addChild(this);	
		}
		
		this.vm = vm;
	}
	
	/**
	 * Returns the <b>instrumented</b> source of this node.
	 * @param node 
	 * @return
	 */
	protected String getSource(ASTNode node) {
		return source.get(node);
	}
	
	public void addChild(IRedefinable child) {
		if (children == null) {
			children = new ArrayList<IRedefinable>();
		}
		children.add(child);
	}

	@Override
	public boolean canRedefine(IRedefinable toBeRedefined, boolean inPlace) {
		if (toBeRedefined == null) {
			return true;
		}
		if (!(toBeRedefined instanceof ASTRedefinable)) {
			return false;
		}
		boolean equalKey = Util.equals(toBeRedefined.key(), key());
		return equalKey;
	}

	@Override
	public List<IRedefinable> getChildren() {
		return children;
	}

	@Override
	public IRedefinable getParent() {
		return parent;
	}
	
	/**
	 * Returns the ancestors of this {@link IRedefinable},
	 * as a list. Lower index = closer ancestor.
	 * @return
	 */
	protected List<IRedefinable> getAncestors() {
		ArrayList<IRedefinable> result = new ArrayList<IRedefinable>();
		IRedefinable parent = this.parent;
		while (parent != null) {
			result.add(parent);
			parent = parent.getParent();
		}
		return result;
	}

}
