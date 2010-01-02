/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.ui.internal.properties;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyOwnerWorkingCopy;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.IPropertyOwner.IWorkingCopy;
import com.mobilesorcery.sdk.ui.BuildConfigurationsContentProvider;
import com.mobilesorcery.sdk.ui.BuildConfigurationsLabelProvider;
import com.mobilesorcery.sdk.ui.UIUtils;

public class BuildSettingsPropertyPage extends PropertyPage implements PropertyChangeListener {

	public class BuildConfigurationChangedListener implements
			ISelectionChangedListener {

		private boolean active = true;

		public void selectionChanged(SelectionChangedEvent event) {
			if (!active) {
				return;
			}
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			String id = (String) selection.getFirstElement();
			updateCurrentBuildConfiguration();
			changeConfiguration(id);
			initUI();
		}

		public void setActive(boolean active) {
			this.active = active;
		}
	}

	private final static String[] INCREMENTAL_BUILD_STRATEGY_OPTIONS = new String[] { "Use GCC -MF Option", "Always perform FULL build" };
	
    private class UpdateListener implements Listener {
        private boolean active;

        public void handleEvent(Event event) {
            if (!active) {
                return;
            }

            updateUI(event);
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    private Text additionalIncludePathsText;
    private Button ignoreDefaultIncludePaths;
    private Text additionalLibraryPathsText;
    private Button ignoreDefaultLibraryPaths;
    private Button deadCodeElim;
    private Text gccFlags;
    private Button applicationProjectType;
    private Button libraryProjectType;
    private Text additionalLibrariesText;
    private Button ignoreDefaultLibraries;
    private Text extraRes;
    private Text extraLink;
    private Text libOutputPath;
	private Text appOutputPath;
    private UpdateListener listener;
    private String currentOutputPath;
    private Button gccWall;
    private Button gccWerror;
    private Button gccWextra;
    private Combo incrementalBuildStrategy;
	private Button useDebugRuntimes;
	private ComboViewer buildConfigurations;
	private String currentConfigId;
	private BuildConfigurationChangedListener buildConfigurationListener;
	private HashMap<Object, IWorkingCopy> workingCopies = new HashMap<Object, IWorkingCopy>();
	private Composite main;
	private Composite placeHolder;
	
	private Text excludeFiles;
	private Text heapSize;
	private Text stackSize;
	private Text dataSize;

    protected Control createContents(Composite parent) {
    	placeHolder = new Composite(parent, SWT.NONE);
    	FillLayout placeHolderLayout = new FillLayout();
    	placeHolderLayout.marginHeight = 0;
    	placeHolderLayout.marginWidth = 0;
    	placeHolder.setLayout(placeHolderLayout);
    	fillPlaceHolder(placeHolder);
    	getProject().addPropertyChangeListener(this);
    	return placeHolder;
    }
        
    protected void fillPlaceHolder(Composite placeHolder) {
        main = new Composite(placeHolder, SWT.NONE);
        main.setLayout(new GridLayout(1, false));

        Group projectType = new Group(main, SWT.NONE);
        projectType.setLayout(new GridLayout(2, false));
        projectType.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        projectType.setText("Project &Type");

        applicationProjectType = new Button(projectType, SWT.RADIO);
        applicationProjectType.setText("&Application");

        libraryProjectType = new Button(projectType, SWT.RADIO);
        libraryProjectType.setText("&Library");

        Group buildStrategy = new Group(main, SWT.NONE);
        buildStrategy.setLayout(new GridLayout(2, false));
        buildStrategy.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        buildStrategy.setText("Incremental Build Strateg&y");
        
        Label incrementalBuildStrategyLabel = new Label(buildStrategy, SWT.NONE);
        incrementalBuildStrategyLabel.setText("Incremental Build Strategy To Use:");
        
        incrementalBuildStrategy = new Combo(buildStrategy, SWT.READ_ONLY);
        incrementalBuildStrategy.setItems(INCREMENTAL_BUILD_STRATEGY_OPTIONS);
        incrementalBuildStrategy.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        
        boolean hasConfigurations = getProject().isBuildConfigurationsSupported();
        Group configurationsGroup = null;
        if (hasConfigurations) {
        	configurationsGroup = new Group(main, SWT.NONE);
        	configurationsGroup.setLayout(new GridLayout(1, false));
        	configurationsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        	configurationsGroup.setText("Build Configuration");
        	Composite configurationsComposite = new Composite(configurationsGroup, SWT.NONE);
        	configurationsComposite.setLayout(new GridLayout(2, true));
        	configurationsComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        	Label configurationsLabel = new Label(configurationsComposite, SWT.NONE);
        	configurationsLabel.setText("Configuratio&n:");
        	buildConfigurations = new ComboViewer(configurationsComposite, SWT.READ_ONLY);
        	buildConfigurations.setContentProvider(new BuildConfigurationsContentProvider(getProject()));
        	buildConfigurations.setLabelProvider(new BuildConfigurationsLabelProvider(getProject()));
        	buildConfigurations.setInput(getProject());
        	buildConfigurations.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        	
        	buildConfigurationListener = new BuildConfigurationChangedListener();
        	buildConfigurations.addSelectionChangedListener(buildConfigurationListener);
        }
 
		Composite configurationParent = hasConfigurations ? configurationsGroup : main;

        TabFolder tabs = new TabFolder(configurationParent, SWT.NONE);
        tabs.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        createTabs(tabs);

        listener = new UpdateListener();

        applicationProjectType.addListener(SWT.Selection, listener);
        libraryProjectType.addListener(SWT.Selection, listener);
        applicationProjectType.addListener(SWT.Selection, listener);
        additionalIncludePathsText.addListener(SWT.Modify, listener);
        additionalLibrariesText.addListener(SWT.Modify, listener);
        additionalLibraryPathsText.addListener(SWT.Modify, listener);
        libOutputPath.addListener(SWT.Modify, listener);
        appOutputPath.addListener(SWT.Modify, listener);
        
        changeConfiguration(getProject().getActiveBuildConfiguration());
    	if (getProject().isBuildConfigurationsSupported()) {
    		IBuildConfiguration cfg = getProject().getActiveBuildConfiguration();
    		if (cfg != null) {
    			buildConfigurationListener.setActive(false);
    			buildConfigurations.setSelection(new StructuredSelection(cfg.getId()));
    			buildConfigurationListener.setActive(true);
    		}
    	}
    	
        initUI();
        placeHolder.layout();
    }
    
    private void createTabs(TabFolder tabs) {
    	createPathsTab(tabs);
    	createCompilerOptionsTab(tabs);
    	createPackagingTab(tabs);
	}

	private void createCompilerOptionsTab(TabFolder tabs) {
        TabItem compilerFlagsTab = new TabItem(tabs, SWT.NONE);
        compilerFlagsTab.setText("Compiler &Flags");
        
        Composite compilerFlags = new Composite(tabs, SWT.NONE);
        compilerFlagsTab.setControl(compilerFlags);
        
        compilerFlags.setLayout(new GridLayout(2, false));
        compilerFlags.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        deadCodeElim = new Button(compilerFlags, SWT.CHECK);
        deadCodeElim.setText("Activate Dead Code &Elimination");
        deadCodeElim.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        Label gccFlagsLabel = new Label(compilerFlags, SWT.NONE);
        gccFlagsLabel.setText("Additional GCC &Switches:");
        gccFlags = new Text(compilerFlags, SWT.BORDER | SWT.SINGLE);
        gccFlags.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label extraResLabel = new Label(compilerFlags, SWT.NONE);
        extraResLabel.setText("Extra &Resource Compiler Switches:");
        extraRes = new Text(compilerFlags, SWT.BORDER | SWT.SINGLE);
        extraRes.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label extraLinkLabel = new Label(compilerFlags, SWT.NONE);
        extraLinkLabel.setText("Extra &Linker Switches:");
        extraLink = new Text(compilerFlags, SWT.BORDER | SWT.SINGLE);
        extraLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Label gccWarningsLabel = new Label(compilerFlags, SWT.NONE);
        gccWarningsLabel.setText("GCC &Warnings:");
        gccWarningsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 3));
        
