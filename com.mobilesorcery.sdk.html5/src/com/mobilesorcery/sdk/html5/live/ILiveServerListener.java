package com.mobilesorcery.sdk.html5.live;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public interface ILiveServerListener {

	/**
	 * Called when a vm has been terminated
	 * @param vm The terminated vm
	 */
	void timeout(ReloadVirtualMachine vm);
	
	/**
	 * Called when a device first connects
	 * to a vm
	 * @param vm
	 * @param reset If this was not the first
	 * connect
	 */
	void inited(ReloadVirtualMachine vm, boolean reset);
}
