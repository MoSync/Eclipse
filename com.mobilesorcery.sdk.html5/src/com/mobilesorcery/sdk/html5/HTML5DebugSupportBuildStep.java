package com.mobilesorcery.sdk.html5;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IFileTreeDiff;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.build.AbstractBuildStep;
import com.mobilesorcery.sdk.core.build.AbstractBuildStepFactory;
import com.mobilesorcery.sdk.core.build.BundleBuildStep;
import com.mobilesorcery.sdk.core.build.IBuildStep;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.hotreplace.FileRedefinable;
import com.mobilesorcery.sdk.internal.builder.IncrementalBuilderVisitor;
import com.mobilesorcery.sdk.internal.dependencies.DependencyManager;

public class HTML5DebugSupportBuildStep extends AbstractBuildStep {

	private final class InstrumentationBuilderVisitor extends
			IncrementalBuilderVisitor {
		private final class Rewriter {
			// TODO: Refactor this class into JSODDSupport; makes
			// it much easier.
			private final JSODDSupport op;

			private Rewriter(JSODDSupport op) {
				this.op = op;
			}

			public int rewrite(IResource resourceToInstrument,
					boolean persistToFile, boolean fetchRemotely, boolean delete)
					throws CoreException {
				IPath resourcePath = resourceToInstrument.getFullPath();
				resourcePath = resourcePath.removeFirstSegments(inputRoot
						.getFullPath().segmentCount());
				File outputFile = new File(outputRoot,
						resourcePath.toOSString());
				FileRedefinable result = null;
				if (delete) {
					result = op.delete(resourceToInstrument.getFullPath(),
							op.getBaseline());
				} else {
					outputFile.getParentFile().mkdirs();
					FileWriter output = null;
					try {
						output = persistToFile ? new FileWriter(outputFile)
								: null;
						MoSyncProject mosyncProject = MoSyncProject
								.create(getProject());
						if (fetchRemotely) {
							op.generateRemoteFetch(mosyncProject, output);
						} else {
							// This is a *build* op, so update the baseline.
							result = op.rewrite(
									resourceToInstrument.getFullPath(), output,
									op.getBaseline());
						}
					} catch (IOException e) {
						throw new CoreException(
								new Status(
										IStatus.ERROR,
										Html5Plugin.PLUGIN_ID,
										"Cannot instrument JavaScript for debugging",
										e));
					} finally {
						Util.safeClose(output);
					}
				}
				return result == null ? 0 : result.getMemSize();
			}
		}

		private final HashSet<IResource> resourcesToInstrument = new HashSet<IResource>();
		private final File outputRoot;
		private final IFolder inputRoot;
		private IFileTreeDiff diff;

		public InstrumentationBuilderVisitor(IFolder inputRoot, File outputRoot) {
			this.inputRoot = inputRoot;
			this.outputRoot = outputRoot;
		}

		@Override
		public boolean doesAffectBuild(IResource resource) {
			return JSODDSupport.isValidJavaScriptFile(resource.getLocation());
		}

		@Override
		public boolean visit(IResource resource) throws CoreException {
			boolean shouldVisitChildren = super.visit(resource);
			if (isBuildable(resource)) {
				resourcesToInstrument.add(resource);
			}
			return shouldVisitChildren;
		}

		@Override
		public void setDiff(IFileTreeDiff diff) throws CoreException {
			super.setDiff(diff);
			this.diff = diff;
		}

		public void instrument(IProgressMonitor monitor,
				DependencyManager<IResource> dependencies,
				IProcessConsole console) throws CoreException {

			final JSODDSupport op = Html5Plugin.getDefault().getJSODDSupport(
					project);

			IFileTreeDiff diff = this.diff;
			// If we've changed the IP addr, then rebuild it all...
			if (updateServerProps(op) || op.requiresFullBuild()) {
				diff = null;
				setDiff(diff);
			}

			op.applyDiff(diff);
			Set<IResource> instrumentThese = computeResourcesToRebuild(dependencies);
			Set<IResource> deleted = new HashSet<IResource>(
					Arrays.asList(getDeletedResources()));
			instrumentThese.addAll(deleted);
			boolean fetchRemotely = Html5Plugin.getDefault().shouldFetchRemotely();
			Rewriter rewriter = new Rewriter(op);

			if (!instrumentThese.isEmpty() && fetchRemotely) {
				IResource indexHtml = Html5Plugin.getDefault().getLocalFile(
						project, new Path("index.html"));
				if (indexHtml.getType() != IResource.FILE
						|| !indexHtml.exists()) {
					throw new CoreException(new Status(IStatus.ERROR,
							Html5Plugin.PLUGIN_ID,
							"Missing index.html, cannot build for debugging"));
				}
				rewriter.rewrite(indexHtml, true, true, false);
			}
			for (IResource instrumentThis : instrumentThese) {
				if (monitor.isCanceled()) {
					return;
				}
				dependencies.addDependency(instrumentThis,
						getResourceBundleLocation(project));
				long start = System.currentTimeMillis();
				boolean wasDeleted = deleted.contains(instrumentThis);
				int memoryConsumption = rewriter.rewrite(instrumentThis,
						!fetchRemotely, false, wasDeleted);
				long elapsed = System.currentTimeMillis() - start;
				console.addMessage(MessageFormat.format(
						"Instrumented {0} [{1}, {2}].", instrumentThis
								.getFullPath(),
						Util.elapsedTime((int) elapsed), wasDeleted ? "deleted"
								: Util.dataSize(memoryConsumption)));
			}
		}

		private boolean updateServerProps(JSODDSupport op) throws CoreException {
			try {
				IPath jsoddMetaData = getBuildState().getLocation().append(
						".jsodd");
				SectionedPropertiesFile jsoddPropsFile = SectionedPropertiesFile
						.create();
				if (jsoddMetaData.toFile().exists()) {
					jsoddPropsFile = SectionedPropertiesFile
							.parse(jsoddMetaData.toFile());
				}

				Map<String, String> jsoddProps = jsoddPropsFile
						.getDefaultSection().getEntriesAsMap();

				// TODO: Should NOT use default properties.
				String host = op.getDefaultProperties().get(
						JSODDSupport.SERVER_HOST_PROP);
				String port = op.getDefaultProperties().get(
						JSODDSupport.SERVER_PORT_PROP);

				String oldHost = jsoddProps.get(JSODDSupport.SERVER_HOST_PROP);
				String oldPort = jsoddProps.get(JSODDSupport.SERVER_PORT_PROP);

				Section defaultSection = jsoddPropsFile.getDefaultSection();
				defaultSection.getEntries().clear();
				defaultSection.addEntry(JSODDSupport.SERVER_HOST_PROP, host);
				defaultSection.addEntry(JSODDSupport.SERVER_PORT_PROP, port);

				jsoddPropsFile.write(jsoddMetaData.toFile());

				return !Util.equals(host, oldHost)
						|| !Util.equals(port, oldPort);
			} catch (IOException e) {
				CoreMoSyncPlugin.getDefault().log(e);
				return false;
			}

		}
	}

