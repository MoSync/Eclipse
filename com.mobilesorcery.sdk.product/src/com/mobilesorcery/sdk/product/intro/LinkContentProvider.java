package com.mobilesorcery.sdk.product.intro;

import java.io.PrintWriter;
import java.text.MessageFormat;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;
import org.eclipse.ui.intro.config.IIntroXHTMLContentProvider;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class LinkContentProvider implements IIntroXHTMLContentProvider {

	private static final String HELP_PREFIX = "help:/";
	
	private IIntroContentProviderSite site;

	protected void createActionLink(String actionClass, String href,
			String linkText, Element parent, int breaks) {
		Element link = parent.getOwnerDocument().createElement("a");
		link.setAttribute("class", "content-link");
		link.setAttribute("href", MessageFormat.format(
				"http://org.eclipse.ui.intro/runAction?" + "class={0}&"
						+ "pluginId=com.mobilesorcery.sdk.product&"
						+ "href={1}", actionClass, href));
		parent.appendChild(link);
		link.appendChild(parent.getOwnerDocument().createTextNode(linkText));
		createBR(parent, breaks);
	}

	protected void createBR(Element parent, int count) {
		for (int i = 0; i < count; i++) {
			Element br = parent.getOwnerDocument().createElement("br");
			parent.appendChild(br);
		}
	}

	protected void createMoreLink(String moreText, String morePage,
			Element parent) {
		createLink(MessageFormat.format(
				"http://org.eclipse.ui.intro/showPage?id={0}", morePage),
				moreText, parent);
	}

	protected void createHelpLink(String helpText, String helpPage,
			Element parent) {
		createLink(MessageFormat.format(
				"http://org.eclipse.ui.intro/showHelpTopic?id={0}", helpPage),
				helpText, parent);
	}

	protected void createLink(String href, String linkText, Element parent) {
		if (href.startsWith(HELP_PREFIX)) {
			createHelpLink(linkText, href.substring(HELP_PREFIX.length()), parent);
		} else {
			Element link = parent.getOwnerDocument().createElement("a");
			link.setAttribute("class", "content-link");
			link.setAttribute("href", href);
			link.appendChild(parent.getOwnerDocument().createTextNode(linkText));
			parent.appendChild(link);
		}
	}

	protected void createError(String error, boolean reload, Element parent) {
		Element p = parent.getOwnerDocument().createElement("p");
		p.appendChild(parent.getOwnerDocument().createTextNode(error));
		parent.appendChild(p);
	}

	public void createContent(String id, PrintWriter out) {
	}

	public void createContent(String id, Composite parent, FormToolkit toolkit) {
	}

	public void dispose() {
	}

	public void init(IIntroContentProviderSite site) {
		this.site = site;
	}

	protected IIntroContentProviderSite getSite() {
		return site;
	}
}
