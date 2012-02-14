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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.ParameterResolver;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.ui.UpdateListener.IUpdatableControl;

/**
 * A default property page with convenience methods that many
 * MoSync specific plugins may find useful
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public abstract class MoSyncPropertyPage extends PropertyPage implements IWorkbenchPropertyPage, IUpdatableControl {

	private ParameterResolver resolver;

	private final FieldValidationHelper validationHelper = new FieldValidationHelper();

	public MoSyncPropertyPage() {
		if (!shouldHaveDefaultAndApplyButton()) {
			noDefaultAndApplyButton();
		}
	}

	/**
	 * Clients may override
	 * @return {@code false} by default
	 */
    protected boolean shouldHaveDefaultAndApplyButton() {
		return false;
	}

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
     * @return If {@code provider} is {@code null}, will return a standard message provider.
     */
    protected IMessageProvider validatePathsField(ValidationMessageProvider provider, String fieldName, Text text, IPath[] possibleParentPaths) {
        IMessageProvider result = DefaultMessageProvider.EMPTY;

    	String str = text.getText().trim();
        IPath[] paths = PropertyUtil.toPaths(str);
        IPath[] pathsWithQuotes = PropertyUtil.toPaths(str, false);
        for (int i = 0; i < paths.length; i++) {
        	IPath path = paths[i];
        	IPath pathWithQuotes = pathsWithQuotes[i];
        	boolean hasQuotes = !path.equals(pathWithQuotes);
            if (!hasQuotes && path.toOSString().indexOf(' ') != -1) {
            	result = new DefaultMessageProvider(
                        MessageFormat.format("\"{0}\": space is an invalid delimiter - use quotation marks (\") and comma (,) to separate", fieldName),
                        DefaultMessageProvider.ERROR);
            }

            if (DefaultMessageProvider.isEmpty(result)) {
	            boolean pathExists = possibleParentPaths == null;
	            for (int j = 0; !pathExists && j < possibleParentPaths.length; j++) {
	                IPath fullPath = possibleParentPaths[j].append(paths[i]);
	                pathExists = pathExists |= (fullPath.toFile().exists());
	            }

	            if (!pathExists) {
	                result = new DefaultMessageProvider(MessageFormat.format("\"{0}\" does not exist", paths[i].toOSString()),
	                        DefaultMessageProvider.WARNING);
	            }
            }
        }

        if (provider != null) {
        	provider.setMessage(text, result);
        }

        return result;
    }

    protected void setMessage(IMessageProvider message) {
        String messageStr = message == null ? null : message.getMessage();
        int messageType = message == null ? IMessageProvider.NONE : message.getMessageType();
        setMessage(messageStr, messageType);
        setValid(DefaultMessageProvider.isEmpty(message) || message.getMessageType() != IMessageProvider.ERROR);
        if (message instanceof ValidationMessageProvider) {
        	validationHelper.setMessage((ValidationMessageProvider) message);
        }
    }

    protected void addContentAssist(Text text) {
    	if (resolver == null) {
            resolver = MoSyncBuilder.createParameterResolver(getProject(), null);
    	}
		ParameterResolverContentProvider.createProposalProvider(text, resolver);
	}

    @Override
	public void updateUI() {
        validate();
    }

    /**
     * Validation code - clients may override.
     */
    protected void validate() {

    }
}
