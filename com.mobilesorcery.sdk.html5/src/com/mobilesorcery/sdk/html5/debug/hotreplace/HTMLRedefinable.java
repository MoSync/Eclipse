package com.mobilesorcery.sdk.html5.debug.hotreplace;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;

import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.RedefinitionResult;
import com.mobilesorcery.sdk.html5.debug.rewrite.ISourceSupport;

public class HTMLRedefinable extends FileRedefinable {

	private List<Pair<Integer, Integer>> htmlRanges;
	private List<Pair<Integer, Integer>> htmlRangesRo;

	public HTMLRedefinable(IRedefinable parent, IFile file, ISourceSupport source) {
		this(parent, file, source, false);
	}

	public HTMLRedefinable(IRedefinable parent, IFile file, ISourceSupport source, boolean deleted) {
		super(parent, file, deleted);
		setSource(source);
	}

	public void setHtmlRanges(List<Pair<Integer, Integer>> htmlRanges) {
		this.htmlRanges = htmlRanges;
		this.htmlRangesRo = Collections.unmodifiableList(htmlRanges);
	}

	public List<Pair<Integer, Integer>> getHtmlRanges() {
		return htmlRangesRo;
	}

	public boolean areHtmlRangesEqual(HTMLRedefinable redefinable) {
		// Hmmmm... we actually perform a byte-by-byte comparison here.
		// Fast enough?
		List<Pair<Integer, Integer>> otherRanges = redefinable.getHtmlRanges();
		if (otherRanges.size() != htmlRanges.size()) {
			return false;
		}
		for (int i = 0; i < htmlRanges.size(); i++) {
			Pair<Integer, Integer> htmlRange = htmlRanges.get(i);
			Pair<Integer, Integer> otherRange = otherRanges.get(i);
			
			String htmlSegment = getSourceRange(htmlRange.first,
					htmlRange.second);
			String otherSegment = redefinable.getSourceRange(otherRange.first,
					otherRange.second);
			if (!htmlSegment.equals(otherSegment)) {
				return false;
			}
		}

		return true;
	}
}
