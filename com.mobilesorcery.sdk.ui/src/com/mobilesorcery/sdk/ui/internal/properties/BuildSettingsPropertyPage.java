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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.IsExperimentalTester;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncExtension;
import com.mobilesorcery.sdk.core.MoSyncExtensionManager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncProjectParameterResolver;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.NameSpacePropertyOwner;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.PropertyOwnerWorkingCopy;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.core.IPropertyOwner.IWorkingCopy;
import com.mobilesorcery.sdk.core.templates.ITemplate;
import com.mobilesorcery.sdk.core.templates.ProjectTemplate;
import com.mobilesorcery.sdk.core.templates.TemplateManager;
import com.mobilesorcery.sdk.ui.BuildConfigurationsContentProvider;
import com.mobilesorcery.sdk.ui.BuildConfigurationsLabelProvider;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.MoSyncPropertyPage;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.Note;
import com.mobilesorcery.sdk.ui.ParameterResolverContentProvider;
import com.mobilesorcery.sdk.ui.UIUtils;
import com.mobilesorcery.sdk.ui.UpdateListener;
import com.mobilesorcery.sdk.ui.ValidationMessageProvider;

public class BuildSettingsPropertyPage extends MoSyncPropertyPage implements PropertyChangeListener {

    public BuildSettingsPropertyPage() {
		super(true);
	}

	public class BuildConfigurationChangedListener implements ISelectionChangedListener {

        private boolean active = true;

        @Override
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

    private static final String BINARY_TYPE_NATIVE = "Native";
    private static final String BINARY_TYPE_INTERPRETED = "Interpreted";
    private static final String BINARY_TYPE_RECOMPILED = "Statically Recompiled";
	private static final String[] BINARY_TYPES = new String[] { BINARY_TYPE_NATIVE, BINARY_TYPE_INTERPRETED, BINARY_TYPE_RECOMPILED };

	private Text additionalIncludePathsText;
	private Text additionalNativeIncludePathsText;
    private Button ignoreDefaultIncludePaths;
    private Text additionalLibraryPathsText;
    private Button ignoreDefaultLibraryPaths;
    private Button deadCodeElim;
    private Text gccFlags;
    private Button applicationProjectType;
    private Button libraryProjectType;
    private Button extensionProjectType;
    private Text additionalLibrariesText;
    private Button ignoreDefaultLibraries;
    private Text extraRes;
    private Text extraLink;
    private Text libOutputPath;
    private Text appOutputPath;
    private Text extensions;
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
    private final HashMap<Object, IPropertyOwner> workingCopies = new HashMap<Object, IPropertyOwner>();
    private IWorkingCopy projectWorkingCopy;
	private final HashSet<String> buildProperties = new HashSet<String>();

	private Composite main;
    private Composite placeHolder;

	private StackLayout buildPathsLayout;
	private Composite buildPathsInterpreted;
	private Composite buildPathsNative;
	private Composite buildPaths;
	
    private Text excludeFiles;
    
	private Group memoryGroup;
    private Text heapSize;
    private Text stackSize;
    private Text dataSize;
    private Text vendor;
    private Text version;
    private Text appName;

	private Combo binaryType;

	private boolean supportsExtensions;

	private Label librariesLabel;
	private Label excludeFilesLabel;
	private Label libraryPathsLabel;

