package com.mobilesorcery.sdk.core;

public class Pair<S, T> {

	public final S first;
	public final T second;
	
	public Pair(S first, T second) {
		if (first == null || second == null) {
			throw new IllegalArgumentException();
		}
		
		this.first = first;
		this.second = second;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			return ((Pair) o).first.equals(this.first) &&
			((Pair) o).second.equals(this.second);
		}
		
		return false;
	}
	
	public int hashCode() {
		return first.hashCode() ^ second.hashCode();
	}
}
