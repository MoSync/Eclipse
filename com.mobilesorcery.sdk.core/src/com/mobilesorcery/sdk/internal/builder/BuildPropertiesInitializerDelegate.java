package com.mobilesorcery.sdk.internal.builder;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.IPropertyInitializerDelegate;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.sun.org.apache.bcel.internal.generic.MONITORENTER;

public class BuildPropertiesInitializerDelegate implements IPropertyInitializerDelegate {

    public BuildPropertiesInitializerDelegate() {
    }

    public String getDefaultValue(IPropertyOwner p, String key) {
        if (MoSyncBuilder.EXTRA_COMPILER_SWITCHES.equals(key)) {
            return "-O2";
        }
        
        return null;
    }

}
