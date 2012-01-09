package com.mobilesorcery.sdk.ui;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.Profile;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;
import com.mobilesorcery.sdk.ui.internal.actions.ProfileListContentProvider;

public class PlatformSelectionComposite implements Listener,
		ISelectionChangedListener, IOpenListener {
	public class ProfileTextFilter extends ViewerFilter {

		private final String pattern;
		private final HashMap<Object, Boolean> match = new HashMap<Object, Boolean>();

		public ProfileTextFilter(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			return doesMatch(element);
		}

		private boolean doesMatch(Object element) {
			Boolean match = this.match.get(element);
			if (match != null) {
				return match;
			}
			match = innerMatch(element);
			this.match.put(element, match);
			return match;
		}

		private boolean innerMatch(Object element) {
			if (element instanceof IProfile) {
				IProfile profile = (IProfile) element;
				String name = MoSyncTool.toString(profile);
				String[] subPatterns = pattern.split("\\s+");
				// AND or OR? Let's go for AND.
				for (String subPattern : subPatterns) {
					if (!name.toLowerCase().contains(subPattern.toLowerCase())) {
						return false;
					}
				}
				return true;
			} else if (element instanceof IVendor) {
				IVendor vendor = (IVendor) element;
				for (IProfile profile : vendor.getProfiles()) {
					if (doesMatch(profile)) {
						return true;
					}
				}
				return false;
			}
			return true;
		}

	}

	public class RichProfileLabelProvider extends OwnerDrawLabelProvider {

		private int mode = MoSyncTool.DEFAULT_PROFILE_TYPE;

		public void setMode(int mode) {
			this.mode = mode;
		}

		@Override
		protected void measure(Event event, Object obj) {
			event.setBounds(internalPaint(event, obj, false));
		}

		@Override
		protected void paint(Event event, Object obj) {
			internalPaint(event, obj, true);
		}

		protected Rectangle internalPaint(Event event, Object obj,
				boolean doPaint) {
			Rectangle newBounds = ((TableItem) event.item)
					.getBounds(event.index);
			Rectangle bounds = new Rectangle(newBounds.x, newBounds.y, newBounds.width, newBounds.height); //event.getBounds();
			int width = 0;
			int height = 0;

			String mainText = "";
			String subText = "";
			Point imageSize = new Point(16, 16);
			int indentX = imageSize.x + 5;
			GC gc = event.gc;
			boolean drawLine = false;

			boolean isTargetProfile = false;
			Font originalFont = gc.getFont();
			Font boldFont = MosyncUIPlugin.getDefault().getFont(
					MosyncUIPlugin.FONT_DEFAULT_BOLD);
			if (obj instanceof IProfile) {
				IProfile profile = (IProfile) obj;
				mainText = profile.getName();
				isTargetProfile = profile != null && profile.equals(project.getTargetProfile());
				subText = getProfileDescription(project, profile);
				if (subText.equals(mainText))
					subText = "";
			} else if (obj instanceof IVendor) {
				IVendor platform = (IVendor) obj;
				Image image = MosyncUIPlugin.getDefault().getPlatformImage(
						platform, imageSize);
				height = image.getBounds().height + 2;
				if (doPaint) {
					gc.drawImage(image, bounds.x, bounds.y);
					drawLine = true;
				}
				mainText = platform.getName();
			}
			if (obj instanceof IVendor || isTargetProfile) {
				gc.setFont(boldFont);
			}
			Point mainTextExtent = gc.textExtent(mainText);
			Point subTextExtent = subText.length() == 0 ? new Point(0, 0) : gc
					.textExtent(subText);
			Point actualTextExtent = new Point(mainTextExtent.x,
					subTextExtent.y + mainTextExtent.y);
			width = indentX + actualTextExtent.x;
			height = Math.max(height, actualTextExtent.y);
			Color black = event.display.getSystemColor(SWT.COLOR_BLACK);
			Color gray = event.display.getSystemColor(SWT.COLOR_DARK_GRAY);
			if (doPaint) {
				gc.setForeground(black);
				if (isTargetProfile) {
					String checkmark = Util.isMac() ? "\u2713" : "*";
					gc.drawText(checkmark, bounds.x + 8, bounds.y, true);
				}
				gc.drawText(mainText, bounds.x + indentX, bounds.y, true);
				gc.setForeground(gray);
				gc.drawText(subText, bounds.x + indentX, bounds.y
						+ mainTextExtent.y, true);
				if (drawLine) {
					gc.drawLine(bounds.x, bounds.y + height,
							bounds.x + UIUtils.getDefaultFieldSize(), bounds.y
									+ height);
				}
				gc.setFont(originalFont);
			}

			newBounds.width = width;
			newBounds.height = height;
			return newBounds;
		}

		private String getProfileShortDescription(MoSyncProject project, IProfile profile) {
			return profile.getPackager().getShortDescription(project, profile);
		}

		private String getProfileDescription(MoSyncProject project, IProfile profile) {
			if (mode == MoSyncTool.LEGACY_PROFILE_TYPE) {
				IProfile platformProfile = matchLegacyProfile(project, profile);
				if (platformProfile != null) {
					return MoSyncTool.toString(platformProfile);
				} else {
					List<IProfile> filteredOutProfile = MoSyncTool.getDefault()
							.getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE)
							.getProfilesForRuntime(profile.getRuntime());
					if (filteredOutProfile != null
							&& filteredOutProfile.size() > 0) {
						return MessageFormat.format("(Filtered out - {0})",
								MoSyncTool.toString(filteredOutProfile.get(0)));
					}
				}
				return MessageFormat.format("({0}: Unknown runtime",
						Profile.getAbbreviatedPlatform(profile));
			} else {
				return getProfileShortDescription(project, profile);
			}
		}

	}

	private final Control control;
	private MoSyncProject project;
	private Text filterBox;
	private boolean filterShown;
	private TableViewer profileTable;
	private int mode = -1;
	private RichProfileLabelProvider profileLabelProvider;
	private IProfile currentProfile;
	private final int style;
	private Composite inner;

	private static Shell CURRENT_SHELL;

	public PlatformSelectionComposite(Control control, int style) {
		this.control = control;
		this.style = style;
	}

	public void setProject(MoSyncProject project) {
		this.project = project;
		setMode(project.getProfileManagerType());
	}

	private boolean hasSearchBox() {
		return (style & SWT.SEARCH) != 0;
	}

	private boolean alwaysShowSearchBox() {
		return (style & SWT.BACKGROUND) == 0;
	}

	protected Composite createContentArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(UIUtils.newPrefsLayout(1));
		inner = new Composite(main, SWT.NONE);
		resize(UIUtils.getDefaultFieldSize(), UIUtils.getDefaultListHeight());
		filterBox = new Text(inner, SWT.SEARCH);
		filterBox.addListener(SWT.KeyUp, this);
		filterBox.addListener(SWT.KeyDown, this);

		profileTable = new TableViewer(inner, SWT.NONE);
		profileLabelProvider = new RichProfileLabelProvider();
		profileTable.setLabelProvider(profileLabelProvider);
		profileTable.setContentProvider(new ProfileListContentProvider());
		GridData profileTableData = new GridData(GridData.FILL_BOTH);
		profileTableData.grabExcessVerticalSpace = true;
		profileTableData.verticalAlignment = SWT.TOP;
		profileTable.getControl().setLayoutData(profileTableData);
		profileTable.getControl().setFocus();
		profileTable.getControl().addListener(SWT.KeyDown, this);
		profileTable.addSelectionChangedListener(this);
		profileTable.addOpenListener(this);
		inner.setBackground(profileTable.getControl().getBackground());
		setMode(mode, true);
		showFilterBox(hasSearchBox() && alwaysShowSearchBox());
		return main;
	}

	private void showFilterBox(boolean show) {
		filterShown = show;
		/*setMode(show ? MoSyncTool.LEGACY_PROFILE_TYPE
				: MoSyncTool.DEFAULT_PROFILE_TYPE);*/
		GridData data = show ? new GridData(SWT.FILL, SWT.NONE, true, false)
				: new GridData(0, 0);
		filterBox.setLayoutData(data);
		filterBox.getParent().layout();
		filterBox.getParent().redraw();
	}

	public void show(int style) {
		boolean asDropdown = (style & SWT.DROP_DOWN) != 0;
		close(false);
		int shellStyle = (asDropdown ? SWT.ON_TOP | SWT.TOOL : SWT.TITLE | SWT.CLOSE);
		final Shell shell = new Shell(control.getShell(), shellStyle);
		shell.setText("Select profile");
		CURRENT_SHELL = shell;
		shell.setLayout(UIUtils.newPrefsLayout(1));
		createContentArea(shell);
		shell.pack();
		if (asDropdown) {
			Point controlLocation = control.toDisplay(asDropdown ? control.getLocation() : new Point(0, 0));
			shell.setLocation(controlLocation.x,
					controlLocation.y + control.getSize().y);
		} else {
			UIUtils.centerShell(shell);
		}
		attachListeners(shell, asDropdown);
		shell.open();
	}

	private void close(boolean saveTargetProfile) {
		if (CURRENT_SHELL != null && !CURRENT_SHELL.isDisposed()) {
			CURRENT_SHELL.close();
		}
		CURRENT_SHELL = null;
		if (saveTargetProfile && currentProfile != null) {
			project.setTargetProfile(currentProfile);
		}
	}

	private void attachListeners(Shell shell, boolean asDropdown) {
		if (asDropdown) {
			shell.addListener(SWT.Deactivate, this);
		} else {
			shell.addListener(SWT.Resize, this);
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (event.widget == profileTable.getControl()
				&& event.type == SWT.KeyDown
				&& shouldCharBeSentToSearchBox(event.character)) {
			if (!filterShown) {
				showFilterBox(hasSearchBox());
			}
			if (event.character == SWT.BS) {
				filterBox.setText(filterBox.getText(0, Math.max(0, filterBox.getText().length()-2)));
			} else {
				filterBox.append("" + event.character);
			}
			filterBox.setSelection(filterBox.getText().length());
			filterBox.forceFocus();
		} else if (event.widget == filterBox && event.type == SWT.KeyDown) {
			if (event.keyCode == SWT.ARROW_DOWN
					|| event.keyCode == SWT.ARROW_UP) {
				profileTable.getControl().forceFocus();
				if (profileTable.getSelection().isEmpty()
						&& profileTable.getElementAt(0) != null) {
					profileTable.getTable().select(0);
				}
			}
		} else if (event.widget == filterBox && event.type == SWT.KeyUp && hasSearchBox()) {
			String filterText = filterBox.getText();
			profileTable.setFilters(new ViewerFilter[] { new ProfileTextFilter(
					filterText) });
			if (filterText.length() == 0) {
				showFilterBox(alwaysShowSearchBox());
				profileTable.getControl().forceFocus();
			}
		} else if (event.widget == CURRENT_SHELL
				&& event.type == SWT.Deactivate) {
			close(false);
		} else if (event.widget == CURRENT_SHELL && event.type == SWT.Resize) {
			resize(CURRENT_SHELL.getBounds().width, CURRENT_SHELL.getBounds().height);
		} else if (event.keyCode == SWT.ESC
				&& (event.type == SWT.KeyDown || event.type == SWT.KeyUp)) {
			close(false);
		}
	}

	private void resize(int width, int height) {
		GridLayout innerData = UIUtils.newPrefsLayout(1);
		innerData.verticalSpacing = 0;
		inner.setLayout(innerData);
		inner.setLayoutData(new GridData(width, height));
		inner.setSize(width, height);
		inner.getParent().layout(true, true);
	}

	private boolean shouldCharBeSentToSearchBox(char ch) {
		return Character.isLetterOrDigit(ch) || (ch == SWT.BS && !filterBox.getText().isEmpty());
	}

	public void setMode(int profileManagerType) {
		setMode(profileManagerType, false);
	}

	private void setMode(int profileManagerType, boolean force) {
		if (force || (mode != profileManagerType && profileTable != null)) {
			profileLabelProvider.setMode(profileManagerType);
			if (profileManagerType == MoSyncTool.LEGACY_PROFILE_TYPE) {
				// The content provider uses the mosync tool to get
				// 'legacy' devices.
				profileTable.setInput(MoSyncTool.getDefault());
			} else {
				profileTable.setInput(project);
			}
		}
		mode = profileManagerType;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		updateFromSelection(event.getSelection());
	}

	private void updateFromSelection(ISelection selection) {
		IStructuredSelection sSelection = (IStructuredSelection) selection;
		Object element = sSelection.getFirstElement();
		currentProfile = null;
		if (element instanceof IProfile) {
			IProfile profile = (IProfile) element;
			if (mode == MoSyncTool.LEGACY_PROFILE_TYPE) {
				profile = matchLegacyProfile(project, profile);
			}
			currentProfile = profile;
		}
	}

	private IProfile matchLegacyProfile(MoSyncProject project, IProfile legacyProfile) {
		if (project.getProfileManagerType() == MoSyncTool.LEGACY_PROFILE_TYPE) {
			return legacyProfile;
		} else {
			return DeviceCapabilitiesFilter.matchLegacyProfile(project, legacyProfile);
		}
	}

	@Override
	public void open(OpenEvent event) {
		updateFromSelection(event.getSelection());
		if (currentProfile != null) {
			close(true);
		}
	}
}
