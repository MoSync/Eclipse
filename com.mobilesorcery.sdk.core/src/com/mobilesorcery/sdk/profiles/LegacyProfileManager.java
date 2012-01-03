package com.mobilesorcery.sdk.profiles;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;

import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.core.MoSyncTool;

public class LegacyProfileManager extends ProfileManager {

	private final TreeMap<String, Vendor> vendors = new TreeMap<String, Vendor>(
			String.CASE_INSENSITIVE_ORDER);

	private final HashMap<String, List<IProfile>> profilesForRuntime = new HashMap<String, List<IProfile>>();

	@Override
	public void init() {
		IPath vendorsPath = MoSyncTool.getDefault().getVendorsPath();

		File[] directories = vendorsPath.toFile().listFiles();
		for (int i = 0; i < directories.length; i++) {
			if (directories[i].isDirectory()) {
				Vendor vendor = initVendor(directories[i]);
				this.vendors.put(vendor.getName(), vendor);
			}
		}
	}

	static ImageDescriptor getIconForVendor(File vendorDir) {
		IPath iconPath = new Path(vendorDir.getAbsolutePath()).append("icon.png");
		File iconFile = iconPath.toFile();
		ImageDescriptor icon = null;
		try {
			if (iconFile.exists()) {
				icon = ImageDescriptor.createFromURL(iconFile.toURI().toURL());
			}
		} catch (MalformedURLException e) {
			// Just ignore.
		}

		return icon;
	}

	private Vendor initVendor(File vendorDir) {
		ProfileParser parser = new ProfileParser();

		String name = vendorDir.getName();
		ImageDescriptor icon = getIconForVendor(MoSyncTool.getDefault().getVendorsPath().append(name).toFile());

		Vendor vendor = new Vendor(name, icon);

		File[] profiles = vendorDir.listFiles();
		for (int i = 0; i < profiles.length; i++) {
			if (profiles[i].isDirectory()) {
				String profileName = profiles[i].getName();
				File profileInfoFile = new File(profiles[i], "maprofile.h");
				File runtimeTxtFile = new File(profiles[i], "runtime.txt");
				IProfile profile;
				try {
					profile = parser.parseInfoFile(vendor, profileName,
							profileInfoFile, runtimeTxtFile);
					vendor.addProfile(profile);
					String runtime = toCanonicalRuntime(profile.getRuntime());
					List<IProfile> profilesForOneRuntime =
							profilesForRuntime.get(runtime);
					if (profilesForOneRuntime == null) {
						profilesForOneRuntime = new ArrayList<IProfile>();
						profilesForRuntime.put(runtime, profilesForOneRuntime);
					}
					profilesForOneRuntime.add(profile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return vendor;
	}

	@Override
	public IVendor[] getVendors() {
		return vendors.values().toArray(new IVendor[0]);
	}

	@Override
	public IVendor getVendor(String vendorName) {
		return vendors.get(vendorName);
	}

	@Override
	public List<IProfile> getProfilesForRuntime(String runtime) {
		return profilesForRuntime.get(toCanonicalRuntime(runtime));
	}

	@Override
	public IProfile getDefaultTargetProfile() {
		IProfile result = MoSyncTool.getDefault()
				.getDefaultEmulatorProfile();
		if (result == null) {
			result = super.getDefaultTargetProfile();
		}
		return result;
	}

}