        gccWerror = new Button(compilerFlags, SWT.CHECK);
        gccWerror.setText("Warnings as &Errors");
        gccWall = new Button(compilerFlags, SWT.CHECK);
        gccWall.setText("&All Warnings");
        gccWextra = new Button(compilerFlags, SWT.CHECK);
        gccWextra.setText("E&xtra Warnings");
        
        Group memoryGroup = new Group(compilerFlags, SWT.NONE);
        memoryGroup.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
        memoryGroup.setText("&Memory Settings");
        memoryGroup.setLayout(new GridLayout(3, false));
        
        Label heapSizeLabel = new Label(memoryGroup, SWT.NONE);
        heapSizeLabel.setText("&Heap Size:");
        
        heapSize = new Text(memoryGroup, SWT.BORDER | SWT.SINGLE); 
        heapSize.setLayoutData(new GridData(UIUtils.getDefaultFieldSize() / 2, SWT.DEFAULT));
        Label kb1 = new Label(memoryGroup, SWT.NONE);
        kb1.setText("kb");
        
        Label stackSizeLabel = new Label(memoryGroup, SWT.NONE);
        stackSizeLabel.setText("&Stack Size:");
        
        stackSize = new Text(memoryGroup, SWT.BORDER | SWT.SINGLE);
        stackSize.setLayoutData(new GridData(UIUtils.getDefaultFieldSize() / 2, SWT.DEFAULT));        
        Label kb2 = new Label(memoryGroup, SWT.NONE);
        kb2.setText("kb");
        
