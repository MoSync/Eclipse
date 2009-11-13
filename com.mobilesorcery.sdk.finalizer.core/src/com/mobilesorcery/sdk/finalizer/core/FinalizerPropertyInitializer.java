package com.mobilesorcery.sdk.finalizer.core;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;

public class FinalizerPropertyInitializer implements
		IPropertyInitializerDelegate {


	public String getDefaultValue(IPropertyOwner p, String key) {
		if (FinalizerParser.AUTO_CHANGE_CONFIG.equals(key)) {
			return PropertyUtil.fromBoolean(true);
		} else if (FinalizerParser.BUILD_CONFIG.equals(key)) {
			return IBuildConfiguration.RELEASE_ID;
		}
		
		return null;
	}

}
