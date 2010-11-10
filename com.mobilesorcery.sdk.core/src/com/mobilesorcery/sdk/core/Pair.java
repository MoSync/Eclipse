/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
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
	
	public String toString() {
		return "{" + first + "," + second + "}";
	}
}
