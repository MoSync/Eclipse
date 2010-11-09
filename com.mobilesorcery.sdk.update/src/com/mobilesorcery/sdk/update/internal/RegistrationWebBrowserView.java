package com.mobilesorcery.sdk.update.internal;

import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

// Just because IWebBrowserSupport is useless for our purposes.
public class RegistrationWebBrowserView extends ViewPart {

    static final String VIEW_ID = "com.mobilesorcery.ui.registration.view";

    private Browser browser;

    private URL initialURL;

    private RegistrationPartListener currentListener;

    private boolean active;

    public void init(IEditorSite site) throws PartInitException {
        super.setSite(site);
        setPartName("Registration");
        active = true;
    }
    
    public void createPartControl(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
        // If the user opens manually; we'd like to have
        // a reasonable default
        browser.setUrl(DefaultUpdater2.getInitialURL());
    }

    public Object getAdapter(Class adapter) {
        if (Browser.class.equals(adapter)) {
            return browser;
        }
        
        return null;
    }
    
    public void setFocus() {
    }

    public void dispose() {
        active = false;
        super.dispose();
    }
    
    /**
     * Opens the registration view with a set initial URL; if
     * a view is already open, this method will have no effect.
     * @param whereToGo 
     * @param reopenIntro2 
     */
    public static RegistrationWebBrowserView open(URL whereToGo, boolean reopenIntroHint) throws PartInitException {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IViewPart view = page.showView(VIEW_ID);
        if (!(view instanceof RegistrationWebBrowserView)) {
            throw new PartInitException("Excepted view to be mosync registration view");
        }
        
        RegistrationWebBrowserView registrationView = (RegistrationWebBrowserView) view;
        registrationView.registerListener(reopenIntroHint);
        if (registrationView.initialURL == null && !registrationView.active) {
            setInitialURL(view, whereToGo);
        }
        
        return registrationView;
    }

    private void registerListener(boolean reopenIntro) {
        RegistrationPartListener listener = new RegistrationPartListener(this, reopenIntro);
        if (currentListener == null) {
            currentListener = listener;
            getSite().getPage().addPartListener(listener);
            getSite().getWorkbenchWindow().addPerspectiveListener(listener);
        }
        currentListener.setReopenIntro(reopenIntro);
    }

    public static Browser getBrowser(IViewPart view) {
        return (Browser) view.getAdapter(Browser.class);
    }
    
    static void setInitialURL(IWorkbenchPart part, URL initialURL) {
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

    public boolean isActive() {
        return active;
    }
}
