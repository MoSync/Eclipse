package com.mobilesorcery.sdk.core;


/**
 * The methods of <code>IPropertyInitializer</code> that
 * needs to be implemented if provided via an extension point
 * (com.mobilesorcery.core.propertyinitializers)
 * @author Mattias Bybro, mattias.bybro@purplescout.com
 *
 */
public interface IPropertyInitializerDelegate {
	public static final String EXTENSION_POINT = "com.mobilesorcery.core.propertyinitializers";
	
	public String getDefaultValue(IPropertyOwner p, String key);
}
