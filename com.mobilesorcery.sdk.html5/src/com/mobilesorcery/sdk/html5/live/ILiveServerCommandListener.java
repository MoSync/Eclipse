package com.mobilesorcery.sdk.html5.live;

import org.json.simple.JSONObject;

public interface ILiveServerCommandListener {

	void received(String command, JSONObject json);
	
}
