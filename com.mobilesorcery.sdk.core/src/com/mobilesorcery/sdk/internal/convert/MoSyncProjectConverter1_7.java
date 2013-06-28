package com.mobilesorcery.sdk.internal.convert;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncProject.IConverter;
import com.mobilesorcery.sdk.core.Version;

public class MoSyncProjectConverter1_7 implements IConverter {

	public final static Version VERSION = new Version("1.7");

	private static IConverter instance = new MoSyncProjectConverter1_7();

	public static IConverter getInstance() {
		return instance;
	}

	@Override
	public void convert(MoSyncProject project) throws CoreException {
		if (VERSION.isNewer(project.getFormatVersion())) {
			for (String cfg : project.getBuildConfigurations()) {
				IPropertyOwner cfgProperties = MoSyncBuilder.getPropertyOwner(project, cfg);
				cfgProperties.setProperty(MoSyncBuilder.ADDITIONAL_NATIVE_INCLUDE_PATHS,
						cfgProperties.getProperty(MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS));
			}
			// Minor hack -- we want to adjust the native paths.
			String outputType = project.getOutputType();
			project.forceOutputType(MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE);
			project.forceOutputType(outputType);
		}
	}
}
