package com.mobilesorcery.sdk.deployment;

import org.eclipse.ui.IMemento;

public interface IDeploymentStrategyFactory {

	public IDeploymentStrategy create(IMemento memento);
	
	public void store(IMemento memento, IDeploymentStrategy strategy);

	/**
	 * Creates a new <code>IDeploymentStrategy</code> with
	 * default settings.
	 */
	public IDeploymentStrategy create();
}
