package com.mobilesorcery.sdk.core;

import java.util.Map;

public class DefaultParameterResolver implements ParameterResolver {

	private Map<String, String> map;

	public DefaultParameterResolver(Map<String, String> map) {
		this.map = map;
	}

	@Override
	public String get(String key) {
		return map.get(key);
	}

}
