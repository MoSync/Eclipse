package com.mobilesorcery.sdk.html5.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.wst.jsdt.debug.core.model.IJavaScriptStackFrame;
import org.eclipse.wst.jsdt.debug.core.model.IScript;

public class JSODDSourceLookupParticipant extends AbstractSourceLookupParticipant {

	@Override
	public String getSourceName(Object object) throws CoreException {
		if (object instanceof IJavaScriptStackFrame) {
			return ((IJavaScriptStackFrame) object).getSourceName();
		}
		if(object instanceof IScript) {
			return URIUtil.lastSegment(((IScript)object).sourceURI());
		}
		return null;
	}


}
