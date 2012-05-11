package com.mobilesorcery.sdk.html5.debug.hotreplace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.wst.jsdt.core.dom.ASTNode;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.IRedefiner;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.debug.rewrite.ISourceSupport;

public abstract class AbstractRedefinable implements IRedefinable {

	private IRedefinable parent;
	private List<IRedefinable> children = new ArrayList<IRedefinable>();
	private ISourceSupport source;
	private HashMap<String, IRedefinable> childrenByKey;

	protected AbstractRedefinable(IRedefinable parent, ISourceSupport source) {
		this.parent = parent;
		this.source = source;
		if (parent != null) {
			parent.addChild(this);	
		}
	}
	
	/**
	 * Returns the <b>instrumented</b> source of this node.
	 * @param node 
	 * @return
	 */
	protected String getInstrumentedSource(IFilter<String> features, ASTNode node) {
		return source.getInstrumentedSource(features, node);
	}
	
	public void addChild(IRedefinable child) {
		children.add(child);
		// Just to trigger reindexing.
		childrenByKey = null;
	}
	
	public IRedefinable getChild(String key) {
		if (childrenByKey == null) {
			childrenByKey = new HashMap<String, IRedefinable>();
			for (IRedefinable child : children) {
				childrenByKey.put(child.key(), child);
			}
		}
		return childrenByKey.get(key);
	}
	
	/**
	 * The default implementation redefines all children as well,
	 * delegating to the {@link #redefineAdded(IRedefinable, ReloadVirtualMachine)}
	 * method if the replacement has a child not present in this
	 * {@link IRedefinable}.
	 */
	@Override
	public void redefine(IRedefinable replacement, IRedefiner redefiner) {
		if (replacement != null && !Util.equals(replacement.key(), key())) {
			throw new IllegalArgumentException("Internal error: key mismatch");
		}
		if (redefiner != null) {
			redefiner.collect(this, replacement);
		}
		HashSet<String> redefined = new HashSet();
		for (IRedefinable child : getChildren()) {
			String key = child.key();
			IRedefinable replacementChild = replacement.getChild(key);
			if (replacementChild != null) {
				replacementChild.redefine(child, redefiner);
				redefined.add(key);
			}
		}
		
		for (IRedefinable replaceChild : replacement.getChildren()) {
			if (!redefined.contains(replaceChild.key())) {
				redefineAdded(replaceChild, redefiner);
			}
		}
	}

	/**
	 * Clients may override.
	 * @param replaceChild
	 * @param redefiner
	 */
	protected void redefineAdded(IRedefinable replaceChild, IRedefiner redefiner) {
		
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
	
	public String constructKey(String subkey) {
		String parentKey = parent == null ? "" : parent.key();
		return parentKey + "/" + subkey;
	}

}
