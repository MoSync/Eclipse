package com.mobilesorcery.sdk.html5.debug;

import java.util.List;

/**
 * Visitor-like interface for interacting with the
 * {@link IRedefinable#redefine(IRedefinable, IRedefinableCollector)}
 * method.
 * @author Mattias Bybro
 *
 */
public interface IRedefiner {

	/**
	 * Collects redefinition information. Will not have any
	 * side-effects until {@link #commit()} is called.
	 * @param redefinable
	 * @param replacement
	 * @param problems
	 * @return 
	 */
	public void collect(IRedefinable redefinable, IRedefinable replacement);
	
	/**
	 * Commits previously collected information.
	 * @throws RedefineException
	 */
	public void commit(boolean reloadHint) throws RedefineException;

}
