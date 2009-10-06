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

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.BuildAction;
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Util;

public class BuildSettingsPropertyPage extends PropertyPage {

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
    private Text outputPath;
    private UpdateListener listener;
    private String currentOutputPath;
    private Button gccWall;
    private Button gccWerror;
    private Button gccWextra;
    private Combo incrementalBuildStrategy;
	private Button useDebugRuntimes;
	//private Text excludeFiles;

    protected Control createContents(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
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
        
        Group buildPaths = new Group(main, SWT.NONE);
        buildPaths.setText("Build &Paths and Files");
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

        /*Label excludeFilesLabel = new Label(buildPaths, SWT.NONE);
        excludeFilesLabel.setText("&List of files to exclude");
        excludeFilesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        excludeFiles = new Text(buildPaths, SWT.BORDER | SWT.SINGLE);
        GridData excludeFilesData = new GridData(GridData.FILL_HORIZONTAL);
        excludeFilesData.horizontalSpan = 2;
        excludeFiles.setLayoutData(excludeFilesData);*/

        Label outputPathLabel = new Label(buildPaths, SWT.NONE);
        outputPathLabel.setText("&Output File (libraries only)");
        outputPathLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));

        outputPath = new Text(buildPaths, SWT.BORDER | SWT.SINGLE);
        GridData outputPathData = new GridData(GridData.FILL_HORIZONTAL);
        outputPathData.horizontalSpan = 2;
        outputPath.setLayoutData(outputPathData);

        Group compilerFlags = new Group(main, SWT.NONE);
        compilerFlags.setText("Compiler &Flags");
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
        
        Group packaging = new Group(main, SWT.NONE);
        packaging.setText("&Packaging");
        packaging.setLayout(new GridLayout(1, false));
        packaging.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        useDebugRuntimes = new Button(packaging, SWT.CHECK);
        useDebugRuntimes.setText("Use Debug &Runtimes");
        
        listener = new UpdateListener();

        initUI();

        applicationProjectType.addListener(SWT.Selection, listener);
        libraryProjectType.addListener(SWT.Selection, listener);
        additionalIncludePathsText.addListener(SWT.Modify, listener);
        additionalLibrariesText.addListener(SWT.Modify, listener);
        additionalLibraryPathsText.addListener(SWT.Modify, listener);
        outputPath.addListener(SWT.Modify, listener);

        return main;
    }

    private void initUI() {
    	incrementalBuildStrategy.select(PropertyUtil.getInteger(getProject(), MoSyncProject.DEPENDENCY_STRATEGY, MoSyncProject.GCC_DEPENDENCY_STRATEGY));
    	
        ignoreDefaultIncludePaths.setSelection(PropertyUtil.getBoolean(getProject(), MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS));
        setText(additionalIncludePathsText, getProject().getProperty(MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS));

        ignoreDefaultLibraryPaths.setSelection(PropertyUtil.getBoolean(getProject(), MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS));
        setText(additionalLibraryPathsText, getProject().getProperty(MoSyncBuilder.ADDITIONAL_LIBRARY_PATHS));

        ignoreDefaultLibraries.setSelection(PropertyUtil.getBoolean(getProject(), MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES));
        setText(additionalLibrariesText, getProject().getProperty(MoSyncBuilder.ADDITIONAL_LIBRARIES));
        
        //setText(excludeFiles, getProject().getProperty(MoSyncProject.EXCLUDE_FILTER_KEY));

        setText(outputPath, getProject().getProperty(MoSyncBuilder.OUTPUT_PATH));

        boolean isLibraryProject = MoSyncBuilder.PROJECT_TYPE_LIBRARY.equals(getProject().getProperty(MoSyncBuilder.PROJECT_TYPE));
        libraryProjectType.setSelection(isLibraryProject);
        applicationProjectType.setSelection(!isLibraryProject);

        deadCodeElim.setSelection(PropertyUtil.getBoolean(getProject(), MoSyncBuilder.DEAD_CODE_ELIMINATION));

        setText(gccFlags, getProject().getProperty(MoSyncBuilder.EXTRA_COMPILER_SWITCHES));
        setText(extraRes, getProject().getProperty(MoSyncBuilder.EXTRA_RES_SWITCHES));
        setText(extraLink, getProject().getProperty(MoSyncBuilder.EXTRA_LINK_SWITCHES));

        Integer gcc = PropertyUtil.getInteger(getProject(), MoSyncBuilder.GCC_WARNINGS);

        gccWerror.setSelection(gcc != null && (gcc & MoSyncBuilder.GCC_WERROR) != 0);
        gccWall.setSelection(gcc != null && (gcc & MoSyncBuilder.GCC_WALL) != 0);
        gccWextra.setSelection(gcc != null && (gcc & MoSyncBuilder.GCC_WEXTRA) != 0);
        
        useDebugRuntimes.setSelection(PropertyUtil.getBoolean(getProject(), MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS));
        
        updateUI(null);
    }

    private void setText(Text text, String value) {
        text.setText(value == null ? "" : value);
    }

    private void updateUI(Event event) {
        listener.setActive(false);
        boolean isLibraryProject = libraryProjectType.getSelection();
        deadCodeElim.setEnabled(!isLibraryProject);
        outputPath.setEnabled(isLibraryProject);

        if (event != null) {
            if (event.widget == libraryProjectType) {
                if (!libraryProjectType.getSelection()) {
                    currentOutputPath = outputPath.getText();
                }

                setText(outputPath, libraryProjectType.getSelection() ? currentOutputPath : null);
            }
        }
        if (applicationProjectType.getSelection()) {
            setText(outputPath, null);
        }

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
            if (outputPath.getText().length() > 0 && Util.getExtension(new File(outputPath.getText())).length() == 0) {
                severity = WARNING;
                message = "Output file has no extension";
            }
            
            if (new File(outputPath.getText()).getParentFile() == null) {
                severity = ERROR;
                message = "Output file must be at an absolute location";
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
        boolean changed = false;
        changed |= PropertyUtil.setInteger(getProject(), MoSyncProject.DEPENDENCY_STRATEGY, incrementalBuildStrategy.getSelectionIndex());
        
        changed |= getProject().setProperty(MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS, additionalIncludePathsText.getText().replace(';', ','));
        changed |= PropertyUtil.setBoolean(getProject(), MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS, ignoreDefaultIncludePaths
                .getSelection());

        changed |= getProject().setProperty(MoSyncBuilder.ADDITIONAL_LIBRARY_PATHS, additionalLibraryPathsText.getText().replace(';', ','));
        changed |= PropertyUtil.setBoolean(getProject(), MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS, ignoreDefaultLibraryPaths
                .getSelection());

        changed |= getProject().setProperty(MoSyncBuilder.ADDITIONAL_LIBRARIES, additionalLibrariesText.getText().replace(';', ','));
        changed |= PropertyUtil.setBoolean(getProject(), MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES, ignoreDefaultLibraries
                .getSelection());

        //changed |= getProject().setProperty(MoSyncProject.EXCLUDE_FILTER_KEY, excludeFiles.getText());
        
        if (outputPath.isEnabled()) {
            changed |= getProject().setProperty(MoSyncBuilder.OUTPUT_PATH, outputPath.getText());
        }

        changed |= PropertyUtil.setBoolean(getProject(), MoSyncBuilder.DEAD_CODE_ELIMINATION, deadCodeElim.getSelection());

        changed |= getProject().setProperty(MoSyncBuilder.PROJECT_TYPE,
                libraryProjectType.getSelection() ? MoSyncBuilder.PROJECT_TYPE_LIBRARY : "");

        changed |= getProject().setProperty(MoSyncBuilder.EXTRA_COMPILER_SWITCHES, gccFlags.getText());
        changed |= getProject().setProperty(MoSyncBuilder.EXTRA_RES_SWITCHES, extraRes.getText());
        changed |= getProject().setProperty(MoSyncBuilder.EXTRA_LINK_SWITCHES, extraLink.getText());

        int gccWarnings = 0;
        gccWarnings |= gccWerror.getSelection() ? MoSyncBuilder.GCC_WERROR : 0;
        gccWarnings |= gccWall.getSelection() ? MoSyncBuilder.GCC_WALL : 0;
        gccWarnings |= gccWextra.getSelection() ? MoSyncBuilder.GCC_WEXTRA : 0;
        
        changed |= PropertyUtil.setInteger(getProject(), MoSyncBuilder.GCC_WARNINGS, gccWarnings);
        
        changed |= PropertyUtil.setBoolean(getProject(), MoSyncBuilder.USE_DEBUG_RUNTIME_LIBS, useDebugRuntimes.getSelection());
        
        if (changed) {
            ProjectBuildAction action = new ProjectBuildAction(getShell(), IncrementalProjectBuilder.FULL_BUILD);
            action.setProject(getProject().getWrappedProject());
            action.run();
        }

        return true;
    }

    private MoSyncProject getProject() {
        IProject wrappedProject = (IProject) getElement();
        MoSyncProject project = MoSyncProject.create(wrappedProject);

        return project;
    }
}
