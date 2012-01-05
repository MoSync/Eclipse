package com.mobilesorcery.sdk.wizards.internal;

import org.eclipse.jface.resource.ImageDescriptor;

import com.mobilesorcery.sdk.wizards.IProjectTemplateExtensionDelegate;

public interface IProjectTemplateExtension extends IProjectTemplateExtensionDelegate {

	ImageDescriptor getImage();

	String getName();

	String getType();

}
