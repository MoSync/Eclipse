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
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

public class SyntaxColoringPreferenceComposite extends Composite {

	public class SyntaxColoringLabelProvider extends LabelProvider {
		public String getText(Object element) {
			if (element instanceof SyntaxColoringPreference) {
				return ((SyntaxColoringPreference) element).getDisplayName();
			}
			
			return "" + element;
		}
	}

	static class SyntaxColoringPreferenceContentProvider implements ITreeContentProvider {

		private static final Object[] EMPTY = new Object[0];

		private HashMap<String, Set<SyntaxColoringPreference>> categories = new HashMap<String, Set<SyntaxColoringPreference>>();
		private Set<SyntaxColoringPreference> uncategorized = new TreeSet<SyntaxColoringPreference>(SyntaxColoringPreference.COMPARATOR);

		public SyntaxColoringPreferenceContentProvider(SyntaxColoringPreference[] elements) {
			init(elements);
		}

		private void init(SyntaxColoringPreference[] elements) {
			if (elements == null) {
				return;
			}

			for (int i = 0; i < elements.length; i++) {
				String category = elements[i].getCategory();
				
				Set<SyntaxColoringPreference> children = null;
				
				if (category == null) {
					children = uncategorized;
				} else {
					children = categories.get(category);
					if (children == null) {
						children = new TreeSet<SyntaxColoringPreference>(SyntaxColoringPreference.COMPARATOR);
						categories.put(category, children);
					}
				}

				children.add(elements[i]);
			}
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof String) {
				// Category
				Set<SyntaxColoringPreference> children = categories.get(parentElement);
				return children == null ? EMPTY : children.toArray();
			} else {
				// No sub-categories.
				return EMPTY;
			}
		}

		public Object getParent(Object element) {
			if (element instanceof SyntaxColoringPreference) {
				return ((SyntaxColoringPreference) element).getCategory();
			} else {
				return null;
			}
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		public Object[] getElements(Object inputElement) {
			ArrayList<String> result = new ArrayList(uncategorized);
			result.addAll(new TreeSet<String>(categories.keySet()));
			return result.toArray();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			init((SyntaxColoringPreference[]) newInput);
		}

	}

	private TreeViewer elementsList;
	private boolean supportsBackground = false;
	private boolean showPreview = true;

	private ColorSelector foreground;
	private Button bold;
	private Button italic;
	private ColorSelector background;
	//private Button underline;
	private StyledText preview;
	private SyntaxColoringPreference currentPref;
	private PreviewDocument doc;
	private ColorManager previewColorManager;
	private SyntaxColorPreferenceManager manager;

	public SyntaxColoringPreferenceComposite(Composite parent, int style) {
		super(parent, style);
		initUI();
	}

	public void setPreviewColorManager(ColorManager previewColorManager) {
		this.previewColorManager = previewColorManager;
	}
	
