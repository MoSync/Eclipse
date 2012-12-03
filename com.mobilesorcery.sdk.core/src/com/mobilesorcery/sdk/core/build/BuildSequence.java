package com.mobilesorcery.sdk.core.build;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

import com.mobilesorcery.sdk.core.Cache;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.build.IBuildStepFactoryExtension.Position;

public class BuildSequence implements IBuildSequence {

	private final MoSyncProject project;
	private ArrayList<IBuildStepFactory> buildStepFactories = new ArrayList<IBuildStepFactory>();
	private ArrayList<IBuildStep> buildSteps;
	
	private final static String[] BASE_SEQUENCE = new String[] { 
			ResourceBuildStep.ID, CompileBuildStep.ID,
			LinkBuildStep.ID, PackBuildStep.ID,
			CopyBuildResultBuildStep.ID
		};

	// Can leak max 8 prjs.
	private static Cache<MoSyncProject, BuildSequence> cache = new Cache<MoSyncProject, BuildSequence> (8);

	public BuildSequence(MoSyncProject project) {
		this.project = project;
		load();
	}

	/**
	 * Returns a cached copy the build sequence for a specific project.
	 * @param project
	 * @return
	 */
	public static BuildSequence getCached(MoSyncProject project) {
		// TODO: cache all projects, dispose when project is disposed.
		if (project.isDisposed()) {
			clearCache(project);
			return null;
		}
		BuildSequence cached = cache.get(project);
		if (cached == null) {
			cached = new BuildSequence(project);
			cache.put(project, cached);
		}
		return cached;
	}

	public static void clearCache(MoSyncProject project) {
		cache.remove(project);
	}

	private void initDefaultFactories() {
		for (int i = 0; i < BASE_SEQUENCE.length; i++) {
			addDefaultFactory(i, BASE_SEQUENCE[i]);
		}
		for (String factoryId : CoreMoSyncPlugin.getDefault().getBuildStepFactories()) {
			IBuildStepFactoryExtension ext = 
					CoreMoSyncPlugin.getDefault().getBuildStepFactoryExtension(factoryId);
			if (ext != null) {
				Pair<Position, String> pos = ext.getDefaultPosition();
				if (pos != null && pos.first != Position.NONE && pos.second != null) {
					// We only allow positions relative to intrinsic factories.
					int ix = getIndex(pos.second);
					addDefaultFactory(ix + (pos.first == Position.BEFORE ? 0 : 1), 
							factoryId);
				}	
			}
		}
	}
	
	private int getIndex(String id) {
		for (int i = 0; i < buildStepFactories.size(); i++) {
			if (buildStepFactories.get(i).getId().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	private boolean isDefaultSequence() {
		int baseSequenceIx = 0;
		for (IBuildStepFactory buildStepFactory : buildStepFactories) {
			if (!buildStepFactory.isDefault()) {
				return false;
			}
			String id = buildStepFactory.getId();
			IBuildStepFactoryExtension ext = CoreMoSyncPlugin.getDefault().
					getBuildStepFactoryExtension(id);
			if (ext == null) {
				// Then we'd better be at the proper base sequence position!
				if (!BASE_SEQUENCE[baseSequenceIx].equals(id)) {
					return false;
				}
				baseSequenceIx++;
			} else {
				Pair<Position, String> pos = ext.getDefaultPosition();
				if (pos == null || pos.first == Position.NONE || pos.second == null) {
					return false;
				}
				boolean correctPositionBefore = pos.first == Position.BEFORE &&
						pos.second.equals(BASE_SEQUENCE[baseSequenceIx]);
				boolean correctPositionAfter = baseSequenceIx > 0 &&
						pos.first == Position.AFTER &&
						pos.second.equals(BASE_SEQUENCE[baseSequenceIx - 1]);
				if (!correctPositionAfter && !correctPositionBefore) {
					return false;
				}
			}
		}
		
		return baseSequenceIx == BASE_SEQUENCE.length;
	}

	private void addDefaultFactory(int ix, String id) {
		IBuildStepFactory factory = CoreMoSyncPlugin.getDefault().createBuildStepFactory(id);
		buildStepFactories.add(ix, factory);
	}

	private void load() {
		FileReader input = null;
        try {
        	IPath buildFilePath = getBuildFilePath();
            if (buildFilePath.toFile().exists()) {
            	buildStepFactories.clear();
            	input = new FileReader(buildFilePath.toFile());
                XMLMemento memento = XMLMemento.createReadRoot(input);
                IMemento[] buildSteps = memento.getChildren("buildStep");
                for (int i = 0; i < buildSteps.length; i++) {
                	IMemento buildStep = buildSteps[i];
                	String buildStepId = buildStep.getString("type");
                	IBuildStepFactory factory = CoreMoSyncPlugin.getDefault().createBuildStepFactory(buildStepId);
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

	public void save() throws IOException {
		FileWriter output = null;
		try {
	    	File buildFilePath = getBuildFilePath().toFile();
	    	if (isDefaultSequence()) {
    			buildFilePath.delete();
	    	} else {
                XMLMemento memento = XMLMemento.createWriteRoot("buildSequence");
                for (IBuildStepFactory buildStepFactory : buildStepFactories) {
                	IMemento buildStepMemento = memento.createChild("buildStep");
                	buildStepMemento.putString("type", buildStepFactory.getId());
                	buildStepFactory.store(buildStepMemento);
                }

            	output = new FileWriter(buildFilePath);
                memento.save(output);
	    	}
		} finally {
			Util.safeClose(output);
		}

	}

	private IPath getBuildFilePath() {
		return project.getWrappedProject().getLocation().append(".build");
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

	/**
	 * Returns the set of build step factories. This method
	 * returns a copy of the internal list, so to edit the build
	 * sequence, use the {@link #apply(List)} method.
	 * @return
	 */
	public List<IBuildStepFactory> getBuildStepFactories() {
		return new ArrayList<IBuildStepFactory>(this.buildStepFactories);
	}

	/**
	 * Will apply a list of build step factories to this sequence
	 * and save it.
	 * @param buildStepFactories
	 * @throws IOException
	 */
	public void apply(List<IBuildStepFactory> buildStepFactories) throws IOException {
		this.buildSteps = null;
		this.buildStepFactories = new ArrayList<IBuildStepFactory>(buildStepFactories);
		save();
	}


	/**
	 * Sets this build sequence to the default sequence.
	 */
	public void setToDefault() {
		buildStepFactories.clear();
		initDefaultFactories();
	}

	public <T extends IBuildStepFactory> List<T> getBuildStepFactories(Class<T> clazz) {
		ArrayList<T> result = new ArrayList<T>();
		for (IBuildStepFactory factory : buildStepFactories) {
			if (factory.getClass().isAssignableFrom(clazz)) {
				result.add((T) factory);
			}
		}
		return result;
	}
}
