package com.mobilesorcery.sdk.html5;

import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.build.CompileBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.IBuildStepFactoryExtension;
import com.mobilesorcery.sdk.core.build.IBuildStepFactoryExtension.Position;

public class HTML5DebugSupportBuildStepExtension implements IBuildStepFactoryExtension {

	public final static String ID = "com.mobilesorcery.sdk.html5.debugsupport";

	private final static Pair<Position, String> POSITION = new Pair<Position, String>(Position.NONE, null);
	
	public HTML5DebugSupportBuildStepExtension() {
	}

	@Override
	public IBuildStepFactory createFactory() {
		return new HTML5DebugSupportBuildStep.Factory();
	}

	@Override
	public Pair<Position, String> getDefaultPosition() {
		return POSITION;
	}

}
