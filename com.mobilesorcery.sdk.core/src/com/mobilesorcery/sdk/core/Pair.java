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
		this.first = first;
		this.second = second;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			Pair other = (Pair) o;
			return Util.equals(other.first, first) && Util.equals(other.second, second);
		}

		return false;
	}

	@Override
	public int hashCode() {
		int firstHc = first == null ? 0 : first.hashCode();
		int secondHc = second == null ? 0 : second.hashCode();
		return firstHc ^ secondHc;
	}

	@Override
	public String toString() {
		return "{" + first + "," + second + "}";
	}
}