        Label dataSizeLabel = new Label(memoryGroup, SWT.NONE);
        dataSizeLabel.setText("&Data Size:");
        
        dataSize = new Text(memoryGroup, SWT.BORDER | SWT.SINGLE);
        dataSize.setLayoutData(new GridData(UIUtils.getDefaultFieldSize() / 2, SWT.DEFAULT));
        Label kb3 = new Label(memoryGroup, SWT.NONE);
        kb3.setText("kb");
	}

	private void createPackagingTab(TabFolder tabs) {
		TabItem packagingTab = new TabItem(tabs, SWT.NONE);
		packagingTab.setText("&Packaging");
		
		Composite packaging = new Composite(tabs, SWT.NONE);
		packagingTab.setControl(packaging);
		
        packaging.setLayout(new GridLayout(1, false));
        packaging.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        useDebugRuntimes = new Button(packaging, SWT.CHECK);
        useDebugRuntimes.setText("Use Debug &Runtimes");
	}

	private void createPathsTab(TabFolder tabs) {
		TabItem buildPathsTab = new TabItem(tabs, SWT.NONE);
		buildPathsTab.setText("&Paths and Files");
        Composite buildPaths = new Composite(tabs, SWT.NONE);
        buildPathsTab.setControl(buildPaths);
        
        buildPaths.setLayout(new GridLayout(2, false));
        buildPaths.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label includePathsLabel = new Label(buildPaths, SWT.NONE);
        includePathsLabel.setText("Additional &Include Paths:");
        includePathsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        additionalIncludePathsText = new Text(buildPaths, SWT.BORDER | SWT.SINGLE);
        additionalIncludePathsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ignoreDefaultIncludePaths = new Button(buildPaths, SWT.CHECK);
        ignoreDefaultIncludePaths.setText("Ignore &Default");

        Label libraryPathsLabel = new Label(buildPaths, SWT.NONE);
        libraryPathsLabel.setText("Additional &Library Paths:");
        libraryPathsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        additionalLibraryPathsText = new Text(buildPaths, SWT.BORDER | SWT.SINGLE);
        additionalLibraryPathsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ignoreDefaultLibraryPaths = new Button(buildPaths, SWT.CHECK);
        ignoreDefaultLibraryPaths.setText("Ignore De&fault");

        Label librariesLabel = new Label(buildPaths, SWT.NONE);
        librariesLabel.setText("Additional &Libraries:");
        librariesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        additionalLibrariesText = new Text(buildPaths, SWT.BORDER | SWT.SINGLE);
        additionalLibrariesText.setText("Additional L&ibraries");
        additionalLibrariesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ignoreDefaultLibraries = new Button(buildPaths, SWT.CHECK);
        ignoreDefaultLibraries.setText("I&gnore Default");

        Label excludeFilesLabel = new Label(buildPaths, SWT.NONE);
        excludeFilesLabel.setText("Exclude file pattern");
        excludeFilesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        excludeFiles = new Text(buildPaths, SWT.BORDER | SWT.SINGLE);
        GridData excludeFilesData = new GridData(GridData.FILL_HORIZONTAL);
        excludeFilesData.horizontalSpan = 2;
        excludeFiles.setLayoutData(excludeFilesData);

        Label outputPathLabel = new Label(buildPaths, SWT.NONE);
        outputPathLabel.setText("&Output File (libraries only)");
        outputPathLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        libOutputPath = new Text(buildPaths, SWT.BORDER | SWT.SINGLE);
        GridData outputPathData = new GridData(GridData.FILL_HORIZONTAL);
        outputPathData.horizontalSpan = 2;
        libOutputPath.setLayoutData(outputPathData);

        Label appPathLabel = new Label(buildPaths, SWT.NONE);
        appPathLabel.setText("&Output Directory (applications only)");
        appPathLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        appOutputPath = new Text(buildPaths, SWT.BORDER | SWT.SINGLE);
        GridData appPathData = new GridData(GridData.FILL_HORIZONTAL);
        appPathData.horizontalSpan = 2;
        appOutputPath.setLayoutData(appPathData);	}

