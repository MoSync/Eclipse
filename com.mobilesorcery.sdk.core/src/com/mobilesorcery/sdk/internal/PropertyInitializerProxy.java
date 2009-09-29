package com.mobilesorcery.sdk.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IPropertyInitializer;
import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;

public class PropertyInitializerProxy implements IPropertyInitializer {

	private static final String CLASS = "class";

	private static final String CONTEXT = "context";

	private static final String PREFIX = "prefix";

	private IConfigurationElement element;

	private IPropertyInitializerDelegate delegate;

	private String context;

	private String prefix;

	public PropertyInitializerProxy(IConfigurationElement element) {
		this.element = element;
	}

	public String getDefaultValue(IPropertyOwner p, String key) {
		initDelegate();
		return delegate.getDefaultValue(p, key);
	}

	public String getContext() {
		initDelegate();
		return context;
	}
	
	public String getPrefix() {
		initDelegate();
		return prefix;
	}

	private void initDelegate() {
		try {
			if (delegate == null) {
				delegate = (IPropertyInitializerDelegate) element.createExecutableExtension(CLASS);
				context = getContext(element);
				prefix = getPrefix(element);
				element = null;
			}
		} catch (CoreException e) {
			CoreMoSyncPlugin.getDefault().log(e);
			delegate = IPropertyInitializer.NULL;
		}

	}

	public static String getPrefix(IConfigurationElement element) {
		return element.getAttribute(PREFIX);
	}

	public static String getContext(IConfigurationElement element) {
		return element.getAttribute(CONTEXT);
	}
	
}
