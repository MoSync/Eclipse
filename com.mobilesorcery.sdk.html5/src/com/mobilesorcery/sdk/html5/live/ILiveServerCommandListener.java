package com.mobilesorcery.sdk.html5.live;

import org.json.simple.JSONObject;

public interface ILiveServerCommandListener {

	void received(int sessionId, String command, JSONObject json);
	
}