	protected void reinitUI() {
    	main.dispose();
    	fillPlaceHolder(placeHolder);
    }
    
    private void initUI() {
    	IWorkingCopy configProperties = getWorkingCopyOfBuildConfiguration();
    	
    	incrementalBuildStrategy.select(PropertyUtil.getInteger(getProject(), MoSyncProject.DEPENDENCY_STRATEGY, MoSyncProject.GCC_DEPENDENCY_STRATEGY));
    	
        ignoreDefaultIncludePaths.setSelection(PropertyUtil.getBoolean(configProperties, MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS));
        setText(additionalIncludePathsText, configProperties.getProperty(MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS));

        ignoreDefaultLibraryPaths.setSelection(PropertyUtil.getBoolean(configProperties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS));
        setText(additionalLibraryPathsText, configProperties.getProperty(MoSyncBuilder.ADDITIONAL_LIBRARY_PATHS));

        ignoreDefaultLibraries.setSelection(PropertyUtil.getBoolean(configProperties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES));
        setText(additionalLibrariesText, configProperties.getProperty(MoSyncBuilder.ADDITIONAL_LIBRARIES));
        
        setText(excludeFiles, configProperties.getProperty(MoSyncProject.EXCLUDE_FILTER_KEY));

        setText(libOutputPath, configProperties.getProperty(MoSyncBuilder.LIB_OUTPUT_PATH));
        setText(appOutputPath, configProperties.getProperty(MoSyncBuilder.APP_OUTPUT_PATH));

        boolean isLibraryProject = MoSyncBuilder.PROJECT_TYPE_LIBRARY.equals(getProject().getProperty(MoSyncBuilder.PROJECT_TYPE));
        libraryProjectType.setSelection(isLibraryProject);
        applicationProjectType.setSelection(!isLibraryProject);

        deadCodeElim.setSelection(PropertyUtil.getBoolean(configProperties, MoSyncBuilder.DEAD_CODE_ELIMINATION));

        setText(gccFlags, configProperties.getProperty(MoSyncBuilder.EXTRA_COMPILER_SWITCHES));
        setText(extraRes, configProperties.getProperty(MoSyncBuilder.EXTRA_RES_SWITCHES));
        setText(extraLink, configProperties.getProperty(MoSyncBuilder.EXTRA_LINK_SWITCHES));

        Integer gcc = PropertyUtil.getInteger(configProperties, MoSyncBuilder.GCC_WARNINGS);

        gccWerror.setSelection(gcc != null && (gcc & MoSyncBuilder.GCC_WERROR) != 0);
        gccWall.setSelection(gcc != null && (gcc & MoSyncBuilder.GCC_WALL) != 0);
        gccWextra.setSelection(gcc != null && (gcc & MoSyncBuilder.GCC_WEXTRA) != 0);
        
        setText(heapSize, configProperties.getProperty(MoSyncBuilder.MEMORY_HEAPSIZE_KB));
        setText(stackSize, configProperties.getProperty(MoSyncBuilder.MEMORY_STACKSIZE_KB));
        setText(dataSize, configProperties.getProperty(MoSyncBuilder.MEMORY_DATASIZE_KB));
        
        useDebugRuntimes.setSelection(PropertyUtil.getBoolean(configProperties, MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS));
        
        updateUI(null);
    }
    