	public static class Factory extends AbstractBuildStepFactory {

		@Override
		public IBuildStep create() {
			return new HTML5DebugSupportBuildStep(this);
		}

		@Override
		public String getId() {
			return HTML5DebugSupportBuildStepExtension.ID;
		}

		@Override
		public String getName() {
			return "HTML5/JavaScript bundling";
		}

	}

	public HTML5DebugSupportBuildStep(Factory prototype) {
		setName(prototype.getName());
	}

	public IResource getResourceBundleLocation(IProject project) {
		return project.findMember(new Path("Resources/LocalFiles.bin"));
	}

	@Override
	public int incrementalBuild(MoSyncProject project, IBuildSession session,
			IBuildVariant variant, IFileTreeDiff diff, IBuildResult result,
			IProgressMonitor monitor) throws Exception {
		IProject wrappedProject = project.getWrappedProject();
		IPath inputRootPath = Html5Plugin.getHTML5Folder(wrappedProject);
		IFolder inputRoot = wrappedProject.getFolder(inputRootPath);
		if (inputRoot.exists()) {
			File outputResource = getResourceBundleLocation(wrappedProject)
					.getLocation().toFile();
			IPropertyOwner properties = MoSyncBuilder.getPropertyOwner(project,
					variant.getConfigurationId());
			if (PropertyUtil.getBoolean(properties,
					MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS)) {
				monitor.beginTask("Instrumenting JavaScript source files", 10);
				File outputRoot = MoSyncBuilder
						.getOutputPath(wrappedProject, variant)
						.append(inputRootPath).toFile();
				// Ok, do NOT copy files that we may want to instrument
				// Do not uncomment this until we have a strategy --
				// for various reasons (fixed file systems for example)
				// we keep a conservative view at this point.
				//if (!Html5Plugin.getDefault().shouldFetchRemotely()) {
					copyUninstrumentedFiles(monitor, inputRoot.getLocation()
							.toFile(), outputRoot);
				//}

				InstrumentationBuilderVisitor visitor = new InstrumentationBuilderVisitor(
						inputRoot, outputRoot);
				visitor.setProject(wrappedProject);
				visitor.setDiff(diff);
				DependencyManager<IResource> deps = getBuildState()
						.getDependencyManager();

				visitor.instrument(new SubProgressMonitor(monitor, 7), deps,
						getConsole());

				// TODO -- would prefer it not to always output there... Since
				// we'll get build problems for sure!
				BundleBuildStep.bundle(outputRoot, outputResource);
			} else {
				BundleBuildStep.bundle(inputRoot.getLocation().toFile(),
						outputResource);
			}
		}
		monitor.done();
		return CONTINUE;
	}

	private void copyUninstrumentedFiles(IProgressMonitor monitor,
			File inputRoot, File outputRoot) throws IOException {
		Util.copyDir(new SubProgressMonitor(monitor, 3), inputRoot, outputRoot,
				new FileFilter() {
					@Override
					public boolean accept(File file) {
						return !JSODDSupport.isValidJavaScriptFile(new Path(
								file.getAbsolutePath()));
					}
				});
	}

}
