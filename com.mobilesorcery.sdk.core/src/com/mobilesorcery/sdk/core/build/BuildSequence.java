package com.mobilesorcery.sdk.core.build;

import java.io.FileReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;

public class BuildSequence implements IBuildSequence {

	private MoSyncProject project;
	private ArrayList<IBuildStepFactory> buildStepFactories = new ArrayList<IBuildStepFactory>();
	private ArrayList<IBuildStep> buildSteps;

	public BuildSequence(MoSyncProject project) {
		this.project = project;
		load();
	}
	
	private void initDefaultFactories() {
		addStandardFactory(CompileBuildStep.ID);
		addStandardFactory(ResourceBuildStep.ID);
		addStandardFactory(LinkBuildStep.ID);
		addStandardFactory(PackBuildStep.ID);
	}
	
	private void addStandardFactory(String id) {
		IBuildStepFactory factory = createFactory(id);
		buildStepFactories.add(factory);
	}

	private IBuildStepFactory createFactory(String id) {
		// TODO: We may want to allow for extension points
		if (CompileBuildStep.ID.equals(id)) {
			return new CompileBuildStep.Factory();
		} else if (ResourceBuildStep.ID.equals(id)) {
			return new ResourceBuildStep.Factory();
		} else if (LinkBuildStep.ID.equals(id)) {
			return new LinkBuildStep.Factory();
		} else if (PackBuildStep.ID.equals(id)) {
			return new PackBuildStep.Factory();
		} else if (CommandLineBuildStep.ID.equals(id)) {
			return new CommandLineBuildStep.Factory();
		}
		
		return null;
	}

	private void load() {
		FileReader input = null;
        try {
        	IPath buildFilePath = project.getWrappedProject().getLocation().append(".build");
            if (buildFilePath.toFile().exists()) {
            	buildStepFactories.clear();
            	input = new FileReader(buildFilePath.toFile());
                XMLMemento memento = XMLMemento.createReadRoot(input);
                IMemento buildSequence = memento.getChild("buildSequence");
                IMemento[] buildSteps = buildSequence.getChildren("buildStep");
                for (int i = 0; i < buildSteps.length; i++) {
                	IMemento buildStep = buildSteps[i];
                	String buildStepId = buildStep.getString("type");
                	IBuildStepFactory factory = createFactory(buildStepId);
                	if (factory != null) {
                		factory.load(buildStep);
                		this.buildStepFactories.add(factory);
                	} else {
                		CoreMoSyncPlugin.getDefault().getLog().log(new Status(IStatus.INFO, CoreMoSyncPlugin.PLUGIN_ID, MessageFormat.format("Build step factory {0} missing", buildStepId)));
                	}
                }
            } else {
            	initDefaultFactories();
            }
        } catch (Exception e) {
        	CoreMoSyncPlugin.getDefault().log(e);
        } finally {
        	Util.safeClose(input);
        }
	}

	@Override
	public List<IBuildStep> getBuildSteps(IBuildSession session) {
		if (this.buildSteps == null) {
			buildSteps = new ArrayList<IBuildStep>();
			for (IBuildStepFactory factory : this.buildStepFactories) {
				IBuildStep buildStep = factory.create();
				if (buildStep != null && buildStep.shouldAdd(session)) {
					buildSteps.add(buildStep);
				}
			}
		}
		
		return buildSteps;
	}

	public void assertValid(IBuildSession session) throws CoreException {
		List<IBuildStep> buildSteps = getBuildSteps(session);
		HashSet<String> buildStepsBefore = new HashSet<String>();
		for (IBuildStep buildStep : buildSteps) {
			String[] dependees = buildStep.getDependees();
			if (dependees != null) {
					for (String dependee : buildStep.getDependees()) {
					if (!buildStepsBefore.contains(dependee)) {
						throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, 
								MessageFormat.format("The build step {0} requires this build step with this id to be performed prior to it: {1}", buildStep.getName(), dependee)));
					}
				}
			}
			buildStepsBefore.add(buildStep.getId());
		}
	}
}
