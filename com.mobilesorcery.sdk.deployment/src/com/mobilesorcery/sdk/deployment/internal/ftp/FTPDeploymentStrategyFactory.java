package com.mobilesorcery.sdk.deployment.internal.ftp;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.deployment.IDeploymentStrategy;
import com.mobilesorcery.sdk.deployment.IDeploymentStrategyFactory;

public class FTPDeploymentStrategyFactory implements IDeploymentStrategyFactory {

	public IDeploymentStrategy create(IMemento memento) {
		FTPDeploymentStrategy strategy = new FTPDeploymentStrategy();
		strategy.setHost(memento.getString("host"));
		strategy.setRemotePath(memento.getString("path"));
		strategy.setUsername(memento.getString("u"));
		strategy.setPassword(memento.getString("p"));
		return strategy;
	}

	public void store(IMemento memento, IDeploymentStrategy strategy) {
		if (strategy instanceof FTPDeploymentStrategy) {
			FTPDeploymentStrategy ftpStrategy = (FTPDeploymentStrategy) strategy;
			memento.putString("host", ftpStrategy.getHost());
			memento.putString("path", ftpStrategy.getRemotePath());
			memento.putString("u", ftpStrategy.getUsername());
			memento.putString("p", ftpStrategy.getPassword());
		}
	}

	public IDeploymentStrategy create() {
		return new FTPDeploymentStrategy();
	}

}
