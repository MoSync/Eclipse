package com.mobilesorcery.sdk.profiling.internal;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.profiling.IProfilingListener;
import com.mobilesorcery.sdk.profiling.IProfilingSession;

public class ShowProfilingViewListener implements IProfilingListener {

    private static final ShowProfilingViewListener INSTANCE = new ShowProfilingViewListener();
    
    private ShowProfilingViewListener() {
        
    }
    
    public void handleEvent(ProfilingEventType eventType, IProfilingSession session) {
        if (eventType == ProfilingEventType.STARTED) {
            Display d = PlatformUI.getWorkbench().getDisplay();
            d.asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    if (activePage != null && activePage.findView("com.mobilesorcery.sdk.profiling.ui.profiling") == null) {
                        try {
                            activePage.showView("com.mobilesorcery.sdk.profiling.ui.profiling");
                        } catch (PartInitException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        }
    }

    public static IProfilingListener getInstance() {
        return INSTANCE;
    }

}
