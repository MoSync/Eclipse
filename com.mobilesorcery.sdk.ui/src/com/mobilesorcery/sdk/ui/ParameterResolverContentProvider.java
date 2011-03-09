package com.mobilesorcery.sdk.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.ParameterResolver;

public class ParameterResolverContentProvider implements IContentProposalProvider {

	private ParameterResolver resolver;

	public ParameterResolverContentProvider(ParameterResolver resolver) {
		this.resolver = resolver;
	}
	
	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		// Find last % sign
		boolean foundToken = false;
		char ch = '\0';
		int ix = Math.min(position, contents.length() - 1);
		while (ix >= 0 && !foundToken) {
			ch = contents.charAt(ix);
			foundToken = ch == '%';
			ix--;
		}
		
		// +1 to remove the % sign
		String potentialMatch = contents.substring(ix + 1, position);
		String match = foundToken ? potentialMatch : "";
		String param = ParameterResolver.getParameter(potentialMatch);
		if (param == null) {
		    return getPrefixProposals(match);
		} else {
			return getParameterProposals(potentialMatch);
		}
	}

	private IContentProposal[] getPrefixProposals(String match) {
		ArrayList<IContentProposal> filteredPrefixes = new ArrayList<IContentProposal>();
		for (String prefix : resolver.listPrefixes()) {
			prefix = "%" + prefix + (prefix.endsWith(":") ? "" : "%");
			if (prefix.startsWith(match)) {
				filteredPrefixes.add(new ContentProposal(
						prefix.substring(match.length()),
						prefix, null));
			}
		}
		return filteredPrefixes.toArray(new IContentProposal[filteredPrefixes.size()]);
	}

	private IContentProposal[] getParameterProposals(String match) {
		String matchWithoutPerc = match.length() > 0 && match.charAt(0) == '%' ? match.substring(1) : match;
		String prefix = ParameterResolver.getPrefix(matchWithoutPerc);
		
		ArrayList<IContentProposal> filteredPrefixes = new ArrayList<IContentProposal>();
		List<String> availableParams = resolver.listAvailableParameters(prefix);
		if (availableParams != null) {
			for (String availableParam : availableParams) {
				String completion = "%" + prefix + availableParam + "%";
				if (completion.startsWith(match)) {
					filteredPrefixes.add(new ContentProposal(
							completion.substring(match.length()),
							completion, null));
				}
			}
		}
		return filteredPrefixes.toArray(new IContentProposal[filteredPrefixes.size()]);
	}

	/**
	 * Configures a text field to accept content proposals from a {@link ParameterResolver()}
	 * @param text
	 * @param resolver
	 * @return
	 */
	public static ParameterResolverContentProvider createProposalProvider(Text text, ParameterResolver resolver) {
		ParameterResolverContentProvider provider = new ParameterResolverContentProvider(resolver);
		try {
			char[] autoActivationCharacters = new char[] { '%', ':' };
			KeyStroke keyStroke = KeyStroke.getInstance("Ctrl+Space");
			ContentProposalAdapter adapter = new ContentProposalAdapter(
			text, new TextContentAdapter(),
			provider,
			keyStroke, autoActivationCharacters);
		} catch (ParseException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
		
		return provider;
	}
}