    @Override
	protected Control createContents(Composite parent) {
    	supportsExtensions = MoSyncExtensionManager.supportsExtensions() || Boolean.TRUE.equals(IsExperimentalTester.isExperimental());
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
        projectType.setLayout(new GridLayout(3, false));
        projectType.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        projectType.setText("Project &Type");

        applicationProjectType = new Button(projectType, SWT.RADIO);
        applicationProjectType.setText("&Application");

        libraryProjectType = new Button(projectType, SWT.RADIO);
        libraryProjectType.setText("&Library");
        
        if (supportsExtensions) {
	        extensionProjectType = new Button(projectType, SWT.RADIO);
	        extensionProjectType.setText("&Extension");
        }

        Group buildStrategy = new Group(main, SWT.NONE);
        buildStrategy.setLayout(new GridLayout(2, false));
        buildStrategy.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        buildStrategy.setText("Building");

        Label binaryTypeLabel = new Label(buildStrategy, SWT.NONE);
        binaryTypeLabel.setText("Binary Type");
        
        binaryType = new Combo(buildStrategy, SWT.READ_ONLY);
        binaryType.setItems(BINARY_TYPES);
        binaryType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        
        Label binaryTypeInfo = new Label(buildStrategy, SWT.WRAP);
        binaryTypeInfo.setText("The binary type indicates how building should take place.\n" +
        		"If a certain binary type is not available for a platform, " +
        		"the 'Interpreted' binary type will be used.");
        binaryTypeInfo.setFont(MosyncUIPlugin.getDefault().getFont(MosyncUIPlugin.FONT_INFO_TEXT));
        binaryTypeInfo.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
        
        Label incrementalBuildStrategyLabel = new Label(buildStrategy, SWT.NONE);
        incrementalBuildStrategyLabel.setText("Incremental Build Strategy To Use:");

        incrementalBuildStrategy = new Combo(buildStrategy, SWT.READ_ONLY);
        incrementalBuildStrategy.setItems(INCREMENTAL_BUILD_STRATEGY_OPTIONS);
        incrementalBuildStrategy.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        boolean hasConfigurations = getProject().areBuildConfigurationsSupported();
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

        listener = new UpdateListener(this);

        applicationProjectType.addListener(SWT.Selection, listener);
        libraryProjectType.addListener(SWT.Selection, listener);
        applicationProjectType.addListener(SWT.Selection, listener);
        incrementalBuildStrategy.addListener(SWT.Selection, listener);
        binaryType.addListener(SWT.Selection, listener);
        additionalIncludePathsText.addListener(SWT.Modify, listener);
        additionalNativeIncludePathsText.addListener(SWT.Modify, listener);
        additionalLibrariesText.addListener(SWT.Modify, listener);
        additionalLibraryPathsText.addListener(SWT.Modify, listener);
        libOutputPath.addListener(SWT.Modify, listener);
        appOutputPath.addListener(SWT.Modify, listener);
        stackSize.addListener(SWT.Modify, listener);
        heapSize.addListener(SWT.Modify, listener);
        dataSize.addListener(SWT.Modify, listener);
        version.addListener(SWT.Modify, listener);
        appName.addListener(SWT.Modify, listener);
        
        if (supportsExtensions) {
            extensionProjectType.addListener(SWT.Selection, listener);
        	extensions.addListener(SWT.Modify, listener);
        }

        changeConfiguration(getProject().getActiveBuildConfiguration());
        if (getProject().areBuildConfigurationsSupported()) {
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
        addContentAssist(gccFlags);

        Label extraResLabel = new Label(compilerFlags, SWT.NONE);
        extraResLabel.setText("Extra &Resource Compiler Switches:");
        extraRes = new Text(compilerFlags, SWT.BORDER | SWT.SINGLE);
        extraRes.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addContentAssist(extraRes);

        Label extraLinkLabel = new Label(compilerFlags, SWT.NONE);
        extraLinkLabel.setText("Extra &Linker Switches:");
        extraLink = new Text(compilerFlags, SWT.BORDER | SWT.SINGLE);
        extraLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addContentAssist(extraLink);

        Label gccWarningsLabel = new Label(compilerFlags, SWT.NONE);
        gccWarningsLabel.setText("GCC &Warnings:");
        gccWarningsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 3));

        gccWerror = new Button(compilerFlags, SWT.CHECK);
        gccWerror.setText("Warnings as &Errors");
        gccWall = new Button(compilerFlags, SWT.CHECK);
        gccWall.setText("&All Warnings");
        gccWextra = new Button(compilerFlags, SWT.CHECK);
        gccWextra.setText("E&xtra Warnings");

        memoryGroup = new Group(compilerFlags, SWT.NONE);
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

        packaging.setLayout(new GridLayout(2, false));
        packaging.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        useDebugRuntimes = new Button(packaging, SWT.CHECK);
        useDebugRuntimes.setText("Use Debug &Runtimes");
        GridData useDebugRuntimesData = new GridData(GridData.FILL_HORIZONTAL);
        useDebugRuntimesData.horizontalSpan = 2;
        useDebugRuntimes.setLayoutData(useDebugRuntimesData);

