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

/**
 * <p>An interface for initializing properties in a decoupled manner.</p>
 * <p>An <code>IPropertyInitializer</code> always returns a <emph>context</emph>
 * in which it is applicable; like "mosync projects" -- then this property initializer
 * only affects mosync projects.
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IPropertyInitializer extends IPropertyInitializerDelegate {
	public static final IPropertyInitializer NULL = new IPropertyInitializer() {
		public String getDefaultValue(IPropertyOwner p, String key) {
			return "";
		}

		public String getContext() {
			return null;
		}

		public String getPrefix() {
			return null;
		}	
	};
	
	/**
	 * The prefix that should be prepended to all keys to avoid
	 * naming conflicts. (Think of it as a namespace.)
	 * @return
	 */
	public String getPrefix();
	
	/**
	 * The context in which this initializer applies.
	 * @return
	 */
	public String getContext();
	 
}
