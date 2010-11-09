package com.mobilesorcery.sdk.product.intro.actions;

import java.net.URL;
import java.util.Properties;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class ExternalLinkAction implements IIntroAction {

    public void run(IIntroSite site, Properties params) {
        String href = params.getProperty("href");
        if (!Util.isEmpty(href)) {
            try {
                String url = Util.toGetUrl(href, MosyncUIPlugin.getDefault().getVersionParameters(false));
                IWebBrowser wb = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                wb.openURL(new URL(url));
            } catch (Exception e) {
                CoreMoSyncPlugin.getDefault().log(e);
            }
        }
    }

}
