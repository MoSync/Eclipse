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
package com.mobilesorcery.sdk.profiles.filter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.profiles.IProfile;

public class PlatformDeviceFilter extends AbstractDeviceFilter {

	public final static int EXACT_MATCH = 0;
	public final static int STARTS_WITH = 1;
	public final static int CONTAINS = 2;
	public final static int REGEXP = 3;
	
	private String matchingPlatform;
	private int matchStrategy;
	private Pattern platformPattern;

	/**
	 * <p>Creates a platform filter that accepts the
	 * set of platforms defined by <code>platform</code>
	 * @param platform A string that should match the profile's
	 * name
	 * @param matchStrategy How the string should be matched; using
	 * one of these strategies:
	 * <ul>
	 * <li><code>EXACT_MATCH</code></li>
	 * <li><code>STARTS_WITH</code></li>
	 * <li><code>CONTAINS</code></li>
	 * <li><code>REGEXP</code></li> 
	 * </ul>
	 * @throws PatternSyntaxException If using the <code>REGEXP</code>
	 * strategy and <code>platform</code> is an invalid regular expression.
	 */
	public PlatformDeviceFilter(String platform, int matchStrategy) {
		this.matchingPlatform = platform;
		this.matchStrategy = matchStrategy;
		if (matchStrategy == REGEXP) {
			platformPattern = Pattern.compile(platform);
		}
	}
	
	public boolean acceptProfile(IProfile profile) {
		String platform = profile.getPlatform();
		if (platform == null) {
			return false;
		}
		
		switch (matchStrategy) {
		case EXACT_MATCH:
			return platform.equals(matchingPlatform);
		case STARTS_WITH:
			return platform.startsWith(matchingPlatform);
		case CONTAINS:
			return platform.contains(matchingPlatform);
		case REGEXP:
			return platformPattern.matcher(platform).matches();
		default:
			throw new IllegalArgumentException("Invalid match strategy"); //$NON-NLS-1$
		}
	}

	public String getFactoryId() {
		throw new UnsupportedOperationException();
	}

	public void saveState(IMemento memento) {
		throw new UnsupportedOperationException();
	}

}
