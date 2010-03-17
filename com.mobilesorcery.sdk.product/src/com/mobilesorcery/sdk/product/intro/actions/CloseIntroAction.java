package com.mobilesorcery.sdk.product.intro.actions;

import java.util.Properties;

import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;

public class CloseIntroAction implements IIntroAction {

    public void run(IIntroSite site, Properties params) {
        IIntroPart part = site.getWorkbenchWindow().getWorkbench().getIntroManager().getIntro();
        site.getWorkbenchWindow().getWorkbench().getIntroManager().closeIntro(part);
    }

}
