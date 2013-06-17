/*  Copyright (C) 2013 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.core.build;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IPackager;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;

public class NativeLibBuildStep extends AbstractBuildStep {

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new NativeLibBuildStep(this);
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public String getName() {
			return "Build using native tools";
		}
	}
	
	public static final String ID = "native";

	public NativeLibBuildStep(Factory factory) {
		setId(factory.getId());
		setName(factory.getName());
	}

	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult result,
			IProgressMonitor monitor) throws Exception {
		boolean setNative = MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE.equals(project.getProperty(MoSyncBuilder.OUTPUT_TYPE));
		if (setNative && project.getProfileManagerType() != MoSyncTool.DEFAULT_PROFILE_TYPE) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
					"Native compilation is only supported for projects using the platform profile database."));
		}
		if (!isOutputType(project, variant, MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE)) {
			getConsole().addMessage("Project does not use native compilation");
			if (setNative) {
				getConsole().addMessage(IProcessConsole.ERR, "The platform does not support native compilation");
			}
			return CONTINUE;
		}
		
		build(project, session, variant, result);
		
		return IBuildStep.CONTINUE;
	}
	
	public void build(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IBuildResult result) throws Exception {
		assertConfigId(variant);
		
		IProfile profile = variant.getProfile();
		IPackager packager = profile.getPackager();
		
		packager.buildNative(project, session, variant, result);
	}

	private static void assertConfigId(IBuildVariant variant) throws CoreException {
		String validatedConfig = validateConfigId(variant);
		if (!Util.isEmpty(validatedConfig)) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID,
				validatedConfig));
		}
	}
	
	public static String validateConfigId(IBuildVariant variant) {
		String configId = variant.getConfigurationId();
		for (int i = 0; i < configId.length(); i++) {
			char ch = configId.charAt(i);
			if (!Character.isLetter(ch) && !Character.isDigit(ch) &&
				ch != '-' && ch !=  '_') {
				return MessageFormat.format(
						"Invalid configuration name: {0}. Only letters, digits, hyphen and underscored allowed.",
						configId);
			}
		}
		return null;
	}
}
