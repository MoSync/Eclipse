package com.mobilesorcery.sdk.ui;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.profiles.IDeviceFilter;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;
import com.mobilesorcery.sdk.profiles.filter.ProfileFilter;
import com.mobilesorcery.sdk.ui.DefaultProfileFilterComposite.PlatformControl;

public class DefaultProfileFilterComposite extends Composite implements DisposeListener, Listener {

	class PlatformControl {
		Composite main;
		Label image;
		Button selected;
		public PlatformControl(Composite main, Label image,
				Button selected) {
			this.main = main;
			this.image = image;
			this.selected = selected;
		}
	}

	class CapabilityControl {
		Composite main;
		Button required;
		Button optional;
		public CapabilityControl(Composite main, Button required, Button optional) {
			this.main = main;
			this.required = required;
			this.optional = optional;
		}
	}

	private MoSyncProject project;

	private Font boldFont;

	private HashSet<IVendor> platforms = new HashSet<IVendor>();
	private HashSet<String> requiredCapabilities = new HashSet<String>();
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

		Section platformSection = toolkit.createSection(form.getBody(), Section.DESCRIPTION);
		platformSection.setText("Platforms");
		platformSection.setDescription("Select the platforms to associate with this project.");
		platformSection.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		platformSection.setLayout(UIUtils.newPrefsLayout(1));
		Composite platformSectionMain = toolkit.createComposite(platformSection);
		platformSection.setClient(platformSectionMain);

		ColumnLayout platformSectionLayout = new ColumnLayout();
		platformSectionLayout.minNumColumns = 2;
		platformSectionLayout.maxNumColumns = 8;
		platformSectionMain.setLayout(platformSectionLayout);
		ProfileManager mgr = MoSyncTool.getDefault().getProfileManager(MoSyncTool.DEFAULT_PROFILE_MANAGER);
		IVendor[] vendors = mgr.getVendors();
		for (IVendor vendor : vendors) {
			PlatformControl platformComposite = createPlatformSelector(toolkit, platformSectionMain, vendor);
			platformControls.put(vendor, platformComposite);
		}

		Section capabilitiesSection = toolkit.createSection(form.getBody(), Section.DESCRIPTION);
		capabilitiesSection.setText("Capabilities");
		capabilitiesSection.setDescription("Select capabilities and features for this project. " +
				"Check 'Optional' if your app should still run without the capability. " +
				"This will make sure that platforms that do not support the 'Optional' capability " +
				"will still be built.");

		capabilitiesSection.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		capabilitiesSection.setLayout(UIUtils.newPrefsLayout(1));
		Composite capabilitiesSectionMain = toolkit.createComposite(capabilitiesSection);
		capabilitiesSection.setClient(capabilitiesSectionMain);

		ColumnLayout capabilitiesSectionLayout = new ColumnLayout();
		capabilitiesSectionLayout.minNumColumns = 2;
		capabilitiesSectionLayout.maxNumColumns = 12;
		capabilitiesSectionMain.setLayout(capabilitiesSectionLayout);

		String[] availableCapabilities =
				MoSyncTool.getDefault().getProfileManager(MoSyncTool.DEFAULT_PROFILE_MANAGER).getAvailableCapabilities();

		for (String capability : availableCapabilities) {
			CapabilityControl capabilityComposite = createCapabilitiesSelector(toolkit, capabilitiesSectionMain, capability);
			capabilityControls.put(capability, capabilityComposite);
		}

