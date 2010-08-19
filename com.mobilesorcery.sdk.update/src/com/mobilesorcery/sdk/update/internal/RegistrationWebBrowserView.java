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
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.ViewPart;

import com.mobilesorcery.sdk.update.MosyncUpdatePlugin;

// Just because IWebBrowserSupport is useless for our purposes.
public class RegistrationWebBrowserView extends ViewPart {

    static final String VIEW_ID = "com.mobilesorcery.ui.registration.view";

    private Browser browser;

    private URL initialURL;

    public void init(IEditorSite site) throws PartInitException {
        super.setSite(site);
        setPartName("Registration");
    }
    
    public void createPartControl(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
    }

    public Object getAdapter(Class adapter) {
        if (Browser.class.equals(adapter)) {
            return browser;
        }
        
        return null;
    }
    
    public void setFocus() {
    }

    /**
     * Opens the registration view with a set initial URL; if
     * a view is already open, this method will have no effect.
     * @param whereToGo 
     */
    public static IViewPart open(URL whereToGo) throws PartInitException {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IViewPart view = page.showView(VIEW_ID);
        setInitialURL(view, whereToGo);
        return view;
    }

    public static Browser getBrowser(IViewPart view) {
        return (Browser) view.getAdapter(Browser.class);
    }
    
    public static void setInitialURL(IWorkbenchPart part, URL initialURL) {
        if (initialURL != null && part instanceof RegistrationWebBrowserView) {
            RegistrationWebBrowserView registrationView = (RegistrationWebBrowserView) part;
            registrationView.initialURL = initialURL;
            registrationView.browser.setUrl(initialURL.toExternalForm());
        }  
    }
    
    public static URL getInitialURL(IWorkbenchPart part) {
        if (part instanceof RegistrationWebBrowserView) {
            return ((RegistrationWebBrowserView) part).initialURL;
        }
        
        return null;
    }
}
