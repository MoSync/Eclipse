package com.mobilesorcery.sdk.profiling.ui.views;

import java.util.Comparator;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingListener;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.ProfilingPlugin;

public class ProfilingView extends ViewPart {
    
    public final static String ID = "com.mobilesorcery.sdk.profiling.ui.profiling";

    public class ProfilingListener implements IProfilingListener {
        public void handleEvent(ProfilingEventType eventType, final IProfilingSession session) {
            if (profilingComposite != null && eventType == ProfilingEventType.STOPPED) {
                profilingComposite.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        profilingComposite.setInput(session.getInvocation());
                    }                    
                });
            }
        }
    }

	private ProfilingComposite profilingComposite;
    private ProfilingListener profilingEventListener;

	public ProfilingView() {
	    profilingEventListener = new ProfilingListener();
	}
	
	public void createPartControl(Composite parent) {
	    profilingComposite = new ProfilingComposite(parent, SWT.FLAT);
	   	ProfilingPlugin.getDefault().addProfilingListener(profilingEventListener);
	}

    public void dispose() {
	    ProfilingPlugin.getDefault().removeProfilingListener(profilingEventListener);
	    super.dispose();
	}
	
	public void setFocus() {
		profilingComposite.setFocus();
	}
}