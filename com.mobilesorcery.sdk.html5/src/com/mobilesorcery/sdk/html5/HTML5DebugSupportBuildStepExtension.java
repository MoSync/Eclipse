package com.mobilesorcery.sdk.html5;

import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.IBuildStepFactoryExtension;

public class HTML5DebugSupportBuildStepExtension implements IBuildStepFactoryExtension {

	public final static String ID = "com.mobilesorcery.sdk.html5.debugsupport";

	public HTML5DebugSupportBuildStepExtension() {
	}

	@Override
	public IBuildStepFactory createFactory() {
		return new HTML5DebugSupportBuildStep.Factory();
	}

}
