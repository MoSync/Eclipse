package com.mobilesorcery.sdk.core.templates;

import org.eclipse.jface.resource.ImageDescriptor;


public interface IProjectTemplateExtension extends IProjectTemplateExtensionDelegate {

	ImageDescriptor getImage();

	String getName();

	String getType();

}
