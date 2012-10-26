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
package com.mobilesorcery.sdk.ui.targetphone;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;

public interface ITargetPhoneTransportDelegate {

	final static String NAME_ATTR = "name";

	final static String LEGACY_PROFILE_ATTR = "profile";

	final static String DEFAULT_PROFILE_ATTR = "platform.profile";

	/**
	 * Stores information about a target phone that may later be loaded
	 * by the <code>load</code> method. The name and target profile need
	 * not be stored, as this is already stored by the framework.
	 * @param phone The phone to store in the memento
	 * @param memento The <code>IMemento</code> to use for storing the phone's data
	 * @return
	 */
	public boolean store(ITargetPhone phone, IMemento memento);

	public ITargetPhone load(IMemento memento, String name);

	/**
	 * Sends a file to a device and may launch it, if the device
	 * permits it.
	 * @param shell
	 * @param project
	 * @param phone
	 * @param variant 
	 * @param packageToSend
	 * @param monitor
	 * @throws CoreException
	 */
	public void send(IShellProvider shell, MoSyncProject project, IBuildVariant variant, ITargetPhone phone, File packageToSend, IProgressMonitor monitor) throws CoreException;

	public ITargetPhone scan(IShellProvider shell, IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns a filter indicating which profiles this transport
	 * supports.
	 * @return <code>null</code> if accepts all profiles
	 */
	public IDeviceFilter getAcceptedProfiles();

	public String getDescription(String context);

	/**
	 * Returns <code>true</code> if this transport is available;
	 * if for example a certain piece of hardware is not available,
	 * then this may return <code>false</code>.
	 * @return
	 */
	public boolean isAvailable();

}
