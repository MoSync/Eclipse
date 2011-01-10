package com.mobilesorcery.sdk.core.build;

import org.eclipse.ui.IMemento;

public interface IBuildStepFactory {

	public void load(IMemento memento);
	
	public void store(IMemento memento);
	
	public IBuildStep create();
	
}

