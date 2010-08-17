package com.mobilesorcery.sdk.update.internal;

import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ILocalWorkingSetManager;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

// Just because IWebBrowserSupport is useless for our purposes.
public class RegistrationWebBrowserEditor extends EditorPart {

    static class InternalEditorInput implements IEditorInput {
        private URL initialURL;
        private String title;

        public InternalEditorInput(String title, URL initialURL) {
            this.title = title;
            this.initialURL = initialURL;
        }
        
        public URL getInitialURL() {
            return initialURL;
        }

        public boolean exists() {
            return true;
        }

        public ImageDescriptor getImageDescriptor() {
            return null;
        }

        public String getName() {
            return title;
        }

        public IPersistableElement getPersistable() {
            return null;
        }

        public String getToolTipText() {
            return title;
        }

        public Object getAdapter(Class adapter) {
            return null;
        }
        
        public boolean equals(Object o) {
            if (o instanceof InternalEditorInput) {
                return getInitialURL().equals(((InternalEditorInput) o).getInitialURL());
            }
            
            return false;
        }
        
        public int hashCode() {
            return initialURL.hashCode();
        }
    }

    private static final String EDITOR_ID = "com.mobilesorcery.ui.registration.editor";

    private URL initialURL;

    private Browser browser;

    public void doSave(IProgressMonitor iprogressmonitor) {
    }

    public void doSaveAs() {
    }

    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        setSite(site);
        setInput(editorInput);
        if (editorInput instanceof InternalEditorInput) {
            this.initialURL = ((InternalEditorInput) editorInput).getInitialURL();
        } else {
            throw new PartInitException("This editor input is not valid!");
        }
    }

    public boolean isDirty() {
        return false;
    }
    
    public boolean isSaveAsAllowed() {
        return false;
    }

    public void createPartControl(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
        browser.setUrl(initialURL.toExternalForm());
    }

    public Object getAdapter(Class adapter) {
        if (Browser.class.equals(adapter)) {
            return browser;
        }
        
        return null;
    }
    
    public void setFocus() {
    }

    public static IEditorPart openURL(String title, URL initialURL) throws PartInitException {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart editor = page.openEditor(new InternalEditorInput(title, initialURL), EDITOR_ID);
        return editor;
    }

    public static Browser getBrowser(IEditorPart editor) {
        return (Browser) editor.getAdapter(Browser.class);
    }
    
    public static URL getInitialURL(IEditorInput input) {
        if (input instanceof InternalEditorInput) {
            return ((InternalEditorInput) input).getInitialURL();
        }
        
        return null;
    }
}
