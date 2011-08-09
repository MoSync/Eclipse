package com.mobilesorcery.sdk.builder.iphoneos;

public class SDK {

	private String name;
	
	private String id;

	SDK(String id, String name) {
		this.name = name;
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean isSimulatorSDK() {
		// Some kind of heuristic...
		return name.contains("Simulator");
	}

}
