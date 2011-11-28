package com.mobilesorcery.sdk.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.xml.sax.Attributes;

import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.LineReader;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.Profile;
import com.mobilesorcery.sdk.profiles.Vendor;

public class ProfileDBManager extends ProfileManager {

	private static final class ProfileDBLineHandler extends
			LineReader.XMLLineAdapter {

		private final ProfileDBManager db;
		private final HashMap<String, Vendor> families = new HashMap<String, Vendor>();
		private final Set<IProfile> profiles = new HashSet<IProfile>();
		private final HashMap<String, List<IProfile>> profilesForRuntime = new HashMap<String, List<IProfile>>();
		private final HashSet<String> capabilities = new HashSet<String>();
		private final CountDownLatch done;

		public ProfileDBLineHandler(ProfileDBManager db, CountDownLatch done) {
			this.db = db;
			this.done = done;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes atts) {
			if ("platform".equals(qName)) {
				String familyName = atts.getValue("family");
				String variant = atts.getValue("variant");
				String runtime = atts.getValue("runtime");

				Vendor family = families.get(familyName);
				if (family == null) {
					IPath iconDir = MoSyncTool.getDefault().getProfilesPath()
							.append("platforms").append(familyName);
					ImageDescriptor icon = LegacyProfileManager
							.getIconForVendor(iconDir.toFile());
					family = new Vendor(familyName, icon);
					families.put(familyName, family);
				}
				Profile profile = new Profile(family, variant);
				setRuntime(profile, runtime);
				family.addProfile(profile);
				profiles.add(profile);
			} else if ("capability".equals(qName)) {
				String name = atts.getValue("name");
				capabilities.add(name);
			}
		}

		private void setRuntime(Profile profile, String runtime) {
			// For backwards compatibility, add platforms/runtime...
			profile.setRuntime("profiles/runtimes/" + runtime);
			String canonicalRuntime = toCanonicalRuntime(profile.getRuntime());
			List<IProfile> profilesForThisRuntime = profilesForRuntime
					.get(canonicalRuntime);
			if (profilesForThisRuntime == null) {
				profilesForThisRuntime = new ArrayList<IProfile>();
				profilesForRuntime
						.put(canonicalRuntime, profilesForThisRuntime);
			}
			profilesForThisRuntime.add(profile);
		}

		@Override
		public synchronized void doStop(Exception e) {
			if (done != null) {
				done.countDown();
			}
		}
	}

	private final TreeMap<String, Vendor> vendors = new TreeMap<String, Vendor>(
			String.CASE_INSENSITIVE_ORDER);

	private final TreeSet<String> capabilities = new TreeSet<String>(
			String.CASE_INSENSITIVE_ORDER);

	private final HashMap<String, List<IProfile>> profilesForRuntime = new HashMap<String, List<IProfile>>();

	private boolean inited = false;

	@Override
	public synchronized void init() {
		if (inited) {
			return;
		}
		ProfileDBLineHandler lh = runProfileDb(new ArrayList<String>(
				Arrays.asList(new String[] { "-g", "*" })));
		vendors.clear();
		vendors.putAll(lh.families);
		capabilities.addAll(lh.capabilities);
		profilesForRuntime.putAll(lh.profilesForRuntime);

		inited = true;
	}

	private ProfileDBLineHandler runProfileDb(List<String> args) {
		CountDownLatch done = new CountDownLatch(1);
		IPath profiledb = MoSyncTool.getDefault().getBinary("profiledb");

		CommandLineExecutor profileDbExe = new CommandLineExecutor(
				CoreMoSyncPlugin.LOG_CONSOLE_NAME);

		ProfileDBLineHandler lh = new ProfileDBLineHandler(this, done);
		profileDbExe.setLineHandlers(lh, null);

		try {
			ArrayList<String> cmdLine = new ArrayList<String>();
			cmdLine.add(profiledb.toOSString());
			cmdLine.addAll(args);
			profileDbExe.runCommandLine(cmdLine.toArray(new String[0]));
		} catch (Exception e) {
			// Then validate will kick in and notify the user!
			CoreMoSyncPlugin.getDefault().log(e);
		}

		try {
			done.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// Ignore.
			Thread.currentThread().interrupt();
		}
		return lh;
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
	public String[] getAvailableCapabilities() {
		return capabilities.toArray(new String[0]);
	}

	public Set<IProfile> match(String profilePattern,
			String[] requiredCapabilities, String[] optionalCapabilities) {
		ArrayList<String> args = new ArrayList<String>();
		args.add("-m");
		args.add(profilePattern);
		args.addAll(Arrays.asList(requiredCapabilities));
		if (optionalCapabilities.length > 0) {
			args.add("-o");
			args.addAll(Arrays.asList(optionalCapabilities));
		}
		ProfileDBLineHandler lh = runProfileDb(args);
		return lh.profiles;
	}

	public List<IProfile> profilesForRuntime(String runtime) {
		return profilesForRuntime.get(toCanonicalRuntime(runtime));
	}

	private static String toCanonicalRuntime(String runtime) {
		return Util.convertSlashes(runtime);
	}

}
