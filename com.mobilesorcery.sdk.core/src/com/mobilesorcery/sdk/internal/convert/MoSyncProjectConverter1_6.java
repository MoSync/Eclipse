package com.mobilesorcery.sdk.internal.convert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncProject.IConverter;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.CompileBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStepFactory;
import com.mobilesorcery.sdk.core.build.NativeLibBuildStep;

public class MoSyncProjectConverter1_6 implements IConverter {

	public final static Version VERSION = new Version("1.6");
	
	private static IConverter instance = new MoSyncProjectConverter1_6();

	public static MoSyncProject.IConverter getInstance() {
		return instance;
	}
	
	@Override
	public void convert(MoSyncProject project) throws CoreException {
		if (VERSION.isNewer(project.getFormatVersion())) {
			BuildSequence sequence = new BuildSequence(project);
			if (sequence.getBuildStepFactories(NativeLibBuildStep.Factory.class).isEmpty()) {
				List<IBuildStepFactory> factories = sequence.getBuildStepFactories();
				ArrayList<IBuildStepFactory> newFactories = new ArrayList<IBuildStepFactory>();
				for (IBuildStepFactory factory : factories) {
					if (CompileBuildStep.ID.equals(factory.getId())) {
						newFactories.add(new NativeLibBuildStep.Factory());
					}
					newFactories.add(factory);
				}
				try {
					sequence.apply(newFactories);
				} catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, e.getMessage(), e));
				}
			}
		}
	}

}