        Label versionLabel = new Label(packaging, SWT.NONE);
        versionLabel.setText("&Version:");

        version = new Text(packaging, SWT.BORDER | SWT.SINGLE);
        version.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label vendorNameLabel = new Label(packaging, SWT.NONE);
        vendorNameLabel.setText("&Publisher:");

        vendor = new Text(packaging, SWT.BORDER | SWT.SINGLE);
        vendor.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label appNameLabel = new Label(packaging, SWT.NONE);
        appNameLabel.setText("&Application Name:");

        appName = new Text(packaging, SWT.BORDER | SWT.SINGLE);
        appName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    private void createPathsTab(TabFolder tabs) {
        TabItem buildPathsTab = new TabItem(tabs, SWT.NONE);
        buildPathsTab.setText("&Paths and Files");
        buildPaths = new Composite(tabs, SWT.NONE);
        buildPathsTab.setControl(buildPaths);

        buildPathsLayout = new StackLayout();
        buildPaths.setLayout(buildPathsLayout);

        buildPathsInterpreted = new Composite(buildPaths, SWT.NONE);
       
        buildPathsInterpreted.setLayout(new GridLayout(2, false));
        buildPathsInterpreted.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label includePathsLabel = new Label(buildPathsInterpreted, SWT.NONE);
        includePathsLabel.setText("Additional &Include Paths:");
        includePathsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        additionalIncludePathsText = new Text(buildPathsInterpreted, SWT.BORDER | SWT.SINGLE);
        additionalIncludePathsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addContentAssist(additionalIncludePathsText);

        ignoreDefaultIncludePaths = new Button(buildPathsInterpreted, SWT.CHECK);
        ignoreDefaultIncludePaths.setText("Ignore &Default");

        libraryPathsLabel = new Label(buildPathsInterpreted, SWT.NONE);
        libraryPathsLabel.setText("Additional &Library Paths:");
        libraryPathsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        additionalLibraryPathsText = new Text(buildPathsInterpreted, SWT.BORDER | SWT.SINGLE);
        additionalLibraryPathsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addContentAssist(additionalLibraryPathsText);

        ignoreDefaultLibraryPaths = new Button(buildPathsInterpreted, SWT.CHECK);
        ignoreDefaultLibraryPaths.setText("Ignore De&fault");

        librariesLabel = new Label(buildPathsInterpreted, SWT.NONE);
        librariesLabel.setText("Additional &Libraries:");
        librariesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        additionalLibrariesText = new Text(buildPathsInterpreted, SWT.BORDER | SWT.SINGLE);
        additionalLibrariesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ignoreDefaultLibraries = new Button(buildPathsInterpreted, SWT.CHECK);
        ignoreDefaultLibraries.setText("I&gnore Default");

        excludeFilesLabel = new Label(buildPathsInterpreted, SWT.NONE);
        excludeFilesLabel.setText("Exclude file pattern");
        excludeFilesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        excludeFiles = new Text(buildPathsInterpreted, SWT.BORDER | SWT.SINGLE);
        GridData excludeFilesData = new GridData(GridData.FILL_HORIZONTAL);
        excludeFilesData.horizontalSpan = 2;
        excludeFiles.setLayoutData(excludeFilesData);
        
        Label outputPathLabel = new Label(buildPathsInterpreted, SWT.NONE);
        outputPathLabel.setText("&Output File (libraries only)");
        outputPathLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        libOutputPath = new Text(buildPathsInterpreted, SWT.BORDER | SWT.SINGLE);
        GridData outputPathData = new GridData(GridData.FILL_HORIZONTAL);
        outputPathData.horizontalSpan = 2;
        libOutputPath.setLayoutData(outputPathData);

        Label appPathLabel = new Label(buildPathsInterpreted, SWT.NONE);
        appPathLabel.setText("&Output Directory (applications only)");
        appPathLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        appOutputPath = new Text(buildPathsInterpreted, SWT.BORDER | SWT.SINGLE);
        GridData appPathData = new GridData(GridData.FILL_HORIZONTAL);
        appPathData.horizontalSpan = 2;
        appOutputPath.setLayoutData(appPathData);
       
        buildPathsNative = new Composite(buildPaths, SWT.NONE);
        buildPathsNative.setLayout(new GridLayout(2, false));
        buildPathsNative.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 
        Label includeNativePathsLabel = new Label(buildPathsNative, SWT.NONE);
        includeNativePathsLabel.setText("Additional &Include Paths (relative to project):");
        includeNativePathsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        additionalNativeIncludePathsText = new Text(buildPathsNative, SWT.BORDER | SWT.SINGLE);
        additionalNativeIncludePathsText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        addContentAssist(additionalIncludePathsText);

        if (supportsExtensions) {
	        Label extensionsLabel = new Label(buildPathsNative, SWT.NONE);
	        extensionsLabel.setText("Extensions");
	        extensionsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
	
	        extensions = new Text(buildPathsNative, SWT.BORDER | SWT.SINGLE);
	        GridData extensionsData = new GridData(GridData.FILL_HORIZONTAL);
	        extensionsData.horizontalSpan = 2;
	        extensions.setLayoutData(extensionsData);
        }
    }

