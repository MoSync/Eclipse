package com.mobilesorcery.sdk.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class Cache<K, V> extends LinkedHashMap<K, V> {

	private final int maxSize;

	public Cache(int maxSize) {
		this.maxSize = maxSize;
	}

	protected int maxSize() {
		return maxSize;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
		boolean remove = size() > maxSize();
		if (remove) {
			onRemoval(entry.getKey(), entry.getValue());
		}
		return remove;
	}

	@Override
	public void clear() {
		for (Map.Entry<K, V> entry : entrySet()) {
			onRemoval(entry.getKey(), entry.getValue());
		}
		super.clear();
	}

	/**
	 * Clients may override. Will be called after removal
	 * from cache.
	 * @param value
	 */
	protected void onRemoval(K key, V value) {

	}
}
