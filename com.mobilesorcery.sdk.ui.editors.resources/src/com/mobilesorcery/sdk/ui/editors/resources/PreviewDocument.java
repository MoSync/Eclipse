package com.mobilesorcery.sdk.ui.editors.resources;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.TextStyle;


public class PreviewDocument {

	class Snippet {
		final String prefId;
		final String text;

		public Snippet(String prefId, String text) {
			this.prefId = prefId;
			this.text = text;
		}
		
	}

	private ArrayList<Snippet> snippets = new ArrayList<Snippet>();
	private ColorManager manager;
	private StyledText ui;
	private SyntaxColorPreferenceManager prefs;
	private ColorManager colorManager;
	
	public PreviewDocument(SyntaxColorPreferenceManager prefs, ColorManager manager) {
		this.manager = manager;
		this.prefs = prefs;
	}
	
	/**
	 * Adds a code snippet that will be displayed using the style preferences
	 * in pref
	 * @param prefId
	 * @param text
	 */
	public void addSnippet(String prefId, String text) {
		Snippet snippet = new Snippet(prefId, text);
		snippets.add(snippet);
	}

	public void attachUI(StyledText ui) {
		this.ui = ui;
		init();
	}
	
	public void init() {
		StringBuffer text = new StringBuffer();
		for (Snippet snippet : snippets) {
			text.append(snippet.text);
		}
		
		ui.setText(text.toString());
		updateSyntaxColoring(null);
	}

	/**
	 * 
	 * @param updatedPref The pref to update, or <code>null</code> if 
	 * all should be updated
	 */
	public void updateSyntaxColoring(SyntaxColoringPreference updatedPref) {
		int start = 0;
		int end = 0;
		for (Snippet snippet : snippets) {
			end = start + snippet.text.length();

			String prefId = snippet.prefId;
			SyntaxColoringPreference pref = prefId == null ? null : prefs.get(prefId);
			boolean applicable = pref != null && (updatedPref == null || pref.getName().equals(updatedPref.getName()));
			if (applicable) {
				RGB fg = pref.getForeground();
				Color fgColor = manager.setColor(pref.getName() + SyntaxColoringPreference.FG_SUFFIX, fg);
				RGB bg = pref.getBackground();
				Color bgColor = manager.setColor(pref.getName() + SyntaxColoringPreference.BG_SUFFIX, bg);
				int style = (pref.isBold() ? SWT.BOLD : 0) | (pref.isItalic() ? SWT.ITALIC : 0) | (pref.isUnderline() ? SWT.UNDERLINE_SINGLE : 0);
				// TODO: Background
				StyleRange range = new StyleRange(start, end - start, fgColor, null, style);
				range.underline = pref.isUnderline();
				range.strikeout = pref.isStrikethrough();
				ui.setStyleRange(range);
			}
			start = end;
		}
	}
}