    protected void reinitUI() {
        main.dispose();
        fillPlaceHolder(placeHolder);
    }

    private void initUI() {
    	listener.setActive(false);

    	IPropertyOwner projectWorkingCopy = getWorkingCopyOfBuildConfiguration(getProject());

    	boolean isLibraryProject = MoSyncBuilder.PROJECT_TYPE_LIBRARY.equals(projectWorkingCopy.getProperty(addBuildProperty(MoSyncBuilder.PROJECT_TYPE)));
    	boolean isExtensionProject = MoSyncBuilder.PROJECT_TYPE_EXTENSION.equals(projectWorkingCopy.getProperty(addBuildProperty(MoSyncBuilder.PROJECT_TYPE)));
    	boolean isAppProject = !isLibraryProject && !isExtensionProject;
        libraryProjectType.setSelection(isLibraryProject);
        applicationProjectType.setSelection(isAppProject);

        binaryType.setText(getBinaryTypeLabel(getProject().getProperty(MoSyncBuilder.OUTPUT_TYPE)));
        incrementalBuildStrategy.select(PropertyUtil.getInteger(projectWorkingCopy, addBuildProperty(MoSyncProject.DEPENDENCY_STRATEGY), MoSyncProject.GCC_DEPENDENCY_STRATEGY));

    	IPropertyOwner configProperties = getWorkingCopyOfBuildConfiguration();

        ignoreDefaultIncludePaths.setSelection(PropertyUtil.getBoolean(configProperties, addBuildProperty(MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS)));
        setText(additionalIncludePathsText, configProperties.getProperty(addBuildProperty(MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS)));

        setText(additionalNativeIncludePathsText, configProperties.getProperty(addBuildProperty(MoSyncBuilder.ADDITIONAL_NATIVE_INCLUDE_PATHS)));
        
        ignoreDefaultLibraryPaths.setSelection(PropertyUtil.getBoolean(configProperties, addBuildProperty(MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS)));
        setText(additionalLibraryPathsText, configProperties.getProperty(addBuildProperty(MoSyncBuilder.ADDITIONAL_LIBRARY_PATHS)));

        ignoreDefaultLibraries.setSelection(PropertyUtil.getBoolean(configProperties, addBuildProperty(MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES)));
        setText(additionalLibrariesText, configProperties.getProperty(addBuildProperty(MoSyncBuilder.ADDITIONAL_LIBRARIES)));

        setText(excludeFiles, configProperties.getProperty(addBuildProperty(MoSyncProject.EXCLUDE_FILTER_KEY)));
        
        setText(libOutputPath, configProperties.getProperty(addBuildProperty(MoSyncBuilder.LIB_OUTPUT_PATH)));
        setText(appOutputPath, configProperties.getProperty(addBuildProperty(MoSyncBuilder.APP_OUTPUT_PATH)));

        deadCodeElim.setSelection(PropertyUtil.getBoolean(configProperties, addBuildProperty(MoSyncBuilder.DEAD_CODE_ELIMINATION)));

        setText(gccFlags, configProperties.getProperty(addBuildProperty(MoSyncBuilder.EXTRA_COMPILER_SWITCHES)));
        setText(extraRes, configProperties.getProperty(addBuildProperty(MoSyncBuilder.EXTRA_RES_SWITCHES)));
        setText(extraLink, configProperties.getProperty(addBuildProperty(MoSyncBuilder.EXTRA_LINK_SWITCHES)));

        Integer gcc = PropertyUtil.getInteger(configProperties, addBuildProperty(MoSyncBuilder.GCC_WARNINGS));

        gccWerror.setSelection(gcc != null && (gcc & MoSyncBuilder.GCC_WERROR) != 0);
        gccWall.setSelection(gcc != null && (gcc & MoSyncBuilder.GCC_WALL) != 0);
        gccWextra.setSelection(gcc != null && (gcc & MoSyncBuilder.GCC_WEXTRA) != 0);

        setText(heapSize, configProperties.getProperty(addBuildProperty(MoSyncBuilder.MEMORY_HEAPSIZE_KB)));
        setText(stackSize, configProperties.getProperty(addBuildProperty(MoSyncBuilder.MEMORY_STACKSIZE_KB)));
        setText(dataSize, configProperties.getProperty(addBuildProperty(MoSyncBuilder.MEMORY_DATASIZE_KB)));

        useDebugRuntimes.setSelection(PropertyUtil.getBoolean(configProperties, addBuildProperty(MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS)));
        String outputType = configProperties.getProperty(addBuildProperty(MoSyncBuilder.OUTPUT_TYPE));
        setText(version, configProperties.getProperty(addBuildProperty(MoSyncBuilder.PROJECT_VERSION)));
        setText(vendor, configProperties.getProperty(addBuildProperty(DefaultPackager.APP_VENDOR_NAME_BUILD_PROP)));
        setText(appName, configProperties.getProperty(addBuildProperty(MoSyncBuilder.APP_NAME)));
        
        if (supportsExtensions) {
        	setText(extensions, configProperties.getProperty(addBuildProperty(MoSyncBuilder.EXTENSIONS)));
            extensionProjectType.setSelection(isExtensionProject);
        }
        
        listener.setActive(true);
        updateUI();
    }

