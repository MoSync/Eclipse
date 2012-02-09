package com.mobilesorcery.sdk.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.omg.CORBA.Request;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.core.SimpleQueue;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;
import com.mobilesorcery.sdk.profiles.filter.ProfileFilter;

public class DefaultProfileFilterComposite extends Composite implements
		DisposeListener, Listener {

	class PlatformControl {
		Composite main;
		Label image;
		Button selected;

		public PlatformControl(Composite main, Label image, Button selected) {
			this.main = main;
			this.image = image;
			this.selected = selected;
		}
	}

	class CapabilityControl {
		Composite main;
		Button selected;
		Button required;

		public CapabilityControl(Composite main, Button selected,
				Button required) {
			this.main = main;
			this.selected = selected;
			this.required = required;
		}
	}

	private MoSyncProject project;

	private Font boldFont;

	private HashSet<IVendor> platforms = new HashSet<IVendor>();
	private final HashMap<IVendor, Boolean> eligiblePlatforms = new HashMap<IVendor, Boolean>();
	private HashSet<String> selectedCapabilities = new HashSet<String>();
	private HashSet<String> optionalCapabilities = new HashSet<String>();

	private final HashMap<IVendor, PlatformControl> platformControls = new HashMap<IVendor, PlatformControl>();
	private final HashMap<String, CapabilityControl> capabilityControls = new HashMap<String, CapabilityControl>();

	public DefaultProfileFilterComposite(Composite parent, int style) {
		super(parent, style);
		init();
		addDisposeListener(this);
	}

	private void init() {
		setLayout(new GridLayout(1, false));
		Composite main = new Composite(this, SWT.BORDER);
		main.setLayout(UIUtils.newPrefsLayout(1));

		FormToolkit toolkit = new FormToolkit(getDisplay());
		toolkit.setBorderStyle(SWT.BORDER);
		ScrolledForm form = toolkit.createScrolledForm(main);
		form.setText("Project profile settings");
		form.getBody().setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(GridData.FILL_BOTH));
		form.setLayoutData(new GridData(GridData.FILL_BOTH));

		Section platformSection = toolkit.createSection(form.getBody(),
				Section.DESCRIPTION);
		platformSection.setText("Platforms");
		platformSection
				.setDescription("Select the platforms to associate with this project.");
		platformSection.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true,
				false));
		platformSection.setLayout(UIUtils.newPrefsLayout(1));
		Composite platformSectionMain = toolkit
				.createComposite(platformSection);
		platformSection.setClient(platformSectionMain);

		ColumnLayout platformSectionLayout = new ColumnLayout();
		platformSectionLayout.minNumColumns = 2;
		platformSectionLayout.maxNumColumns = 8;
		platformSectionMain.setLayout(platformSectionLayout);
		ProfileManager mgr = MoSyncTool.getDefault().getProfileManager(
				MoSyncTool.DEFAULT_PROFILE_TYPE);
		IVendor[] vendors = mgr.getVendors();
		for (IVendor vendor : vendors) {
			PlatformControl platformComposite = createPlatformSelector(toolkit,
					platformSectionMain, vendor);
			platformControls.put(vendor, platformComposite);
		}

		Section capabilitiesSection = toolkit.createSection(form.getBody(),
				Section.DESCRIPTION);
		capabilitiesSection.setText("Capabilities");
		capabilitiesSection
				.setDescription("Select capabilities and features for this project.\n"
						+ "Check 'Must have' if the capability is essential for your app. This will filter out all"
						+ "platforms that do not have this capability.\n"
						+ "If the capability is not essential, but rather an optional feature, uncheck 'Must have'");

		capabilitiesSection.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT,
				true, false));
		capabilitiesSection.setLayout(UIUtils.newPrefsLayout(1));
		Composite capabilitiesSectionMain = toolkit
				.createComposite(capabilitiesSection);
		capabilitiesSection.setClient(capabilitiesSectionMain);

		ColumnLayout capabilitiesSectionLayout = new ColumnLayout();
		capabilitiesSectionLayout.minNumColumns = 2;
		capabilitiesSectionLayout.maxNumColumns = 12;
		capabilitiesSectionMain.setLayout(capabilitiesSectionLayout);

		String[] availableCapabilities = MoSyncTool.getDefault()
				.getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE)
				.getAvailableCapabilities(false);

		for (String capability : availableCapabilities) {
			CapabilityControl capabilityComposite = createCapabilitiesSelector(
					toolkit, capabilitiesSectionMain, capability);
			capabilityControls.put(capability, capabilityComposite);
		}

		form.reflow(true);
	}

	private CapabilityControl createCapabilitiesSelector(FormToolkit toolkit,
			Composite parent, String capability) {
		Composite result = toolkit.createComposite(parent);
		GridLayout resultLayout = UIUtils.newPrefsLayout(2);
		resultLayout.verticalSpacing = 0;
		result.setLayout(resultLayout);
		Button selectedButton = new Button(result, SWT.CHECK);
		selectedButton.addListener(SWT.Selection, this);
		selectedButton.setData(capability);
		Label nameLabel = toolkit.createLabel(result, capability);
		if (boldFont == null) {
			boldFont = UIUtils.modifyFont(nameLabel.getFont(), SWT.BOLD);
		}

		nameLabel
				.setLayoutData(new GridData(SWT.LEFT, SWT.DEFAULT, true, false));
		nameLabel.setFont(boldFont);

		Label spacer = toolkit.createLabel(result, "");

		Button optionalButton = new Button(result, SWT.CHECK);
		optionalButton.setText("Must have");
		optionalButton.setLayoutData(new GridData(SWT.LEFT, SWT.DEFAULT, true,
				false));
		optionalButton.setData(capability);
		optionalButton.addListener(SWT.Selection, this);
		toolkit.adapt(selectedButton, true, true);
		toolkit.adapt(optionalButton, true, true);
		toolkit.adapt(result);
		return new CapabilityControl(result, selectedButton, optionalButton);
	}

	private PlatformControl createPlatformSelector(FormToolkit toolkit,
			Composite parent, IVendor platform) {
		Composite result = toolkit.createComposite(parent);
		result.setLayout(new GridLayout(1, false));
		Image image = getPlatformImage(platform, true);
		String name = platform.getName();
		Label iconLabel = null;
		iconLabel = new Label(result, SWT.NONE);
		iconLabel.setAlignment(SWT.CENTER);
		GridData iconLabelData = new GridData(GridData.FILL_HORIZONTAL);
		iconLabelData.heightHint = 48;
		iconLabel.setLayoutData(iconLabelData);
		if (image != null) {
			iconLabel.setImage(image);
		}
		toolkit.adapt(iconLabel, true, true);

		Button nameButton = new Button(result, SWT.CHECK);
		nameButton.setText(name);
		nameButton.addListener(SWT.Selection, this);
		nameButton.setLayoutData(new GridData(SWT.CENTER, SWT.DEFAULT, true,
				false));
		toolkit.adapt(nameButton, true, true);
		nameButton.setData(platform);

		toolkit.adapt(result);
		return new PlatformControl(result, iconLabel, nameButton);
	}

	private Image getPlatformImage(IVendor platform, boolean isEligible) {
		if (isEligible) {
			return MosyncUIPlugin.getDefault().getPlatformImage(platform, null);
		} else {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE);
		}
	}

	public void setProject(MoSyncProject project) {
		this.project = project;
		IDeviceFilter[] filters = project.getDeviceFilter().getFilters();
		// No filter = all platforms!
		HashSet<IVendor> platforms = null;
		for (int i = 0; i < filters.length; i++) {
			IDeviceFilter filter = filters[i];
			if (filter instanceof ProfileFilter) {
				platforms = new HashSet<IVendor>(
						((ProfileFilter) filter)
								.getVendorsWithAllProfilesAccepted());
			} else if (filter instanceof DeviceCapabilitiesFilter) {
				optionalCapabilities = new HashSet<String>(
						((DeviceCapabilitiesFilter) filter)
								.getOptionalCapabilities());
				selectedCapabilities = new HashSet<String>(
						((DeviceCapabilitiesFilter) filter)
								.getRequiredCapabilities());
				selectedCapabilities.addAll(optionalCapabilities);
			}
		}
		if (platforms == null) {
			platforms = new HashSet<IVendor>(Arrays.asList(MoSyncTool
					.getDefault()
					.getProfileManager(MoSyncTool.DEFAULT_PROFILE_TYPE)
					.getVendors()));
		}
		this.platforms = platforms;
		updateUI(true);
	}

	private void updateUI(boolean init) {
		if (init) {
			for (IVendor platform : platformControls.keySet()) {
				PlatformControl control = platformControls.get(platform);
				control.selected.setSelection(platforms.contains(platform));
			}

			for (String capability : capabilityControls.keySet()) {
				CapabilityControl control = capabilityControls.get(capability);
				boolean isRequired = !optionalCapabilities.contains(capability);
				boolean isSelected = selectedCapabilities.contains(capability);
				control.selected.setSelection(isSelected);
				control.required.setSelection(isRequired);
				updateCapabilityUI(capability);
			}
			updateEligiblePlatforms();
		}
	}

	private void updateCapabilityUI(String capability) {
		CapabilityControl control = capabilityControls.get(capability);
		boolean isSelected = selectedCapabilities.contains(capability);
		control.required.setVisible(isSelected);
	}

	private void updateEligiblePlatforms() {
		getShell().setEnabled(false);
		Cursor prevCursor = getShell().getCursor();
		getShell().setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));
		DeviceCapabilitiesFilter filter = createCapabilitiesFilter();
		for (IVendor platform : platformControls.keySet()) {
			boolean acceptedPlatform = filter.accept(platform);
			PlatformControl platformControl = platformControls.get(platform);
			platformControl.image.setImage(getPlatformImage(platform, acceptedPlatform));
			platformControl.selected.setEnabled(acceptedPlatform);
			boolean selectPlatform = acceptedPlatform && platforms.contains(platform);
			platformControl.selected.setSelection(selectPlatform);
			eligiblePlatforms.put(platform, acceptedPlatform);
		}
		getShell().setCursor(prevCursor);
		getShell().setEnabled(true);
	}

	public void updateProject() {
		project.getDeviceFilter().removeAllFilters();
		ProfileFilter platformFilter = new ProfileFilter(MoSyncTool.DEFAULT_PROFILE_TYPE);
		platformFilter.setStyle(ProfileFilter.REQUIRE);
		for (IVendor platform : platforms) {
			platformFilter.setVendor(platform, true);
		}
		project.getDeviceFilter().addFilter(platformFilter);

		project.getDeviceFilter().addFilter(createCapabilitiesFilter());
	}

	private DeviceCapabilitiesFilter createCapabilitiesFilter() {
		HashSet<String> requiredCapabilities = new HashSet<String>(selectedCapabilities);
		requiredCapabilities.removeAll(optionalCapabilities);
		DeviceCapabilitiesFilter capabilitiesFilter = DeviceCapabilitiesFilter.create(
				requiredCapabilities.toArray(new String[0]),
				optionalCapabilities.toArray(new String[0]));
		return capabilitiesFilter;
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		if (boldFont != null) {
			boldFont.dispose();
		}
	}

	@Override
	public void handleEvent(Event event) {
		Object data = event.widget.getData();
		if (data instanceof String) {
			// Capability
			String capability = (String) data;
			CapabilityControl capabilityControl = this.capabilityControls
					.get(capability);
			if (event.widget == capabilityControl.required) {
				if (capabilityControl.required.getSelection()) {
					optionalCapabilities.remove(capability);
				} else {
					optionalCapabilities.add(capability);
				}
			}
			if (event.widget == capabilityControl.selected) {
				if (capabilityControl.selected.getSelection()) {
					selectedCapabilities.add(capability);
				} else {
					selectedCapabilities.remove(capability);
				}
			}
			updateCapabilityUI(capability);
			updateEligiblePlatforms();
		} else if (data instanceof IVendor) {
			IVendor platform = (IVendor) data;
			PlatformControl platformControl = this.platformControls
					.get(platform);
			if (platformControl != null) {
				if (platformControl.selected.isEnabled() && platformControl.selected.getSelection()) {
					platforms.add(platform);
				} else {
					platforms.remove(platform);
				}
			}
		}
	}

}
