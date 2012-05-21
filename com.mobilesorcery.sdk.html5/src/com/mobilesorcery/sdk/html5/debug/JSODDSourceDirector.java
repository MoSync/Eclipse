package com.mobilesorcery.sdk.html5.debug;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

public class JSODDSourceDirector extends AbstractSourceLookupDirector {

	@Override
	public void initializeParticipants() {
		addParticipants(new ISourceLookupParticipant[] { new JSODDSourceLookupParticipant() });
	}

	@Override
	public boolean isFindDuplicates() {
		return true;
	}
}