    private String addBuildProperty(String buildProperty) {
		buildProperties.add(buildProperty);
		return buildProperty;
	}

	public void changeConfiguration(IBuildConfiguration cfg) {
        this.currentConfigId = cfg == null ? null : cfg.getId();
    }

    public void changeConfiguration(String id) {
        this.currentConfigId = id;
    }

    public IPropertyOwner getBuildConfigurationProperties() {
        if (currentConfigId == null) {
            return getProject();
        } else {
            IBuildConfiguration cfg = getProject().getBuildConfiguration(currentConfigId);
            return cfg == null ? getProject() : cfg.getProperties();
        }
    }

    public IPropertyOwner getWorkingCopyOfBuildConfiguration() {
    	IPropertyOwner original = getBuildConfigurationProperties();
    	return getWorkingCopyOfBuildConfiguration(original);
    }

    public IPropertyOwner getWorkingCopyOfBuildConfiguration(IPropertyOwner original) {
        IPropertyOwner workingCopy = workingCopies.get(original);
        if (workingCopy == null) {
        	if (original == getProject()) {
        		projectWorkingCopy = new PropertyOwnerWorkingCopy(original);
        		workingCopy = projectWorkingCopy;
        	} else {
        		String ns = ((NameSpacePropertyOwner) original).getNamespace();
        		workingCopy = new NameSpacePropertyOwner(getWorkingCopyOfBuildConfiguration(getProject()), ns);
        	}
            workingCopies.put(original, workingCopy);
        }

        return workingCopy;
    }

    void clearWorkingCopies() {
        workingCopies.clear();
    }

    boolean applyWorkingCopies() {
        boolean result = projectWorkingCopy.apply();
        return result;
    }

