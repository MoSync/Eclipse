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
package com.mobilesorcery.sdk.importproject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

/**
 * 
 * @author Mattias Bybro
 * @deprecated Modify this to ignore .mopro files, until then it's deprecated.
 */
public class FindProjectsRunnable implements IRunnableWithProgress {
    
    public static final String FINISHED = "finished"; //$NON-NLS-1$

    private PropertyChangeSupport listeners = new PropertyChangeSupport(this);
    
    private File root;
    private Set<File> projectDescriptionFiles = new HashSet<File>();

    public FindProjectsRunnable(File root) {
        this.root = root;
    }

    public File[] getProjectDescriptionFiles() {
        return projectDescriptionFiles.toArray(new File[projectDescriptionFiles.size()]);
    }
    
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
            monitor.beginTask(Messages.FindProjectsRunnable_ScanningDirectories, IProgressMonitor.UNKNOWN);
            findInDirectory(monitor, root);
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        }
    }

    public void findInDirectory(IProgressMonitor monitor, File root) throws IOException {
        try {
            findInDirectory(monitor, root, projectDescriptionFiles, new HashSet<String>());
        } finally {
            listeners.firePropertyChange(new PropertyChangeEvent(this, FINISHED, null, getProjectDescriptionFiles()));
        }
    }
    
    private void findInDirectory(IProgressMonitor monitor, File dir, Set<File> projectDescriptionFiles, Set<String> directoriesVisited) throws IOException {
        monitor.setTaskName(Messages.FindProjectsRunnable_ScanningProgress + dir.getAbsolutePath());
        if (monitor.isCanceled()) {
            return;
        }
        
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            String ext = Util.getExtension(files[i]);
            if (files[i].isDirectory() && !isTemplateDirectory(files[i])) {
                if (directoriesVisited.contains(files[i].getCanonicalPath())) {
                    return;
                }
                
                directoriesVisited.add(files[i].getCanonicalPath());
                findInDirectory(monitor, files[i], projectDescriptionFiles, directoriesVisited);
            } else if ("msp".equals(ext) || "mopro".equals(ext)) { //$NON-NLS-1$ //$NON-NLS-2$
                projectDescriptionFiles.add(files[i]);                
            }
        }
    }

    private boolean isTemplateDirectory(File file) {
		File templateDir = MoSyncTool.getDefault().getTemplatesPath().toFile();
		return file.equals(templateDir);
	}

	public void addFinishedListener(PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(listener);
    }

    public void removeFinishedListener(PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
    }

}
