package com.mobilesorcery.sdk.html5.live;

import org.json.simple.JSONObject;

public interface ILiveServerListener {

	void received(String command, JSONObject json);

}
