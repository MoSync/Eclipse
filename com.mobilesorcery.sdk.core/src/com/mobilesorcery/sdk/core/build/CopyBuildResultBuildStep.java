package com.mobilesorcery.sdk.core.build;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.build.CopyBuildResultBuildStep.Factory;

public class CopyBuildResultBuildStep extends AbstractBuildStep {

	public static class Factory extends AbstractBuildStepFactory {

		private final static String DEFAULT_FOLDER_NAME = "ReleasePackages";
		private String folderName;
		private String cfgId;
		private Boolean isActive;

		@Override
		public IBuildStep create() {
			return new CopyBuildResultBuildStep(this);
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public String getName() {
			return MessageFormat.format("Copy to {0}", getFolderName());
		}

		public String getFolderName() {
			return folderName == null ? DEFAULT_FOLDER_NAME : folderName;
		}

		public void setFolderName(String folderName) {
			this.folderName = folderName;
		}

		public String getConfigurationId() {
			return cfgId;
		}

		public void setConfigurationId(String cfgId) {
			this.cfgId = cfgId;
		}

		public boolean isActive() {
			return isActive == null || isActive;
		}

		public void setActive(boolean isActive) {
			this.isActive = isActive;
		}

		@Override
		public boolean isDefault() {
			return isActive() && cfgId == null && folderName == null;
		}

		@Override
		public void load(IMemento memento) {
			cfgId = memento.getString("cfg");
			folderName = memento.getString("folder");
			isActive = memento.getBoolean("active");
		}

		@Override
		public void store(IMemento memento) {
			if (cfgId != null) {
				memento.putString("cfg", cfgId);
			}
			if (folderName != null) {
				memento.putString("folder", folderName);
			}
			if (isActive != null) {
				memento.putBoolean("active", isActive);
			}
		}

	}

	public static String ID = "copy.build.results";
	private final Factory prototype;

	public CopyBuildResultBuildStep(Factory prototype) {
		this.prototype = prototype;
		setName(prototype.getName());
	}

	@Override
	public boolean shouldAdd(IBuildSession session) {
		return session.doPack();
	}
	
	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult result,
			IProgressMonitor monitor) throws Exception {
		if (!result.getDependencyDelta().getAllDependees().isEmpty()) {
			if (prototype.isActive()) {
				String cfgId = getReleasePackageCfgId(project);
				if (configMatches(project, variant)) {
					IPath outputFolder = project.getWrappedProject()
							.getLocation().append(prototype.getFolderName());
					String cfgFolder = getCfgFolder(project, variant);
					if (cfgFolder != null) {
						outputFolder = outputFolder.append(cfgFolder);
					}
					outputFolder = outputFolder.append(MoSyncTool
							.toString(variant.getProfile()));
					Map<String, List<File>> buildArtifacts = result
							.getBuildResult();
					if (!buildArtifacts.isEmpty()) {
						getConsole().addMessage(
								"Copied to release package folder:");
					}
					for (Map.Entry<String, List<File>> buildArtifact : buildArtifacts
							.entrySet()) {
						for (File file : buildArtifact.getValue()) {
							File dest = outputFolder.append(file.getName())
									.toFile();
							Util.copy(new NullProgressMonitor(), file, dest,
									null);
							getConsole().addMessage("\t" + dest.getAbsolutePath());
						}
					}
					IFolder toRefresh = project.getWrappedProject().getFolder(
							prototype.getFolderName());
					MoSyncBuilder.ensureFolderIsMarkedDerived(toRefresh);
					MoSyncBuilder.refresh(toRefresh);
				} else {
					getConsole()
							.addMessage(
									MessageFormat
											.format("NOTE: This package was not copied the release package folder. Only builds for configuration {0} will be copied",
													cfgId));
				}
			}
		}
		return IBuildStep.CONTINUE;
	}

	private String getCfgFolder(MoSyncProject project, IBuildVariant variant) {
		String cfgId = variant.getConfigurationId();
		return getReleasePackageCfgId(project) == null ? cfgId : null;
	}

	private String getReleasePackageCfgId(MoSyncProject project) {
		String releasePackageCfgId = prototype.getConfigurationId();
		/*
		 * if (releasePackageCfgId == null) { // Fallback. SortedSet<String>
		 * cfgs = project
		 * .getBuildConfigurationsOfType(IBuildConfiguration.RELEASE_TYPE); if
		 * (!cfgs.isEmpty()) { releasePackageCfgId = cfgs.first(); } }
		 */
		return releasePackageCfgId;
	}

	private boolean configMatches(MoSyncProject project, IBuildVariant variant) {
		String cfgId = variant.getConfigurationId();
		String cfgToBuild = getReleasePackageCfgId(project);
		return cfgToBuild == null || Util.equals(cfgId, cfgToBuild);
	}

}
