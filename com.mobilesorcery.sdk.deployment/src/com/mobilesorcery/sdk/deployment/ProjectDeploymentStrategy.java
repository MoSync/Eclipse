package com.mobilesorcery.sdk.deployment;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.filter.CompositeDeviceFilter;

public class ProjectDeploymentStrategy {

    private static final String MOSYNC_DEPLOYMENT_META_DATA_FILENAME = MoSyncProject.MOSYNC_PROJECT_META_DATA_FILENAME + ".deploy";

    private MoSyncProject project;

    private HashMap<IDeploymentStrategy, IDeviceFilter> profileAssignments = new HashMap<IDeploymentStrategy, IDeviceFilter>();

    private ArrayList<IDeploymentStrategy> strategies = new ArrayList<IDeploymentStrategy>();

    private File deploymentFile;

    public ProjectDeploymentStrategy(MoSyncProject project, File deploymentFile) {
        this.project = project;
        this.deploymentFile = deploymentFile;
        initFromMetaFile();
    }

    public void addStrategy(IDeploymentStrategy strategy) {
        strategies.add(strategy);
    }

    public void removeStrategy(IDeploymentStrategy strategy) {
        strategies.remove(strategy);
    }

    public void assignProfiles(IDeploymentStrategy strategy, IDeviceFilter filter) {
        if (!strategies.contains(strategy)) {
            throw new IllegalStateException();
        }
        if (filter != null) {
            profileAssignments.put(strategy, filter);
        } else {
            profileAssignments.remove(strategy);
        }
    }
    
    public IDeviceFilter getAssignedProfiles(IDeploymentStrategy strategy) {
        return profileAssignments.get(strategy);
    }

    private void initFromMetaFile() {
        File deploymentFile = getDeploymentFile();

        strategies = new ArrayList<IDeploymentStrategy>();
        profileAssignments = new HashMap<IDeploymentStrategy, IDeviceFilter>();
        
        if (deploymentFile.exists()) {
            Reader input = null;
            try {
                input = new FileReader(deploymentFile);
                XMLMemento memento = XMLMemento.createReadRoot(input);
                IMemento[] strategyMementos = memento.getChildren("strategy");
                for (int i = 0; i < strategyMementos.length; i++) {
                    IMemento strategyMemento = strategyMementos[i];
                    String type = strategyMemento.getString("type");
                    IMemento strategyPropertiesMemento = strategyMemento.getChild("properties");
                    IDeploymentStrategyFactory factory = DeploymentPlugin.getDefault().getDeploymentStrategyFactory(type);
                    if (factory != null) {
                        IDeploymentStrategy strategy = factory.create(strategyPropertiesMemento);
                        addStrategy(strategy);
                        IMemento filterMemento = strategyMemento.getChild("filter");
                        CompositeDeviceFilter filter = filterMemento == null ? null : CompositeDeviceFilter.read(filterMemento);
                        assignProfiles(strategy, filter);
                    }
                }
            } catch (Exception e) {
                CoreMoSyncPlugin.getDefault().log(e);
            } finally {
                Util.safeClose(input);
            }
        }
    }

    public void saveToMetaFile() throws Exception {
        File deploymentFile = getDeploymentFile();
        XMLMemento memento = XMLMemento.createWriteRoot("deployment");
        Writer output = new FileWriter(deploymentFile);

        try {
            for (IDeploymentStrategy strategy : strategies) {
                IMemento strategyMemento = memento.createChild("strategy");
                String type = strategy.getFactoryId();
                IDeploymentStrategyFactory factory = DeploymentPlugin.getDefault().getDeploymentStrategyFactory(type);
                if (factory != null) {
                    strategyMemento.putString("type", type);
                    IMemento strategyPropertiesMemento = strategyMemento.createChild("properties");
                    factory.store(strategyPropertiesMemento, strategy);
                }

                IDeviceFilter filter = getAssignedProfiles(strategy);
                if (filter != null) {
                    IMemento filterMemento = strategyMemento.createChild("filter");
                    filter.saveState(filterMemento);
                }
            }

            memento.save(output);
        } finally {
            Util.safeClose(output);
        }
    }

    private File getDeploymentFile() {
        if (deploymentFile == null) {
            IPath deploymentPath = project.getWrappedProject().getLocation().append(MOSYNC_DEPLOYMENT_META_DATA_FILENAME);
            return deploymentPath.toFile();
        } else {
            return deploymentFile;
        }
    }

    public List<IDeploymentStrategy> getStrategies() {
        return Collections.unmodifiableList(strategies);
    }

}
