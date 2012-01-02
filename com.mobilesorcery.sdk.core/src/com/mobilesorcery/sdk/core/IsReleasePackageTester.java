package com.mobilesorcery.sdk.core;

import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import com.mobilesorcery.sdk.core.build.BuildSequence;
import com.mobilesorcery.sdk.core.build.CopyBuildResultBuildStep;
import com.mobilesorcery.sdk.core.build.CopyBuildResultBuildStep.Factory;
import com.mobilesorcery.sdk.internal.MoSyncNatureTester;

public class IsReleasePackageTester extends MoSyncNatureTester {

	public IsReleasePackageTester() {
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		return isReleasePackageFolder(receiver);
	}

	public static boolean isReleasePackageFolder(Object receiver) {
		if (receiver instanceof IAdaptable) {
			receiver = ((IAdaptable) receiver).getAdapter(IResource.class);
		}
		// TODO: Get from project + createWorkingCopy method! Can't parse stuff
		// every time.
		MoSyncProject project = null;
		if (receiver instanceof IResource) {
			project = MoSyncProject.create(((IResource) receiver).getProject());
		}
		if (project != null && receiver instanceof IFolder) {
			IFolder folder = (IFolder) receiver;
			// Only top-level
			if (folder.getProjectRelativePath().segmentCount() == 1) {
				BuildSequence seq = new BuildSequence(project);
				List<CopyBuildResultBuildStep.Factory> factories = seq
						.getBuildStepFactories(CopyBuildResultBuildStep.Factory.class);
				for (CopyBuildResultBuildStep.Factory factory : factories) {
					String folderName = factory.getFolderName();
					if (folderName.equalsIgnoreCase(folder.getName())) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
