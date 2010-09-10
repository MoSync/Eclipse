package com.mobilesorcery.sdk.product.intro.actions;

import java.util.Properties;

import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class ShowHelpAction implements IIntroAction {

    public void run(IIntroSite site, Properties params) {
        String helpResource = params.getProperty("helpResource");
        boolean showInExternalBrowser = Boolean.parseBoolean(params.getProperty("showInExternalBrowser"));
        MosyncUIPlugin.getDefault().showHelp(helpResource, showInExternalBrowser);
    }

}