    public void changeConfiguration(IBuildConfiguration cfg) {
    	this.currentConfigId = cfg == null ? null : cfg.getId();
    }

    public void changeConfiguration(String id) {
    	this.currentConfigId = id;
    }
    
    public IPropertyOwner getBuildConfigurationProperties() {
    	return currentConfigId == null ? getProject() : getProject().getBuildConfiguration(currentConfigId).getProperties();
    }
    
    public IWorkingCopy getWorkingCopyOfBuildConfiguration() {
    	IPropertyOwner original = getBuildConfigurationProperties();
    	IWorkingCopy workingCopy = workingCopies.get(original);
    	if (workingCopy == null) {
    		workingCopy = new PropertyOwnerWorkingCopy(original);
    		workingCopies.put(original, workingCopy);
    	}
    	
    	return workingCopy;
    }
    
    void clearWorkingCopies() {
    	workingCopies.clear();
    }
    
    boolean applyWorkingCopies() {
    	boolean result = false;
    	for (IWorkingCopy workingCopy : workingCopies.values()) {
    		result |= workingCopy.apply();
    	}
    	
    	return result;
    }

    private void setText(Text text, String value) {
        text.setText(value == null ? "" : value);
    }

    private void updateUI(Event event) {
        listener.setActive(false);
        boolean isLibraryProject = libraryProjectType.getSelection();
        deadCodeElim.setEnabled(!isLibraryProject);
        libOutputPath.setEnabled(isLibraryProject);
        appOutputPath.setEnabled(!isLibraryProject);

        validate();
        listener.setActive(true);
    }

    private void validate() {
        String message = null;
        int severity = NONE;

        String warnIfSpaces = warnIfSpaces(null, "Additional Libraries", additionalLibrariesText);
        warnIfSpaces = warnIfSpaces(warnIfSpaces, "Additional Library Paths", additionalLibraryPathsText);
        warnIfSpaces = warnIfSpaces(warnIfSpaces, "Additional Include Paths", additionalIncludePathsText);

        if (warnIfSpaces != null) {
            severity = WARNING;
            message = warnIfSpaces;
        }

        if (libraryProjectType.getSelection()) {
            if (libOutputPath.getText().length() > 0 && Util.getExtension(new File(libOutputPath.getText())).length() == 0) {
                severity = WARNING;
                message = "Output file has no extension";
            }
            
            if (libOutputPath.getText().length() == 0) {
                severity = ERROR;
                message = "Library output file must be set";
            }
        } 
        
        setMessage(message, severity);
    }

    private String warnIfSpaces(String shortcurcuit, String fieldName, Text text) {
        if (shortcurcuit != null) {
            return shortcurcuit;
        }

        String str = text.getText().trim();
        IPath[] paths = PropertyUtil.toPaths(str);
        if (paths.length == 1) {
            if (paths[0].toOSString().indexOf(' ') != -1) {
                return MessageFormat.format("\"{0}\": space is an invalid delimiter - use comma (,) instead", fieldName);
            }
        }

        return null;
    }

    public void performDefaults() {
        ignoreDefaultIncludePaths.setSelection(false);
        additionalIncludePathsText.setText("");
    }
   
    public boolean performOk() {
    	// First, update.
    	updateCurrentBuildConfiguration();

    	// Then, apply.
    	boolean changed = applyWorkingCopies();
    	
        if (changed && (getProject().getActiveBuildConfiguration() == null || getProject().getActiveBuildConfiguration().getId().equals(currentConfigId))) {
            ProjectBuildAction action = new ProjectBuildAction(getShell(), IncrementalProjectBuilder.FULL_BUILD);
            action.setProject(getProject().getWrappedProject());
            action.run();
        }

    	return true; 	
    }
    
