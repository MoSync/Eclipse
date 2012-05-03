package com.mobilesorcery.sdk.html5.debug;

import java.util.List;


public interface IRedefinable {

	/**
	 * A 'primary key' of this {@link IRedefinable}.
	 * To be able to redefine, the primary key of
	 * this {@link IRedefinable} and another must match.
	 * @return
	 */
	public String key();
	
	/**
	 * Returns {@code true} if {@code peer} can be redefined.
	 * It can be redefined if it has a {@link #key()} equal
	 * to this {@link #key()},
	 * and if the actual contents of the two {@link IRedefinable}s
	 * differ. If {@link peer} is {@code null}, this method
	 * returns {@code true}.
	 * @param toBeRedefined
	 * @param inPlace
	 * @return
	 */
	public boolean canRedefine(IRedefinable toBeRedefined, boolean inPlace);
	
	public void redefine(IRedefinable toBeRedefined, boolean inPlace) throws RedefineException;
	
	public List<IRedefinable> getChildren();
	
	public IRedefinable getParent();

	public void addChild(IRedefinable child);
	
}
