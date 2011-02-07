package com.mobilesorcery.sdk.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultParameterResolver extends ParameterResolver {

	private Map<String, String> map;

	public DefaultParameterResolver(Map<String, String> map) {
		this.map = map;
	}

	@Override
	public String get(String key) {
		return map.get(key);
	}

	@Override
	public List<String> listPrefixes() {
		return new ArrayList<String>(map.keySet());
	}

}
