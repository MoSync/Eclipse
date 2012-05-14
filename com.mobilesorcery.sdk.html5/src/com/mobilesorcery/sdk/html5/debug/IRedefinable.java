package com.mobilesorcery.sdk.html5.debug;

import java.util.List;


public interface IRedefinable {

	/**
	 * A 'primary key' of this {@link IRedefinable}. To be able to redefine, the
	 * primary key of this {@link IRedefinable} and another must match.
	 * 
	 * @return
	 */
	public String key();

	/**
	 * Performs a redefinition; this method delegates to a {@link IRedefinable}
	 * and traverses the tree of child redefinitions.
	 * @see IRedefiner
	 * @param replacement
	 * @param redefiner
	 */
	public void redefine(IRedefinable replacement, IRedefiner redefiner);

	public List<IRedefinable> getChildren();

	public IRedefinable getParent();

	public void addChild(IRedefinable child);

	public IRedefinable getChild(String key);

}
