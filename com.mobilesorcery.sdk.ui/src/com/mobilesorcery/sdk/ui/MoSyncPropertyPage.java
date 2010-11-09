/*  Copyright (C) 2010 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
 */
package com.mobilesorcery.sdk.ui;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

/**
 * A default property page with convenience methods that many
 * MoSync specific plugins may find useful
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public abstract class MoSyncPropertyPage extends PropertyPage implements IWorkbenchPropertyPage, IUpdatableControl {

    /**
     * Returns the project currently being edited
     * @return
     */
    protected MoSyncProject getProject() {
        IProject wrappedProject = (IProject) getElement();
        MoSyncProject project = MoSyncProject.create(wrappedProject);

        return project;
    }
    
    protected void setText(Text text, String value) {
        text.setText(value == null ? "" : value);
    }
    
    /**
     * Validates a text field with paths.
     * @param shortcurcuit
     * @param fieldName
     * @param text
     * @param possibleParents An array of possible parent (absolute) paths. If <code>null</code> no validation of the existance
     * of all paths will be performed, otherwise it will be performed based on the list of possible parents.
     * @return
     */
    protected IMessageProvider validatePathsField(IMessageProvider shortcurcuit, String fieldName, Text text, IPath[] possibleParentPaths) {
        if (!DefaultMessageProvider.isEmpty(shortcurcuit)) {
            return shortcurcuit;
        }

        String str = text.getText().trim();
        IPath[] paths = PropertyUtil.toPaths(str);
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].toOSString().indexOf(' ') != -1) {
                return new DefaultMessageProvider(
                        MessageFormat.format("\"{0}\": space is an invalid delimiter - use comma (,) instead", fieldName),
                        DefaultMessageProvider.WARNING);
            }
            
            boolean pathExists = possibleParentPaths == null;
            for (int j = 0; !pathExists && j < possibleParentPaths.length; j++) {
                IPath fullPath = possibleParentPaths[j].append(paths[i]);
                pathExists = pathExists |= (fullPath.toFile().exists());
            }
            
            if (!pathExists) {
                return new DefaultMessageProvider(MessageFormat.format("\"{0}\" does not exist", paths[i].toOSString()),
                        DefaultMessageProvider.WARNING);     
            }
        }

        return DefaultMessageProvider.EMPTY;
    }

    protected void setMessage(IMessageProvider message) {
        String messageStr = message == null ? null : message.getMessage();
        int messageType = message == null ? IMessageProvider.NONE : message.getMessageType();
        setMessage(messageStr, messageType);
        setValid(DefaultMessageProvider.isEmpty(message) || message.getMessageType() != IMessageProvider.ERROR);
    }
    
    public void updateUI() {
        validate();
    }
    
    /**
     * Validation code - clients may override.
     */
    protected void validate() {
        
    }
}
