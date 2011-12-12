package com.mobilesorcery.sdk.profiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.xml.sax.Attributes;

import com.mobilesorcery.sdk.core.Capabilities;
import com.mobilesorcery.sdk.core.Capability;
import com.mobilesorcery.sdk.core.CapabilityFragmentation;
import com.mobilesorcery.sdk.core.CapabilityState;
import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.LineReader;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.ProfileManager;
import com.mobilesorcery.sdk.core.Util;

public class ProfileDBManager extends ProfileManager {

	public class ProfileDBResult {
		public final HashMap<String, Vendor> families = new HashMap<String, Vendor>();
		public final Set<IProfile> profiles = new HashSet<IProfile>();
		public final Set<String> capabilities = new TreeSet<String>();
		public final HashSet<String> permissions = new HashSet<String>();
		public final HashMap<String, List<IProfile>> profilesForRuntime = new HashMap<String, List<IProfile>>();
		public final HashMap<String, String> profileMappings = new HashMap<String, String>();
		public final HashMap<IProfile, Capabilities> capabilitiesForProfiles = new HashMap<IProfile, Capabilities>();
	}

	private static final class ProfileDBLineHandler extends
			LineReader.XMLLineAdapter {

		private final CountDownLatch done;
		private boolean inMapTag = false;
		private final ProfileDBResult result;
		private Profile currentProfile;

		public ProfileDBLineHandler(CountDownLatch done, ProfileDBResult result) {
			this.done = done;
			this.result = result;
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes atts) {
			if ("platform".equals(qName)) {
				String familyName = atts.getValue("family");
				String variant = atts.getValue("variant");
				String runtime = atts.getValue("runtime");

				Vendor family = result.families.get(familyName);
				if (family == null) {
					IPath iconDir = MoSyncTool.getDefault().getProfilesPath()
							.append("platforms").append(familyName);
					ImageDescriptor icon = LegacyProfileManager
							.getIconForVendor(iconDir.toFile());
					family = new Vendor(familyName, icon);
					result.families.put(familyName, family);
				}
				currentProfile = new Profile(family, variant,
						MoSyncTool.DEFAULT_PROFILE_TYPE);
				setRuntime(currentProfile, runtime);
				if (!inMapTag) {
					// We never actually add mapped tags
					// since they are only used for finding
					// platforms for devices.
					family.addProfile(currentProfile);
					result.profiles.add(currentProfile);
				} else {
					inMapTag = false;
				}
			} else if ("capability".equals(qName)) {
				String name = atts.getValue("name");
				String type = atts.getValue("type");
				boolean isNested = name.contains("/");
				Object value = atts.getValue("value");
				if (value == null) {
					value = Boolean.TRUE;
				}
				CapabilityState state = CapabilityState.create(atts
						.getValue("state"));
				CapabilityFragmentation fragmentation = CapabilityFragmentation
						.create(atts.getValue("fragmentation"));
				if (!isNested) {
					// TODO: Nested capabilities!
					Capability cap = new Capability(name, state, type, value,
							fragmentation);
					if (Util.isEmpty(type)) {
						// Ok, this is the list of available caps!
						result.capabilities.add(name);
					}
					if (currentProfile != null) {
						Capabilities caps = result.capabilitiesForProfiles.get(currentProfile);
						if (caps == null) {
							caps = new Capabilities();
							result.capabilitiesForProfiles.put(currentProfile, caps);
						}
						caps.setCapability(cap);
					}
				}

				if (state == CapabilityState.REQUIRES_PERMISSION
						|| state == CapabilityState.REQUIRES_PRIVILEGED_PERMISSION) {
					result.permissions.add(name);
				}
			} else if ("map".equals(qName)) {
				String profileName = atts.getValue("platform");
				String ref = atts.getValue("ref");
				if (profileName != null && ref != null) {
					result.profileMappings.put(profileName, ref);
				}
				inMapTag = true;
			}
		}

		private void setRuntime(Profile profile, String runtime) {
			// For backwards compatibility, add platforms/runtime...
			profile.setRuntime("profiles/runtimes/" + runtime);
			String canonicalRuntime = toCanonicalRuntime(profile.getRuntime());
			List<IProfile> profilesForThisRuntime = result.profilesForRuntime
					.get(canonicalRuntime);
			if (profilesForThisRuntime == null) {
				profilesForThisRuntime = new ArrayList<IProfile>();
				result.profilesForRuntime.put(canonicalRuntime,
						profilesForThisRuntime);
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

	private static ProfileDBManager instance = new ProfileDBManager();

	private final TreeMap<String, Vendor> vendors = new TreeMap<String, Vendor>(
			String.CASE_INSENSITIVE_ORDER);

	private final TreeSet<String> capabilities = new TreeSet<String>(
			String.CASE_INSENSITIVE_ORDER);

	private final TreeSet<String> permissions = new TreeSet<String>(
			String.CASE_INSENSITIVE_ORDER);

	private HashMap<String, List<IProfile>> profilesForRuntime = new HashMap<String, List<IProfile>>();

	private boolean inited = false;

	private HashMap<IProfile, Capabilities> capabilitiesForProfiles;

	public static ProfileDBManager getInstance() {
		return instance;
	}

	@Override
	public synchronized void init() {
		if (inited) {
			return;
		}
		inited = true;
		ProfileDBLineHandler lh = runProfileDb(new ArrayList<String>(
				Arrays.asList(new String[] { "-g", "*" })));
		vendors.clear();
		vendors.putAll(lh.result.families);
		capabilities.addAll(lh.result.capabilities);
		permissions.addAll(lh.result.permissions);
		capabilitiesForProfiles = lh.result.capabilitiesForProfiles;
		// Ok, give us some mappings!
		ProfileDBResult matchResult = match("*", new String[0], new String[0]);
		profilesForRuntime = matchResult.profilesForRuntime;
	}

	public static boolean isAvailable() {
		return getTool().toFile().exists();
	}

	private static IPath getTool() {
		IPath profiledb = MoSyncTool.getDefault().getBinary("profiledb");
		return profiledb;
	}

	private ProfileDBLineHandler runProfileDb(List<String> args) {
		CountDownLatch done = new CountDownLatch(1);

		IPath profiledb = getTool();

		CommandLineExecutor profileDbExe = new CommandLineExecutor(
				CoreMoSyncPlugin.LOG_CONSOLE_NAME);

		ProfileDBLineHandler lh = new ProfileDBLineHandler(done,
				new ProfileDBResult());
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
		init();
		return vendors.values().toArray(new IVendor[0]);
	}

	@Override
	public IVendor getVendor(String vendorName) {
		init();
		return vendors.get(vendorName);
	}

	@Override
	public String[] getAvailableCapabilities(boolean permissionsOnly) {
		init();
		if (permissionsOnly) {
			return permissions.toArray(new String[0]);
		} else {
			return capabilities.toArray(new String[0]);
		}
	}

	/**
	 * <p>
	 * Tries to match profiles and returns the matched profiles and a mapping of
	 * profiles, where every key of the map is a matching profile, but the
	 * corresponding value which profile will be actually used.
	 * </p>
	 * <p>
	 * For example, for some projects Android/1.5 will match as well as
	 * Android/2.3. Then the preferred platform variant would be Android/1.5,
	 * but the map returned by this method will allow us to see that instead of
	 * Android/2.3 we will use Android/1.5.
	 *
	 * @param profilePattern
	 * @param requiredCapabilities
	 * @param optionalCapabilities
	 * @return
	 */
	public ProfileDBResult match(String profilePattern,
			String[] requiredCapabilities, String[] optionalCapabilities) {
		init();
		ArrayList<String> args = new ArrayList<String>();
		args.add("--list-mappings");
		args.add("--no-caps");
		args.add("-m");
		args.add(profilePattern);
		args.addAll(Arrays.asList(requiredCapabilities));
		if (optionalCapabilities.length > 0) {
			args.add("-o");
			args.addAll(Arrays.asList(optionalCapabilities));
		}
		ProfileDBLineHandler lh = runProfileDb(args);
		return lh.result;
	}

	@Override
	public List<IProfile> getProfilesForRuntime(String runtime) {
		init();
		List<IProfile> result = profilesForRuntime
				.get(toCanonicalRuntime(runtime));
		return result == null ? null : Collections.unmodifiableList(result);
	}

	public ICapabilities getCapabilities(IProfile profile) {
		return capabilitiesForProfiles.get(profile);
	}
}