		form.reflow(true);
	}

	private CapabilityControl createCapabilitiesSelector(FormToolkit toolkit, Composite parent, String capability) {
		Composite result = toolkit.createComposite(parent);
		result.setLayout(new GridLayout(2, false));
		Button requiredButton = new Button(result, SWT.CHECK);
		requiredButton.addListener(SWT.Selection, this);
		requiredButton.setData(capability);
		Label nameLabel = toolkit.createLabel(result, capability);
		if (boldFont == null) {
			boldFont = UIUtils.modifyFont(nameLabel.getFont(), SWT.BOLD);
		}

		nameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.DEFAULT, true, false));
		nameLabel.setFont(boldFont);

		Label spacer = toolkit.createLabel(result, "");

		Button optionalButton = new Button(result, SWT.CHECK);
		optionalButton.setText("Optional");
		optionalButton.setLayoutData(new GridData(SWT.LEFT, SWT.DEFAULT, true, false));
		optionalButton.setData(capability);
		optionalButton.addListener(SWT.Selection, this);
		toolkit.adapt(requiredButton, true, true);
		toolkit.adapt(optionalButton, true, true);
		toolkit.adapt(result);
		return new CapabilityControl(result, requiredButton, optionalButton);
	}

	private PlatformControl createPlatformSelector(FormToolkit toolkit, Composite parent, IVendor platform) {
		Composite result = toolkit.createComposite(parent);
		result.setLayout(new GridLayout(1, false));
		Image icon = platform.getIcon().createImage(); // TODO: LEAK, LEAK!
		String name = platform.getName();
		Label iconLabel = null;
		if (icon != null) {
			iconLabel = new Label(result, SWT.NONE);
			iconLabel.setAlignment(SWT.CENTER);
			iconLabel.setImage(icon);
			iconLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		Button nameButton = new Button(result, SWT.CHECK);
		nameButton.setText(name);
		nameButton.addListener(SWT.Selection, this);
		nameButton.setLayoutData(new GridData(SWT.CENTER, SWT.DEFAULT, true, false));
		toolkit.adapt(nameButton, true, true);
		nameButton.setData(platform);

		toolkit.adapt(result);
		return new PlatformControl(result, iconLabel, nameButton);
	}

	public void setProject(MoSyncProject project) {
		this.project = project;
		IDeviceFilter[] filters = project.getDeviceFilter().getFilters();
		for (int i = 0; i < filters.length; i++) {
			IDeviceFilter filter = filters[i];
			if (filter instanceof ProfileFilter) {
				platforms = new HashSet<IVendor>(((ProfileFilter) filter).getVendorsWithAllProfilesAccepted());
			} else if (filter instanceof DeviceCapabilitiesFilter) {
				requiredCapabilities = new HashSet<String>(((DeviceCapabilitiesFilter) filter).getRequiredCapabilities());
				optionalCapabilities = new HashSet<String>(((DeviceCapabilitiesFilter) filter).getOptionalCapabilities());
			}
		}
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
				boolean isOptional = optionalCapabilities.contains(capability);
				boolean isRequired = isOptional || requiredCapabilities.contains(capability);
				control.required.setSelection(isRequired);
				control.optional.setSelection(isOptional);
				updateCapabilityUI(capability);
			}
		}
	}

	private void updateCapabilityUI(String capability) {
		CapabilityControl control = capabilityControls.get(capability);
		boolean isRequired = requiredCapabilities.contains(capability);
		control.optional.setVisible(isRequired);
	}

	public void updateProject() {
		project.getDeviceFilter().removeAllFilters();
		ProfileFilter platformFilter = new ProfileFilter();
		platformFilter.setStyle(ProfileFilter.REQUIRE);
		for (IVendor platform : platforms) {
			platformFilter.setVendor(platform, true);
		}
		if (platforms.size() > 0) {
			project.getDeviceFilter().addFilter(platformFilter);
		}

		DeviceCapabilitiesFilter capabilitiesFilter =
				new DeviceCapabilitiesFilter(
						requiredCapabilities.toArray(new String[0]),
						optionalCapabilities.toArray(new String[0]));
		if (optionalCapabilities.size() > 0 || requiredCapabilities.size() > 0) {
			project.getDeviceFilter().addFilter(capabilitiesFilter);
		}
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
			CapabilityControl capabilityControl = this.capabilityControls.get(capability);
			if (event.widget == capabilityControl.optional) {
				if (capabilityControl.optional.getSelection()) {
					optionalCapabilities.add(capability);
				} else {
					optionalCapabilities.remove(capability);
				}
			}
			if (event.widget == capabilityControl.required) {
				if (capabilityControl.required.getSelection()) {
					requiredCapabilities.add(capability);
				} else {
					requiredCapabilities.remove(capability);
				}
			}
			updateCapabilityUI(capability);
		} else if (data instanceof IVendor) {
			IVendor platform = (IVendor) data;
			PlatformControl platformControl = this.platformControls.get(platform);
			if (platformControl != null) {
				if (platformControl.selected.getSelection()) {
					platforms.add(platform);
				} else {
					platforms.remove(platform);
				}
			}
		}
	}



}
