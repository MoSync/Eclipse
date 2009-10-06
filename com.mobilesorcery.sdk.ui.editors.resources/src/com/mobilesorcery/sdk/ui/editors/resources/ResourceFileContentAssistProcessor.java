/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.ui.editors.resources;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Point;

public class ResourceFileContentAssistProcessor implements IContentAssistProcessor {

	private static final char[] AUTO_ACTIVATION_CHARS = new char[] { '.' };

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument doc = viewer.getDocument();
		Point selectedRange = viewer.getSelectedRange();

		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

		if (selectedRange.y == 0) {
			String qualifier = getQualifier(doc, offset);
			computeProposals(qualifier, offset, proposals);
		}

		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return AUTO_ACTIVATION_CHARS;
	}
	
	private void computeProposals(String qualifier, int offset, List<ICompletionProposal> proposals) {
		int qualifierLength = qualifier.length();

		for (int i = 0; i < ResourcesFileScanner.DIRECTIVES.length; i++) {
			if (ResourcesFileScanner.DIRECTIVES[i].startsWith(qualifier) && qualifier.length() > 0) {
				int cursor = ResourcesFileScanner.DIRECTIVES[i].length();

				CompletionProposal proposal = new CompletionProposal(ResourcesFileScanner.DIRECTIVES[i], offset - qualifierLength, qualifierLength, cursor);

				proposals.add(proposal);
			}
		}
	}

	private String getQualifier(IDocument doc, int offset) {
		StringBuffer buf = new StringBuffer();
		while (true) {
			try {
				char c = doc.getChar(--offset);

				if (Character.isWhitespace(c))
					return "";

				buf.append(c);

				if (c == '.')
					return buf.reverse().toString();
			} catch (BadLocationException e) {
				return "";
			}
		}
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

}