    @Override
	public void updateUI() {
		boolean wasActive = listener.isActive();
        listener.setActive(false);
        boolean isLibOrExtProject = !applicationProjectType.getSelection();
        deadCodeElim.setEnabled(!isLibOrExtProject);
        libOutputPath.setEnabled(isLibOrExtProject);
        appOutputPath.setEnabled(!isLibOrExtProject);

        boolean isInterpreted = getBinaryType().equals(MoSyncBuilder.OUTPUT_TYPE_INTERPRETED);
        incrementalBuildStrategy.setEnabled(isInterpreted);
        libraryProjectType.setEnabled(isInterpreted);
        
        boolean isNative = getBinaryType().equals(MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE);
        librariesLabel.setEnabled(!isNative);
        excludeFilesLabel.setEnabled(!isNative);
        libraryPathsLabel.setEnabled(!isNative);
        additionalLibrariesText.setEnabled(!isNative);
        additionalLibrariesText.setEchoChar(isNative ? ' ' : '\0');
        additionalLibraryPathsText.setEnabled(!isNative);
        additionalLibraryPathsText.setEchoChar(isNative ? ' ' : '\0');
        ignoreDefaultLibraries.setEnabled(!isNative);
        ignoreDefaultLibraryPaths.setEnabled(!isNative);
        excludeFiles.setEnabled(!isNative);
        additionalLibraryPathsText.setEchoChar(isNative ? ' ' : '\0');
        deadCodeElim.setEnabled(false);
        memoryGroup.setEnabled(!isNative);
        heapSize.setEnabled(!isNative);
        stackSize.setEnabled(!isNative);
        dataSize.setEnabled(!isNative);
        heapSize.setEchoChar(isNative ? ' ' : '\0');
        stackSize.setEchoChar(isNative ? ' ' : '\0');
        dataSize.setEchoChar(isNative ? ' ' : '\0');
        
        Control oldTop = buildPathsLayout.topControl;
        Control newTop = isNative ? buildPathsNative : buildPathsInterpreted;
        if (oldTop != newTop) {
        	buildPathsLayout.topControl = newTop;
        	buildPaths.layout();
        }
        
        super.updateUI();

        listener.setActive(wasActive);
    }

    @Override
	protected void validate() {
        ValidationMessageProvider message = new ValidationMessageProvider();

        if (appName.getText().indexOf('.') != -1) {
        	message.setMessage(appName, new DefaultMessageProvider("Application name should not have an extension", IMessageProvider.ERROR));
        }

        validatePathsField(message, "Additional Libraries", additionalLibrariesText, null);
        validatePathsField(message, "Additional Library Paths", additionalLibraryPathsText, null);
        validatePathsField(message, "Additional Include Paths", additionalIncludePathsText, null);

        validateMemorySettings(message);

        if (libraryProjectType.getSelection() || (supportsExtensions && extensionProjectType.getSelection())) {
            if (libOutputPath.getText().length() > 0 && Util.getExtension(new File(libOutputPath.getText())).length() == 0) {
            	message.setMessage(libOutputPath, new DefaultMessageProvider("Output file has no extension", IMessageProvider.WARNING));
            }

            if (libOutputPath.getText().length() == 0) {
                message.setMessage(libOutputPath, new DefaultMessageProvider("Library output file must be set", IMessageProvider.ERROR));
            }
        }

        if (supportsExtensions) {
	        String[] extensionList = PropertyUtil.toStrings(extensions.getText());
	        for (String extensionName : extensionList) {
	        	MoSyncExtension extension = MoSyncExtensionManager.getDefault().getExtension(extensionName);
	        	if (extension == null) {
	        		message.setMessage(extensions, new DefaultMessageProvider("Extension does not exist: " + extensionName, IMessageProvider.WARNING));
	        	}
	        }
        }
        
        setMessage(message);
    }

