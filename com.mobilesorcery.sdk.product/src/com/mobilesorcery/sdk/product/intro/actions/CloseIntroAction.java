package com.mobilesorcery.sdk.product.intro.actions;

import java.util.Properties;

import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class CloseIntroAction implements IIntroAction {

    public void run(IIntroSite site, Properties params) {
        MosyncUIPlugin.getDefault().closeIntro(site);
    }

}
