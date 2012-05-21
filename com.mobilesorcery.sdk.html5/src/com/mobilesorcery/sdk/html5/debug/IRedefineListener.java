package com.mobilesorcery.sdk.html5.debug;

import java.util.Map;

public interface IRedefineListener {

	/**
	 * A callback method for when a new set
	 * of {@link IRedefinable}s have been produced.
	 * @param oldRedefinables
	 * @param newRedefinables 
	 */
	void redefined(IRedefinable oldRedefinable, IRedefinable newRedefinable);

}