    private void validateMemorySettings(ValidationMessageProvider provider) {
        if (!new Version(version.getText()).isValid()) {
            provider.setMessage(version, new DefaultMessageProvider("Invalid version format, must comply to major[.minor][.micro][.qualifier]", IMessageProvider.ERROR));
        }

        provider.setMessage(stackSize, validateInteger(stackSize.getText(), "Stack size", 1L << 22));
        provider.setMessage(heapSize, validateInteger(heapSize.getText(), "Heap size", 1L << 22));
        provider.setMessage(dataSize, validateInteger(dataSize.getText(), "Data size", 1L << 22));

        if (provider.isEmpty(stackSize) && provider.isEmpty(heapSize) && provider.isEmpty(dataSize)) {
        	// They're all integers
            long stackSize = Long.parseLong(this.stackSize.getText());
            long heapSize = Long.parseLong(this.heapSize.getText());
            long dataSize = Long.parseLong(this.dataSize.getText());

            if (dataSize < stackSize + heapSize) {
                provider.setMessage(this.dataSize, new DefaultMessageProvider("Data size must be at least as large as the stack and heap sizes combined", IMessageProvider.ERROR));
            } else if (ceil2p(dataSize) != dataSize) {
                provider.setMessage(this.dataSize, new DefaultMessageProvider(MessageFormat.format("Will round up data size to nearest power of 2 ({0} kb)", ceil2p(dataSize)), IMessageProvider.INFORMATION));
            }
        }
    }

    private long ceil2p(long size) {
        size--;
        for (int i = 1; i < 64 / 2; i <<= 1) {
            size = size | (size >> i);
        }
        size++;
        return size;
    }

    private IMessageProvider validateInteger(String value, String fieldName, long max) {
        try {
            long numericalValue = Long.parseLong(value);
            if (numericalValue > max) {
                return new DefaultMessageProvider(MessageFormat.format("Value of {0} too large; must be no more than {1}", fieldName, max),
                        IMessageProvider.ERROR);
            }
        } catch (Exception e) {
            return new DefaultMessageProvider(MessageFormat.format("{0} must be an integer value", fieldName), IMessageProvider.ERROR);
        }

        return DefaultMessageProvider.EMPTY;
    }

    @Override
	public void performDefaults() {
        ArrayList<IPropertyOwner> workingCopies = new ArrayList<IPropertyOwner>();
        MoSyncProject project = getProject();
        if (project.areBuildConfigurationsSupported()) {
	        for (String configId : project.getBuildConfigurations()) {
	        	IPropertyOwner wc = getWorkingCopyOfBuildConfiguration(project.getBuildConfiguration(configId).getProperties());
	        	workingCopies.add(wc);
	        }
        }
        IPropertyOwner projectWorkingCopy = getWorkingCopyOfBuildConfiguration(project);
    	workingCopies.add(projectWorkingCopy);

        for (IPropertyOwner workingCopy : workingCopies) {
        	for (String property : buildProperties) {
        		workingCopy.setToDefault(property);
        	}
        }

        // TODO: That container based properties stuff :)
        // Because we might have wanted this behaviour in all
        // property & preference pages
        String templateId = getProject().getProperty(MoSyncProject.TEMPLATE_ID);
        ProjectTemplate template = templateId == null ? null : TemplateManager.getDefault().getProjectTemplate(templateId);
        if (template != null) {
        	Map<String, String> defaultTemplateProperties = template.getSettings();
        	for (String property : defaultTemplateProperties.keySet()) {
        		// This is not fool-proof, but will cover most of the templates we've created I guess.
        		String key = NameSpacePropertyOwner.getKey(property);
        		if (buildProperties.contains(key)) {
        			String value = defaultTemplateProperties.get(property);
        			projectWorkingCopy.setProperty(property, value);
        		}
        	}
        }
        initUI();
    }

    @Override
	public boolean performOk() {
        // First, update.
        updateCurrentBuildConfiguration();
        // Then, apply.
        boolean changed = applyWorkingCopies();
        changed |= !getBinaryType().equals(getProject().getProperty(MoSyncBuilder.OUTPUT_TYPE));
        try {
        	if (changed) {
        		getProject().setOutputType(getBinaryType());
        	}
        } catch (IllegalArgumentException e) {
        	String message = MessageFormat.format(
        			"The current project build settings are not suitable for {0} projects:\n\n{1}\n\nDo you want to automatically adjust the project?",
        			getBinaryType(), e.getMessage());
        	if (MessageDialog.openQuestion(getShell(), "Change project?", message)) {
        		getProject().forceOutputType(getBinaryType());
        	}
        }

        return true;
    }

    @Override
	public boolean performCancel() {
        clearWorkingCopies();
        return super.performCancel();
    }

