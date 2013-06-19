package com.mobilesorcery.sdk.extensionsupport;

import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.IBuildStepFactoryExtension;
import com.mobilesorcery.sdk.core.build.IBuildStepFactoryExtension.Position;
import com.mobilesorcery.sdk.core.build.LinkBuildStep;

public class ExtensionSupportBuildStepExtension implements
		IBuildStepFactoryExtension {

	public static final String ID = "com.mobilesorcery.sdk.extensionsupport";
	
	public ExtensionSupportBuildStepExtension() {
	}

	@Override
	public IBuildStepFactory createFactory() {
		return new ExtensionSupportBuildStep.Factory();
	}

	@Override
	public Pair<Position, String> getDefaultPosition() {
		return null;
	}

}
