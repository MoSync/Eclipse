package com.mobilesorcery.sdk.product.intro;

import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Element;

import com.mobilesorcery.sdk.product.intro.IntroInitializer.ICallback;

public abstract class LinkListContentProvider extends LinkContentProvider {

	public void createContent(String id, Element parent) {
		boolean shortList = id.endsWith(IntroLink.SHORT_LIST);

		IntroInitializer initializer = IntroInitializer.getInstance();
		switch (initializer.getState()) {
		case IntroInitializer.INIT:
			final Display d = Display.getCurrent();
			IntroInitializer.getInstance().fetch(new ICallback() {
				public void done() {
					d.asyncExec(new Runnable() {
						public void run() {
							getSite().reflow(LinkListContentProvider.this, true);
						}
					});
				}
			});
			break;
		case IntroInitializer.FETCHED:
			onFetch(parent, id, shortList);
			break;
		case IntroInitializer.ERROR:
			createError(getLoadErrorMessage(), false, parent);
			break;
		default:
		}
	}

	protected void onFetch(Element parent, String id, boolean shortList) {
		IntroInitializer initializer = IntroInitializer.getInstance();
		List<IntroLink> links = initializer.getLinks(getLinkType());
		int onShortList = 0;
		for (int i = 0; links != null && i < links.size(); i++) {
			IntroLink link = links.get(i);
			if (!shortList || link.getScopes().contains(IntroLink.SHORT_LIST)) {
				createActionLink(link, parent);
				onShortList++;
			}
		}

		if (shortList && onShortList < links.size()) {
			createMoreLink(parent);
		}
	}

	protected abstract String getLoadErrorMessage();

	protected abstract void createMoreLink(Element parent);

	protected abstract String getLinkType();
	
	protected abstract void createActionLink(IntroLink link, Element parent);
}
