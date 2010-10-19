package com.mobilesorcery.sdk.profiling.internal;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.profiling.IProfilingListener;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.ProfilingSessionEditorInput;

public class ShowProfilingViewListener implements IProfilingListener {

    private static final ShowProfilingViewListener INSTANCE = new ShowProfilingViewListener();
    
    private ShowProfilingViewListener() {
        
    }
    
    public void handleEvent(ProfilingEventType eventType, final IProfilingSession session) {
        if (eventType == ProfilingEventType.STARTED) {
            Display d = PlatformUI.getWorkbench().getDisplay();
            d.asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    ProfilingSessionEditorInput editorInput = new ProfilingSessionEditorInput(session);
                    if (activePage != null && activePage.findEditor(editorInput) == null) {
                        try {
                            activePage.openEditor(editorInput, "com.mobilesorcery.sdk.profiling.ui.profiling");
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
