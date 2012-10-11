package com.mobilesorcery.sdk.molint;

import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.build.CompileBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.IBuildStepFactoryExtension;

public class MolintBuildStepExtension implements IBuildStepFactoryExtension {

	public final static String ID = "com.mobilesorcery.sdk.molint";
	
	private final static Pair<Position, String> POSITION = new Pair<Position, String>(Position.AFTER, CompileBuildStep.ID);
	
	public MolintBuildStepExtension() {
	}

	@Override
	public IBuildStepFactory createFactory() {
		return new MolintBuildStep.Factory();
	}

	@Override
	public Pair<Position, String> getDefaultPosition() {
		return POSITION;
	}

}
