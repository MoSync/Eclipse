package com.mobilesorcery.sdk.html5.live;

import org.json.simple.JSONObject;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public interface ILiveServerListener {

	void received(String command, JSONObject json);

	void timeout(ReloadVirtualMachine vm);
}
