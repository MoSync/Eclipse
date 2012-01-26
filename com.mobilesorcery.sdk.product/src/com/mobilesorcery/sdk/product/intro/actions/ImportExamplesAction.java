package com.mobilesorcery.sdk.product.intro.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;
import org.eclipse.ui.progress.WorkbenchJob;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section;
import com.mobilesorcery.sdk.ui.ImportProjectsRunnable;

/**
 * An action that imports examples into the current workspace, and closes the
 * welcome screen.
 *
 * @author Mattias F
 */
public class ImportExamplesAction implements IIntroAction {
	/**
	 * Represents a job that imports all examples into the current workspace.
	 *
	 * @author Mattias
	 */
	private final class ImportExamplesProjectJob extends WorkbenchJob {
		private String type;

		private ImportExamplesProjectJob(String name) {
			super(name);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			File examplesDir = MoSyncTool.getDefault()
					.getMoSyncExamplesDirectory().toFile();
			File exampleManifestFile = new File(examplesDir, "examples.list");

			if (!exampleManifestFile.exists()) {
				return Status.OK_STATUS;
			}

			try {
				String type = getType();
				/* Parse examples list */
				Map<String, String> exampleMap = parseExampleManifest(type, exampleManifestFile);

				/* Do not import examples that we already have in the workspace */
				removeExistingExamples(exampleMap);

				/*
				 * If there are any examples left to import, add them to the
				 * example lists
				 */
				ArrayList<File> projectFiles = new ArrayList<File>();
				ArrayList<String> preferredProjectNames = new ArrayList<String>();
				addExamples(type, exampleMap, projectFiles, preferredProjectNames);

				/* Create the import job */
				File projectFileArray[] = projectFiles.toArray(new File[0]);
				String preferredProjectNameArray[] = preferredProjectNames
						.toArray(new String[0]);
				ImportProjectsRunnable importer = new ImportProjectsRunnable(
						projectFileArray,
						preferredProjectNameArray,
						ImportProjectsRunnable.COPY_ALL_FILES
								| ImportProjectsRunnable.USE_NEW_PROJECT_IF_AVAILABLE);
				importer.useNewCopyFilter();
				Job job = importer.createJob(true);

				closeWelcomeWhenDone(job);

			} catch (Exception e) {
				return new Status(IStatus.ERROR,
						"com.mobilesorcery.sdk.product",
						"Could not import examples", e);
			}

			return Status.OK_STATUS;
		}

		/**
		 * Parses the example manifest file.
		 * @param type
		 *
		 * @param exampleManifestFile
		 * @return A map from example names to their path.
		 * @throws IOException
		 */
		private Map<String, String> parseExampleManifest(
				String type, File exampleManifestFile) throws IOException {
			SectionedPropertiesFile exampleManifest = SectionedPropertiesFile
					.parse(exampleManifestFile);
			Section exampleSection = exampleManifest
					.getFirstSection(type);
			if (exampleSection == null) {
				throw new IOException("No examples of type: " + type);
			}
			return exampleSection.getEntriesAsMap();
		}

		/**
		 * Removes the examples that already exists in the current workspace,
		 * from the list of examples to be imported.
		 *
		 * @param exampleMap
		 *            Map of example names to directories.
		 */
		private void removeExistingExamples(Map<String, String> exampleMap) {
			IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
			IProject[] alreadyImportedProjects = wsRoot.getProjects();
			for (int i = 0; i < alreadyImportedProjects.length; i++) {
				IProject project = alreadyImportedProjects[i];
				exampleMap.remove(project.getName());
			}
		}

		/**
		 * Adds the examples with full paths to the given lists.
		 *
		 * @param exampleMap
		 *            Map of example names to directories.
		 * @param projectFiles
		 *            Output list of absolute paths to example projects.
		 * @param preferredProjectNames
		 *            Output list of preferred project names.
		 */
		private void addExamples(String type, Map<String, String> exampleMap,
				ArrayList<File> projectFiles,
				ArrayList<String> preferredProjectNames) {
			if (exampleMap.isEmpty()) {
				return;
			}

			for (String newExample : exampleMap.keySet()) {
				String newExampleDir = exampleMap.get(newExample);
				IPath newExampleFullDir = MoSyncTool.getDefault()
						.getMoSyncExamplesDirectory().append(type).append(newExampleDir);
				projectFiles.add(newExampleFullDir.append(
						MoSyncProject.MOSYNC_PROJECT_META_DATA_FILENAME)
						.toFile());
				preferredProjectNames.add(newExample);
			}
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getType() {
			return type == null ? "cpp" : type;
		}
	}

	/**
	 * Makes the given job close the welcome screen when it is done.
	 *
	 * @param job
	 *            The job that should close the welcome screen upon completion.
	 */
	private void closeWelcomeWhenDone(Job job) {
		/* Close the welcome screen. */
		final IIntroManager im = PlatformUI.getWorkbench().getIntroManager();
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				im.getIntro().getIntroSite().getShell().getDisplay()
						.asyncExec(new Runnable() {
							@Override
							public void run() {
								IIntroPart part = im.getIntro();
								im.closeIntro(part);
							}
						});
			}
		});
	}

	/**
	 * @see IIntroAction.run.
	 */
	@Override
	public void run(IIntroSite site, Properties params) {
		ImportExamplesProjectJob job = new ImportExamplesProjectJob(
				"Importing example projects");
		String type = params.getProperty("type");
		job.setType(type);
		job.setUser(true);
		job.schedule();
	}
}
