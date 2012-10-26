package com.mobilesorcery.sdk.ui.internal.help;

import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;

public class SearchBox extends WorkbenchWindowControlContribution implements ISelectionListener {

	private String pendingQuery = null;
	private boolean usePendingQuery = true;

	public SearchBox() {
		
	}

	public SearchBox(String id) {
		super(id);
	}
	
	public void dispose() {
		getWorkbenchWindow().getSelectionService().removePostSelectionListener(this);
	}
	
	@Override
	protected Control createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));
		
		getWorkbenchWindow().getSelectionService().addPostSelectionListener(this);
		
		final Text searchText = new Text(main, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.CANCEL);
		searchText.setLayoutData(new GridData(4 * UIUtils.getDefaultFieldSize() / 5, SWT.DEFAULT));
		
		TextContentAdapter adapter = new TextContentAdapter();
		final Rectangle iconBounds = adapter.getInsertionBounds(searchText);
		final Image searchIcon = MosyncUIPlugin.resize(MosyncUIPlugin.getDefault().getImageRegistry().get(MosyncUIPlugin.IMG_LOOKUP), SWT.DEFAULT, iconBounds.height, false, true);
		
		searchText.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				GC gc = e.gc;
				if (searchText.getText().isEmpty() && !searchText.isFocusControl()) {
					String text = "Search Online Docs";
					
					Point extent = gc.textExtent(text);
					int textX = e.x + (e.width - extent.x) / 2;
					int textY = e.y + (e.height - extent.y) / 2;
					if (searchIcon != null) {
						int iconX = e.x + e.width - iconBounds.width - 20;
						int iconY = e.y + iconBounds.y;
						textX = (e.x + iconX - extent.x) / 2;
						gc.drawImage(searchIcon, iconX, iconY);
					}
					Color oldColor = gc.getForeground();
					gc.setForeground(e.display.getSystemColor(SWT.COLOR_GRAY));
					e.gc.drawText(text, textX, textY);
					gc.setForeground(oldColor);
				}
			}
		});
		
		searchText.addListener(SWT.KeyUp, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.CR && !searchText.getText().isEmpty()) {
					usePendingQuery = true;
					pendingQuery = null;
					URL url = createSearchURL(searchText.getText());
					if (url != null) {
						try {
							PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
						} catch (PartInitException e) {
							Policy.getStatusHandler().show(new Status(IStatus.ERROR, MosyncUIPlugin.PLUGIN_ID, "Cannot load browser"), "Cannot load browser");
						}
					}
				}
			}
			
		});
		
		searchText.addListener(SWT.FocusIn, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (!Util.isEmpty(pendingQuery) && usePendingQuery) {
					searchText.setText(pendingQuery);
					event.display.asyncExec(new Runnable() {
						@Override
						public void run() {
							// Bah. This will be called after mouseup
							searchText.selectAll();
						}						
					});
				}
			}
		});
		searchText.addListener(SWT.MouseUp, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				if (pendingQuery != null && usePendingQuery) {
					pendingQuery = null;
					searchText.selectAll();
				}
			}
		});
		
		searchText.addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				usePendingQuery = !searchText.getText().isEmpty();
			}
		});
		
		return main;
	}

	protected URL createSearchURL(String searchQuery) {
		try {
			return new URL(MessageFormat.format("http://www.mosync.com/content/search-mosync?as_q={0}&src=ide", URLEncoder.encode(searchQuery, "UTF-8")));
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		IContextProvider ctx = (IContextProvider) part.getAdapter(IContextProvider.class);
		if (ctx != null) {
			pendingQuery = ctx.getSearchExpression(selection);
		} else {
			if (selection instanceof ITextSelection) {
				pendingQuery = ((ITextSelection) selection).getText();
			}
		}
	}

}
