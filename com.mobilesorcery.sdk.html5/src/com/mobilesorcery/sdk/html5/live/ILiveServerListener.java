package com.mobilesorcery.sdk.html5.live;

import org.json.simple.JSONObject;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public interface ILiveServerListener {

	void received(String command, JSONObject json);

	/**
	 * Called when a vm has been terminated
	 * @param vm The terminated vm
	 */
	void timeout(ReloadVirtualMachine vm);
}
