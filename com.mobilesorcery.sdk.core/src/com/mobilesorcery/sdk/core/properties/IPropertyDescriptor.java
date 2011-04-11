/*  Copyright (C) 2011 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core.properties;

/**
 * <p>An interface representing a property (oftentimes
 * associated with a project).</p>
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 * @param <PropertyType>
 */
public interface IPropertyDescriptor<PropertyType> extends IPropertyStorage<PropertyType> {

	public String getName();
	
	public Class<PropertyType> getType();

}