	public void initUI() {
		GridLayout layout = new GridLayout(2, false);
		setLayout(layout);

		elementsList = new TreeViewer(this, SWT.BORDER | SWT.SINGLE);
		elementsList.setLabelProvider(new SyntaxColoringLabelProvider());
		elementsList.setContentProvider(new SyntaxColoringPreferenceContentProvider(null));
		GridData elementsListData = new GridData(GridData.FILL_BOTH);
		elementsListData.verticalSpan = 5;
		elementsList.getControl().setLayoutData(elementsListData);

		ISelectionChangedListener elementChangedListener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				setCurrentElement(selection.getFirstElement());
			}
		};

		elementsList.addSelectionChangedListener(elementChangedListener);

		Composite colorComposite = new Composite(this, SWT.NONE);
		colorComposite.setLayout(new GridLayout(2, false));

		Label foregroundLabel = new Label(colorComposite, SWT.NONE);
		foregroundLabel.setText("Color:");
		foreground = new ColorSelector(colorComposite);

		if (supportsBackground) {
			Label backgroundLabel = new Label(colorComposite, SWT.NONE);
			backgroundLabel.setText("Background:");
			background = new ColorSelector(colorComposite);
		}

		bold = new Button(this, SWT.CHECK);
		bold.setText("Bold");
		italic = new Button(this, SWT.CHECK);
		italic.setText("Italic");
		/*underline = new Button(this, SWT.CHECK);
		underline.setText("Underline");*/

		if (showPreview) {
			Composite previewComposite = new Composite(this, SWT.NONE);
			previewComposite.setLayout(new GridLayout(1, false));
			GridData previewCompositeData = new GridData(GridData.FILL_BOTH);
			previewCompositeData.horizontalSpan = 2;
			previewComposite.setLayoutData(previewCompositeData);
			
			Label previewLabel = new Label(previewComposite, SWT.NONE);
			GridData previewLabelData = new GridData(GridData.FILL_HORIZONTAL);
			previewLabel.setLayoutData(previewLabelData);
			previewLabel.setText("Preview:");

			preview = new StyledText(previewComposite, SWT.BORDER);
			GridData previewData = new GridData(GridData.FILL_BOTH);
			preview.setLayoutData(previewData);
			preview.setEditable(false);
		}

		elementsList.setSelection(new StructuredSelection());

		foreground.addListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				RGB newColor = (RGB) event.getNewValue();
				if (currentPref != null) {
					currentPref.setForeground(newColor);
					refreshPreview();
				}
			}
		});

		if (background != null) {
			background.addListener(new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					RGB newColor = (RGB) event.getNewValue();
					if (currentPref != null) {
						currentPref.setBackground(newColor);
						refreshPreview();
					}
				}
			});
		}

		bold.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (currentPref != null) {
					currentPref.setBold(bold.getSelection());
					refreshPreview();
				}
			}
		});

		italic.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (currentPref != null) {
					currentPref.setItalic(italic.getSelection());
					refreshPreview();
				}
			}
		});

		/*underline.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (currentPref != null) {
					currentPref.setUnderline(underline.getSelection());
					refreshPreview();
				}
			}
		});*/
	}

	public void refreshPreview() {
		if (doc != null) {
			doc.updateSyntaxColoring(currentPref);
		}
	}

	protected void setCurrentElement(Object firstElement) {
		if (firstElement instanceof SyntaxColoringPreference) {
			SyntaxColoringPreference pref = (SyntaxColoringPreference) firstElement;
			currentPref = pref;
			RGB prefForeground = pref.getForeground();
			foreground.setColorValue(prefForeground == null ? PreferenceConverter.COLOR_DEFAULT_DEFAULT : prefForeground);
			foreground.setEnabled(true);
			if (background != null) {
				RGB prefBackground = pref.getBackground();
				background.setColorValue(prefBackground == null ? PreferenceConverter.COLOR_DEFAULT_DEFAULT : prefBackground);
				background.setEnabled(true);
			}

			bold.setSelection(pref.isBold());
			bold.setEnabled(true);
			italic.setSelection(pref.isItalic());
			italic.setEnabled(true);
			//underline.setSelection(pref.isUnderline());
			//underline.setEnabled(true);
		} else {
			// Null or category.
			currentPref = null;
			foreground.setEnabled(false);
			if (background != null) {
				background.setEnabled(false);
			}

			bold.setSelection(false);
			bold.setEnabled(false);
			italic.setSelection(false);
			italic.setEnabled(false);
			//underline.setSelection(false);
			//underline.setEnabled(false);
		}
	}

	public void setSyntaxElements(SyntaxColorPreferenceManager manager) {
		this.manager = manager;
		refreshElementList();
	}

	private void refreshElementList() {
		elementsList.setInput(manager.getAll());
		setCurrentElement(null);
	}

	public void setPreviewDocument(PreviewDocument doc) {
		this.doc = doc;
		doc.attachUI(preview);
	}

	public void refresh() {
		refreshElementList();
		refreshPreview();
	}
	
}