    public boolean performCancel() {
    	clearWorkingCopies();
    	return super.performCancel();
    }
    
    public boolean updateCurrentBuildConfiguration() {
    	IWorkingCopy configProperties = getWorkingCopyOfBuildConfiguration();
    	
        boolean changed = false;
        changed |= PropertyUtil.setInteger(getProject(), MoSyncProject.DEPENDENCY_STRATEGY, incrementalBuildStrategy.getSelectionIndex());
        
        changed |= configProperties.setProperty(MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS, additionalIncludePathsText.getText().replace(';', ','));
        changed |= PropertyUtil.setBoolean(configProperties, MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS, ignoreDefaultIncludePaths
                .getSelection());

        changed |= configProperties.setProperty(MoSyncBuilder.ADDITIONAL_LIBRARY_PATHS, additionalLibraryPathsText.getText().replace(';', ','));
        changed |= PropertyUtil.setBoolean(configProperties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS, ignoreDefaultLibraryPaths
                .getSelection());

        changed |= configProperties.setProperty(MoSyncBuilder.ADDITIONAL_LIBRARIES, additionalLibrariesText.getText().replace(';', ','));
        changed |= PropertyUtil.setBoolean(configProperties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES, ignoreDefaultLibraries
                .getSelection());

        changed |= configProperties.setProperty(MoSyncProject.EXCLUDE_FILTER_KEY, excludeFiles.getText());
        changed |= configProperties.setProperty(MoSyncBuilder.LIB_OUTPUT_PATH, libOutputPath.getText());
    	changed |= configProperties.setProperty(MoSyncBuilder.APP_OUTPUT_PATH, appOutputPath.getText());

        changed |= PropertyUtil.setBoolean(configProperties, MoSyncBuilder.DEAD_CODE_ELIMINATION, deadCodeElim.getSelection());

        changed |= getProject().setProperty(MoSyncBuilder.PROJECT_TYPE,
                libraryProjectType.getSelection() ? MoSyncBuilder.PROJECT_TYPE_LIBRARY : "");

        changed |= configProperties.setProperty(MoSyncBuilder.EXTRA_COMPILER_SWITCHES, gccFlags.getText());
        changed |= configProperties.setProperty(MoSyncBuilder.EXTRA_RES_SWITCHES, extraRes.getText());
        changed |= configProperties.setProperty(MoSyncBuilder.EXTRA_LINK_SWITCHES, extraLink.getText());

        int gccWarnings = 0;
        gccWarnings |= gccWerror.getSelection() ? MoSyncBuilder.GCC_WERROR : 0;
        gccWarnings |= gccWall.getSelection() ? MoSyncBuilder.GCC_WALL : 0;
        gccWarnings |= gccWextra.getSelection() ? MoSyncBuilder.GCC_WEXTRA : 0;
        
        changed |= PropertyUtil.setInteger(configProperties, MoSyncBuilder.GCC_WARNINGS, gccWarnings);
        
        changed |= configProperties.setProperty(MoSyncBuilder.MEMORY_HEAPSIZE_KB, heapSize.getText());
        changed |= configProperties.setProperty(MoSyncBuilder.MEMORY_STACKSIZE_KB, stackSize.getText());
        changed |= configProperties.setProperty(MoSyncBuilder.MEMORY_DATASIZE_KB, dataSize.getText());
        
        changed |= PropertyUtil.setBoolean(configProperties, MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS, useDebugRuntimes.getSelection());
        
        return changed;
    }

    private MoSyncProject getProject() {
        IProject wrappedProject = (IProject) getElement();
        MoSyncProject project = MoSyncProject.create(wrappedProject);

        return project;
    }

	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getPropertyName();
		if (MoSyncProject.BUILD_CONFIGURATION_SUPPORT_CHANGED.equals(property) ||
			MoSyncProject.BUILD_CONFIGURATION_CHANGED.equals(property)) {
			reinitUI();
		}
	}
	
	public void dispose() {
		getProject().removePropertyChangeListener(this);
		super.dispose();
	}
}