    public boolean updateCurrentBuildConfiguration() {
        IPropertyOwner configProperties = getWorkingCopyOfBuildConfiguration();

        boolean changed = false;
        changed |= PropertyUtil.setInteger(getProject(), MoSyncProject.DEPENDENCY_STRATEGY, incrementalBuildStrategy.getSelectionIndex());

        changed |= configProperties.setProperty(MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS, additionalIncludePathsText.getText().replace(';', ','));
        changed |= PropertyUtil.setBoolean(configProperties, MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS, ignoreDefaultIncludePaths.getSelection());

        changed |= configProperties.setProperty(MoSyncBuilder.ADDITIONAL_NATIVE_INCLUDE_PATHS, additionalNativeIncludePathsText.getText().replace(';', ','));

        changed |= configProperties.setProperty(MoSyncBuilder.ADDITIONAL_LIBRARY_PATHS, additionalLibraryPathsText.getText().replace(';', ','));
        changed |= PropertyUtil.setBoolean(configProperties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS, ignoreDefaultLibraryPaths.getSelection());

        changed |= configProperties.setProperty(MoSyncBuilder.ADDITIONAL_LIBRARIES, additionalLibrariesText.getText().replace(';', ','));
        changed |= PropertyUtil.setBoolean(configProperties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES, ignoreDefaultLibraries.getSelection());

        changed |= configProperties.setProperty(MoSyncProject.EXCLUDE_FILTER_KEY, excludeFiles.getText());
        changed |= configProperties.setProperty(MoSyncBuilder.LIB_OUTPUT_PATH, libOutputPath.getText());
        changed |= configProperties.setProperty(MoSyncBuilder.APP_OUTPUT_PATH, appOutputPath.getText());
        
        changed |= PropertyUtil.setBoolean(configProperties, MoSyncBuilder.DEAD_CODE_ELIMINATION, deadCodeElim.getSelection());

        String projectTypeProperty = MoSyncBuilder.PROJECT_TYPE_APPLICATION;
        if (libraryProjectType.getSelection()) {
        	projectTypeProperty = MoSyncBuilder.PROJECT_TYPE_LIBRARY;
        }
        if (supportsExtensions && extensionProjectType.getSelection()) {
        	projectTypeProperty = MoSyncBuilder.PROJECT_TYPE_EXTENSION;
        }
        changed |= getProject().setProperty(MoSyncBuilder.PROJECT_TYPE, projectTypeProperty);

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
        changed |= configProperties.setProperty(MoSyncBuilder.PROJECT_VERSION, version.getText());
        changed |= configProperties.setProperty(DefaultPackager.APP_VENDOR_NAME_BUILD_PROP, vendor.getText());
        changed |= configProperties.setProperty(MoSyncBuilder.APP_NAME, appName.getText());

        if (supportsExtensions) {
        	changed |= configProperties.setProperty(MoSyncBuilder.EXTENSIONS, extensions.getText());
        }

        return changed;
    }

    private String getBinaryTypeLabel(String binaryType) {
    	if (MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE.equals(binaryType)) {
    		return BINARY_TYPE_NATIVE;
    	} else if (MoSyncBuilder.OUTPUT_TYPE_STATIC_RECOMPILATION.equals(binaryType)) {
    		return BINARY_TYPE_RECOMPILED;
    	} else {
    		return BINARY_TYPE_INTERPRETED;
    	}
    }
    
    private String getBinaryType() {
		String selectedType = binaryType.getText();
		if (BINARY_TYPE_NATIVE.equals(selectedType)) {
			return MoSyncBuilder.OUTPUT_TYPE_NATIVE_COMPILE;
		} else if (BINARY_TYPE_RECOMPILED.equals(selectedType)) {
			return MoSyncBuilder.OUTPUT_TYPE_STATIC_RECOMPILATION;
		} else {
			return MoSyncBuilder.OUTPUT_TYPE_INTERPRETED;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
        String property = event.getPropertyName();
        if (MoSyncProject.BUILD_CONFIGURATION_SUPPORT_CHANGED.equals(property) || MoSyncProject.BUILD_CONFIGURATION_CHANGED.equals(property)) {
            reinitUI();
        }
    }

    @Override
	public void dispose() {
        getProject().removePropertyChangeListener(this);
        super.dispose();
    }
}
