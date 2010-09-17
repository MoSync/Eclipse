package com.mobilesorcery.sdk.product.intro.actions;

import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.config.IIntroXHTMLContentProvider;
import org.w3c.dom.Element;

public class AlwaysWelcomeCheckbox extends org.eclipse.ui.intro.contentproviders.AlwaysWelcomeCheckbox implements IIntroXHTMLContentProvider {

    public void createContent(String s, Element element) {
        boolean alwaysShowIntro = getAlwaysShowIntroPref();
        Element divElement = element.getOwnerDocument().createElement("div");
        Element inputElement = element.getOwnerDocument().createElement("input");
        inputElement.setAttribute("type", "checkbox");
        inputElement.setAttribute("onClick", "window.location=\"http://org.eclipse.ui.intro/runAction?" + 
                "pluginId=com.mobilesorcery.sdk.product&class=" + getClass().getName() + "\"");
        element.appendChild(divElement);
        divElement.appendChild(inputElement);
        divElement.appendChild(element.getOwnerDocument().createTextNode(getText()));
        if(alwaysShowIntro)
        {
            inputElement.setAttribute("checked", "checked");
            PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_INTRO, alwaysShowIntro);
        }
    }

    public String getText() {
        return "Show this screen at startup"; 
    }
}
