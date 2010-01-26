/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.internal.debug;

import java.util.Comparator;

import org.eclipse.cdt.debug.core.CDIDebugModel;
import org.eclipse.cdt.debug.core.model.ICLineBreakpoint;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * A class that tries to mitigate strange CDT breakpoint
 * behaviour, like for instance a completely ignorant view
 * of matching -break-insert and the result of this -break-insert.
 * Also, double breakpoints are generated upon each new launch
 * (where more events are generated from mdb)
 * @author Mattias Bybro
 *
 */
public class MoSyncBreakpointSynchronizer implements IBreakpointListener {

	public class MoSyncBreakpointComparator implements Comparator<IBreakpoint> {

		public int compare(IBreakpoint bp1, IBreakpoint bp2) {
			if (bp1 instanceof ICLineBreakpoint && bp2 instanceof ICLineBreakpoint) {
				ICLineBreakpoint cbp1 = (ICLineBreakpoint) bp1;
				ICLineBreakpoint cbp2 = (ICLineBreakpoint) bp2;
				
				// TODO: Conditions, etc!? We may very well want several bp's on one line!
				IMarker mbp1 = cbp1.getMarker();
				IMarker mbp2 = cbp1.getMarker();
				
				try {
					int l1 = cbp1.getLineNumber();
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// Just for transient comparisons, so this is ok
			return new Integer(System.identityHashCode(bp1)).compareTo(System.identityHashCode(bp2));
		}

	}

	private MoSyncCDebugTarget target;

	public MoSyncBreakpointSynchronizer() {
	}
	
	public void install() {
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
	}
	
	public void uninstall() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
	}
	
	public void cleanup(IResource resource) {
		/*IBreakpointManager bp = DebugPlugin.getDefault().getBreakpointManager();
		IBreakpoint[] bps = bp.getBreakpoints(CDIDebugModel.getPluginIdentifier());
		try {
			IMarker[] markers = resource.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);
			HashSet<IMarker> orphanedBreakPointMarkers = new HashSet<IMarker>();
			orphanedBreakPointMarkers.addAll(Arrays.asList(markers));
			for (int i = 0; i < bps.length; i++) {
				orphanedBreakPointMarkers.remove(bps[i].getMarker());
			}
			
			for (IMarker marker : orphanedBreakPointMarkers) {
				if (CLineBreakpoint.getMarkerType().equals(marker.getType())) {
					marker.delete();	
				}
			}
			
		} catch (CoreException e1) {
			// Ignore this, we cannot really do anything about it anyways.
		}*/
	}

	public void breakpointAdded(IBreakpoint breakpoint) {
		if (isBreakpointApplicable(breakpoint)) {
			cleanup(breakpoint.getMarker().getResource());
		}
	}

	private boolean isBreakpointApplicable(IBreakpoint breakpoint) {
		return CDIDebugModel.getPluginIdentifier().equals(breakpoint.getModelIdentifier());
	}

	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (isBreakpointApplicable(breakpoint)) {
			cleanup(breakpoint.getMarker().getResource());
		}
	}
	
}
