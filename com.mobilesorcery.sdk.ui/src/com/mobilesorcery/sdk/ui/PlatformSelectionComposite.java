package com.mobilesorcery.sdk.ui;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Pair;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.Profile;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;
import com.mobilesorcery.sdk.ui.internal.DefaultProfileFilterDialog;

public class PlatformSelectionComposite implements Listener,
		ISelectionChangedListener, IOpenListener, IDoubleClickListener {

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

	public class RichProfileLabelProvider extends StyledCellLabelProvider {

		private Font originalFont = null;

		@Override
		public void update(ViewerCell cell) {
			Object obj = cell.getElement();
			boolean isTargetProfile = false;
			Image image = null;
			String mainText = "";
			String subText = "";
			Point imageSize = new Point(16, 16);
			if (originalFont == null) {
				originalFont = cell.getFont();
			}
			Font boldFont = MosyncUIPlugin.getDefault().getFont(
					MosyncUIPlugin.FONT_DEFAULT_BOLD);
			Font italicFont = MosyncUIPlugin.getDefault().getFont(
					MosyncUIPlugin.FONT_DEFAULT_ITALIC);

			if (obj instanceof IProfile) {
				IProfile profile = (IProfile) obj;
				mainText = profile.getName();
				isTargetProfile = isTargetProfile(profile);
				subText = getProfileDescription(project, profile);
				if (subText.equals(mainText))
					subText = "";
				image = isTargetProfile ? MosyncUIPlugin.getDefault()
						.getImageRegistry()
						.get(MosyncUIPlugin.TARGET_PHONE_IMAGE)
						: MosyncUIPlugin.getDefault().getImageRegistry()
								.get(MosyncUIPlugin.PHONE_IMAGE);
			} else if (obj instanceof IVendor) {
				IVendor platform = (IVendor) obj;
				image = MosyncUIPlugin.getDefault().getPlatformImage(platform,
						imageSize);
				mainText = platform.getName();
			}

			if (image != null) {
				cell.setImage(image);
			}

			int mainTextStyle = SWT.NORMAL;

			if (obj instanceof IVendor) {
				mainTextStyle = SWT.ITALIC;
				cell.setFont(italicFont);
			} else if (isTargetProfile) {
				mainTextStyle = SWT.BOLD;
				cell.setFont(boldFont);
			} else {
				cell.setFont(originalFont);
			}

			boolean hasSubText = Util.isEmpty(subText);
			String space = hasSubText ? "" : " ";
			String fullText = mainText + space + subText;
			cell.setText(fullText);
			StyleRange[] styleRanges = new StyleRange[2];
			styleRanges[0] = new StyleRange(0, mainText.length(), null, null,
					mainTextStyle);
			Color gray = cell.getControl().getDisplay()
					.getSystemColor(SWT.COLOR_DARK_GRAY);
			styleRanges[1] = new StyleRange(mainText.length(),
					fullText.length() - mainText.length(), gray, null,
					mainTextStyle);
			cell.setStyleRanges(styleRanges);
		}

		private boolean isTargetProfile(IProfile profile) {
			return profile != null && getProfileToApply() != null && profile.equals(currentProfile);
		}

	}

	private static final String FILTER = "filter";

	private static final String LOOKUP = "lookup";

	private final Control control;
	private MoSyncProject project;
	private Text filterBox;
	private boolean filterShown;
	private TreeViewer profileTable;
	private int mode = SWT.NONE;
	private RichProfileLabelProvider profileLabelProvider;
	private IProfile currentProfile;
	private final int style;
	private Button applyButton;
	private ProfileContentProvider content;
	private Button cancelButton;
	private Button lookupButton;
	private Button filterButton;
	private Composite extrasPanel;
	private Text filterText;

	private Button convertButton;

	private Text lookupText;

	private static Shell CURRENT_SHELL;

	public PlatformSelectionComposite(Control control, int style) {
		this.control = control;
		this.style = style;
	}

	public void setProject(MoSyncProject project) {
		this.project = project;
	}

	private boolean hasSearchBox() {
		return (style & SWT.SEARCH) != 0;
	}

	private boolean alwaysShowSearchBox() {
		return mode == SWT.SEARCH || (style & SWT.BACKGROUND) == 0;
	}

	protected Composite createContentArea(Composite parent) {
		Shell main = CURRENT_SHELL;
		// main.setLayout(UIUtils.newPrefsLayout(1));
		// inner = new Composite(main, SWT.NONE);
		// inner.setLayoutData(new GridData(GridData.FILL_BOTH));

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());

		Composite profilesArea = toolkit.createComposite(main, SWT.BORDER);
		profilesArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 0;
		profilesArea.setLayout(layout);
		filterBox = new Text(profilesArea, SWT.SEARCH);
		filterBox.addListener(SWT.KeyUp, this);
		filterBox.addListener(SWT.KeyDown, this);

		profileTable = new TreeViewer(profilesArea, SWT.NONE);
		profileLabelProvider = new RichProfileLabelProvider();
		profileTable.setLabelProvider(profileLabelProvider);
		content = new ProfileContentProvider(project);
		profileTable.setContentProvider(content);
		GridData profileTableData = new GridData(SWT.FILL, SWT.FILL, true, true);
		// profileTableData.grabExcessVerticalSpace = true;
		// profileTableData.verticalAlignment = SWT.TOP;
		profileTable.getControl().setLayoutData(profileTableData);
		profileTable.getControl().setFocus();
		profileTable.getControl().addListener(SWT.KeyDown, this);
		profileTable.getControl().addListener(SWT.Selection, this);
		profileTable.addDoubleClickListener(this);
		profileTable.addSelectionChangedListener(this);
		profileTable.addOpenListener(this);

		extrasPanel = toolkit.createComposite(main);
		extrasPanel
				.setLayout(new GridLayout(
						project.getProfileManagerType() == MoSyncTool.DEFAULT_PROFILE_TYPE ? 3
								: 1, false));

		createExtrasPanel(toolkit);
		/*
		 * extrasPanel = new DescriptiveButtonBar(main, SWT.NONE);
		 * extrasPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		 *
		 * extrasPanel.add(FILTER,
		 * MosyncUIPlugin.getDefault().getImageRegistry()
		 * .get(MosyncUIPlugin.IMG_FILTER), "Select Capabilities", "!");
		 * extrasPanel.add(LOOKUP,
		 * MosyncUIPlugin.getDefault().getImageRegistry()
		 * .get(MosyncUIPlugin.IMG_LOOKUP), "Find Profile for Device", "!");
		 *
		 * toolkit.adapt(extrasPanel);
		 */
		Composite buttonBar = toolkit.createComposite(main);
		buttonBar.setLayout(new GridLayout(2, true));
		buttonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.DEFAULT, false,
				false));

		cancelButton = new Button(buttonBar, SWT.PUSH);
		cancelButton.setText("&Cancel");
		cancelButton.setLayoutData(new GridData(SWT.RIGHT, SWT.DEFAULT, false,
				false));
		cancelButton.addListener(SWT.Selection, this);

		applyButton = new Button(buttonBar, SWT.PUSH);
		applyButton.setText("&Apply");
		applyButton.setLayoutData(new GridData(SWT.RIGHT, SWT.DEFAULT, false,
				false));
		applyButton.addListener(SWT.Selection, this);

		main.setBackground(profileTable.getControl().getBackground());
		setMode(mode, true);
		showFilterBox(hasSearchBox() && alwaysShowSearchBox());

		currentProfile = project.getTargetProfile();
		if (currentProfile != null) {
			profileTable.refresh(currentProfile);
			profileTable.reveal(currentProfile);
			profileTable.setSelection(new StructuredSelection(currentProfile));
		}

		toolkit.adapt(profileTable.getControl(), true, true);

		return main;
	}

	private void createExtrasPanel(FormToolkit toolkit) {
		if (project.getProfileManagerType() == MoSyncTool.LEGACY_PROFILE_TYPE) {
			convertButton = new Button(extrasPanel, SWT.PUSH);
			convertButton.setImage(MosyncUIPlugin.getDefault()
					.getImageRegistry().get(MosyncUIPlugin.IMG_FILTER));
			convertButton.setText("Make Platform Based");
			convertButton.addListener(SWT.Selection, this);
			Text convertText = new Text(extrasPanel, SWT.WRAP | SWT.READ_ONLY);
			convertText.setFont(MosyncUIPlugin.getDefault().getFont(
					MosyncUIPlugin.FONT_INFO_TEXT));
			GridData convertTextData = new GridData(
					2 * UIUtils.getDefaultFieldSize(), SWT.DEFAULT);
			convertTextData.verticalAlignment = SWT.TOP;
			convertText.setLayoutData(convertTextData);
			convertText
					.setText("Click the button above to convert this project into a platform based one (instead of the current device based).");
			toolkit.adapt(convertText, true, true);
		} else {
			filterButton = new Button(extrasPanel, SWT.PUSH);
			filterButton.setText("Select Capabilities");
			filterButton.setImage(MosyncUIPlugin.getDefault()
					.getImageRegistry().get(MosyncUIPlugin.IMG_FILTER));
			filterButton.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));
			filterButton.addListener(SWT.Selection, this);

			Label separator = new Label(extrasPanel, SWT.SEPARATOR
					| SWT.VERTICAL);
			separator.setLayoutData(new GridData(SWT.DEFAULT, SWT.FILL, true,
					false, 1, 2));
			lookupButton = new Button(extrasPanel, SWT.PUSH);
			lookupButton.setLayoutData(new GridData(SWT.LEFT, SWT.DEFAULT,
					true, false));
			initLookupButton();
			Point lookupButtonSize = lookupButton.computeSize(SWT.DEFAULT,
					SWT.DEFAULT);
			filterButton.setLayoutData(new GridData(lookupButtonSize.x,
					lookupButtonSize.y));
			lookupButton.addListener(SWT.Selection, this);

			filterText = new Text(extrasPanel, SWT.WRAP | SWT.READ_ONLY);
			filterText.setFont(MosyncUIPlugin.getDefault().getFont(
					MosyncUIPlugin.FONT_INFO_TEXT));
			GridData filterTextData = new GridData(
					UIUtils.getDefaultFieldSize(), SWT.DEFAULT);
			filterTextData.verticalAlignment = SWT.TOP;
			filterText.setLayoutData(filterTextData);
			toolkit.adapt(filterText, true, true);

			lookupText = new Text(extrasPanel, SWT.WRAP | SWT.READ_ONLY);
			lookupText.setFont(MosyncUIPlugin.getDefault().getFont(
					MosyncUIPlugin.FONT_INFO_TEXT));
			GridData lookupTextData = new GridData(
					UIUtils.getDefaultFieldSize(), SWT.DEFAULT);
			lookupTextData.verticalAlignment = SWT.TOP;
			lookupText.setLayoutData(lookupTextData);
			toolkit.adapt(lookupText, true, true);
		}
		CURRENT_SHELL.layout();
	}

	private void showFilterBox(boolean show) {
		filterShown = show;
		GridData data = show ? new GridData(SWT.FILL, SWT.NONE, true, false)
				: new GridData(0, 0);
		filterBox.setLayoutData(data);
		filterBox.getParent().layout();
		filterBox.getParent().redraw();
		filterBox.forceFocus();
		filterBox.setVisible(show);
		if (!show) {
			filterBox.setText("");
			updateFilter();
		}
	}

	public void show(int style) {
		boolean asDropdown = (style & SWT.DROP_DOWN) != 0;
		close(false);
		int shellStyle = (asDropdown ? SWT.ON_TOP | SWT.TOOL : SWT.RESIZE
				| SWT.TITLE | SWT.CLOSE);
		final Shell shell = new Shell(control.getShell(), shellStyle);
		shell.setText("Select active profile");
		CURRENT_SHELL = shell;
		shell.setLayout(new GridLayout(1, false));
		createContentArea(shell);
		int preferredWidth = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		shell.setSize(preferredWidth, 3 * UIUtils.getDefaultListHeight() / 2);

		if (asDropdown) {
			Point controlLocation = control.toDisplay(asDropdown ? control
					.getLocation() : new Point(0, 0));
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
		IProfile newProfile = getProfileToApply();
		if (saveTargetProfile && newProfile != null) {
			project.setTargetProfile(newProfile);
		}
	}

	private IProfile getProfileToApply() {
		IProfile result = currentProfile;
		if (mode == SWT.SEARCH && result != null) {
			result = matchLegacyProfile(project, result);
		}
		return result;
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
		if (event.type == SWT.KeyDown
				&& shouldCharBeSentToSearchBox(event.character)) {
			if (!filterShown) {
				showFilterBox(hasSearchBox());
			}
			if (event.widget != filterBox && event.character == SWT.BS) {
				filterBox.setText(filterBox.getText(0,
						Math.max(0, filterBox.getText().length() - 2)));
				updateFilter();
			} else if (event.widget != filterBox) {
				filterBox.append("" + event.character);
				updateFilter();
			}
			filterBox.setSelection(filterBox.getText().length());
			filterBox.forceFocus();
		} else if (event.widget == filterBox && event.type == SWT.KeyDown) {
			if (event.keyCode == SWT.ARROW_DOWN
					|| event.keyCode == SWT.ARROW_UP) {
				profileTable.getControl().forceFocus();
				if (profileTable.getSelection().isEmpty()
						&& profileTable.getExpandedElements().length > 0) {
					profileTable.getTree().select(
							(TreeItem) profileTable.getExpandedElements()[0]);
				}
			}
		} else if (event.widget == filterBox && event.type == SWT.KeyUp
				&& hasSearchBox()) {
			String filterText = filterBox.getText();
			updateFilter();
			if (filterText.length() == 0) {
				showFilterBox(alwaysShowSearchBox());
				profileTable.getControl().forceFocus();
			}
		} else if (event.widget == CURRENT_SHELL
				&& event.type == SWT.Deactivate) {
			close(false);
		} else if (event.widget == CURRENT_SHELL && event.type == SWT.Resize) {
			CURRENT_SHELL.layout();
			resize(CURRENT_SHELL.getBounds().width,
					CURRENT_SHELL.getBounds().height);
		} else if (event.keyCode == SWT.ESC
				&& (event.type == SWT.KeyDown || event.type == SWT.KeyUp)) {
			close(false);
		} else if (event.widget == filterButton) {
			DefaultProfileFilterDialog dialog = new DefaultProfileFilterDialog(
					CURRENT_SHELL);
			dialog.setProject(project);
			dialog.open();
			setMode(SWT.NONE, true);
		} else if (event.widget == lookupButton) {
			if (mode == SWT.SEARCH) {
				showFilterBox(false);
			}
			setMode(mode == SWT.SEARCH ? SWT.NONE : SWT.SEARCH, true);
		} else if (event.widget == convertButton) {
			project.setProfileManagerType(MoSyncTool.DEFAULT_PROFILE_TYPE);
			// Just close + open.
			close(false);
			show(SWT.NONE);
		} else if (event.widget == cancelButton) {
			close(false);
		} else if (event.widget == applyButton) {
			setAndClose(profileTable.getSelection());
		}
	}

	private void updateFilter() {
		profileTable.setFilters(new ViewerFilter[] { new ProfileTextFilter(
				filterBox.getText()) });
	}

	private void resize(int width, int height) {
		/*
		 * GridLayout innerData = UIUtils.newPrefsLayout(1);
		 * innerData.verticalSpacing = 0; inner.setLayout(innerData);
		 * inner.setLayoutData(new GridData(width, height));
		 * inner.setSize(width, height); inner.getParent().layout(true, true);
		 */
	}

	private boolean shouldCharBeSentToSearchBox(char ch) {
		return Character.isLetterOrDigit(ch)
				|| (ch == SWT.BS && !filterBox.getText().isEmpty());
	}

	private void setMode(int mode, boolean force) {
		this.mode = mode;
		if (force || (this.mode != mode && profileTable != null)) {
			if (mode == SWT.SEARCH) {
				// The content provider uses the mosync tool to get
				// 'legacy' devices.
				content.setProject(null);
				profileTable.setInput(MoSyncTool.getDefault());
				showFilterBox(true);
			} else {
				profileTable.setInput(project);
				content.setProject(project);
			}
			updateFilterMessage();
			updateLookupMessage();
		}
	}

	private void updateFilterMessage() {
		if (filterText != null) {
			int total = project.getProfileManager().getVendors().length;
			int filteredOut = total - project.getFilteredVendors().length;
			filterText
					.setText(MessageFormat
							.format("{0} of {1} platforms are filtered out, click the button above to filter platforms based on available capabilites.",
									filteredOut, total));
		}
	}

	private void initLookupButton() {
		lookupButton.setText("Find Profile for Device");
		lookupButton.setImage(MosyncUIPlugin.getDefault().getImageRegistry()
				.get(MosyncUIPlugin.IMG_LOOKUP));
		if (lookupText != null) {
			lookupText
					.setText("Click the button above to find out which platform a specific device has.");
		}
	}

	private void updateLookupMessage() {
		if (lookupButton != null) {
			if (mode == SWT.SEARCH) {
				Image back = PlatformUI.getWorkbench().getSharedImages()
						.getImage(ISharedImages.IMG_TOOL_BACK);
				lookupButton.setImage(back);
				lookupButton.setText("Back");
				if (lookupText != null) {
					lookupText.setText("");
				}
			} else {
				initLookupButton();
			}
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		updateFromSelection(event.getSelection());
	}

	private void updateFromSelection(ISelection selection) {
		IStructuredSelection sSelection = (IStructuredSelection) selection;
		Object element = sSelection.getFirstElement();
		IProfile oldProfile = currentProfile;
		currentProfile = null;
		if (element instanceof IProfile) {
			IProfile profile = (IProfile) element;
			currentProfile = profile;
		}
		if (oldProfile != null) {
			profileTable.refresh();
		}
		if (currentProfile != null) {
			profileTable.refresh(currentProfile);
		}

		updateUI();
	}

	private void updateUI() {
		applyButton.setEnabled(getProfileToApply() != null);
	}

	private IProfile matchLegacyProfile(MoSyncProject project,
			IProfile legacyProfile) {
		if (project.getProfileManagerType() == MoSyncTool.LEGACY_PROFILE_TYPE) {
			return legacyProfile;
		} else {
			return DeviceCapabilitiesFilter.matchLegacyProfile(project,
					legacyProfile);
		}
	}

	public void setAndClose(ISelection selection) {
		updateFromSelection(selection);
		if (getProfileToApply() != null) {
			close(true);
		}
	}

	@Override
	public void open(OpenEvent event) {
		setAndClose(event.getSelection());
	}

	@Override
	public void doubleClick(DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.getFirstElement() instanceof IVendor) {
			profileTable
					.setExpandedState(selection.getFirstElement(),
							!profileTable.getExpandedState(selection
									.getFirstElement()));
		}
		setAndClose(selection);
	}

	private String getProfileDescription(MoSyncProject project, IProfile profile) {
		if (mode == SWT.SEARCH) {
			IProfile platformProfile = matchLegacyProfile(project, profile);
			if (platformProfile != null) {
				return "\u2192 " + MoSyncTool.toString(platformProfile);
			} else {
				List<IProfile> filteredOutProfile = MoSyncTool.getDefault()
						.getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE)
						.getProfilesForRuntime(profile.getRuntime());
				if (filteredOutProfile != null && filteredOutProfile.size() > 0) {
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

	private String getProfileShortDescription(MoSyncProject project,
			IProfile profile) {
		return profile.getPackager().getShortDescription(project, profile);
	}
}
