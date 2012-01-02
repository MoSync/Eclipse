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
package com.mobilesorcery.sdk.profiles.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.IVendor;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.ProfileFilterFactory;

public class ProfileFilter extends AbstractDeviceFilter {

    private final TreeSet<IProfile> profiles = new TreeSet<IProfile>();

    public ProfileFilter() {

    }

    public void setVendors(IVendor[] vendorsToAccept, boolean add) {
        for (int i = 0; i < vendorsToAccept.length; i++) {
            setVendor(vendorsToAccept[i], add);
        }
    }

    public void setVendors(String[] vendorNames) {
        for (int i = 0; i < vendorNames.length; i++) {
            setVendor(MoSyncTool.getDefault().getVendor(vendorNames[i]), true);
        }
    }

    public void setProfiles(IProfile[] profilesToAccept, boolean add) {
        for (int i = 0; i < profilesToAccept.length; i++) {
            setProfile(profilesToAccept[i], add);
        }
    }

    public void setProfiles(String[] profileNames) {
        for (int i = 0; i < profileNames.length; i++) {
            setProfile(MoSyncTool.getDefault().getProfileManager(MoSyncTool.LEGACY_PROFILE_TYPE).getProfile(profileNames[i]), true);
        }
    }

    public void setVendor(IVendor vendor, boolean add) {
        if (vendor == null) {
            return;
        }

        setProfiles(vendor.getProfiles(), add);
    }

    public void setProfile(IProfile profile, boolean add) {
        if (profile == null) {
            return;
        }

        if (add) {
            profiles.add(profile);
        } else {
            profiles.remove(profile);
        }
    }

    @Override
	public boolean acceptProfile(IProfile profile) {
        boolean acceptIfRequired = profiles.contains(profile);
        return required == acceptIfRequired;
    }

    public Set<IVendor> getVendorsWithAllProfilesAccepted() {
        TreeSet<IVendor> result = new TreeSet<IVendor>();

        TreeSet<IProfile> copyOfProfiles = new TreeSet<IProfile>(profiles);

        while (!copyOfProfiles.isEmpty()) {
            IProfile profile = copyOfProfiles.first();
            IVendor vendor = profile.getVendor();
            IProfile[] profilesForVendor = vendor.getProfiles();
            int profileCount = profilesForVendor.length;
            int profilesBefore = copyOfProfiles.size();
            copyOfProfiles.removeAll(Arrays.asList(profilesForVendor));
            int profilesAfter = copyOfProfiles.size();
            if (profilesBefore - profilesAfter == profileCount) {
                result.add(vendor);
            }

            copyOfProfiles.remove(profile); // Well, just in case...
        }

        return result;
    }

    private IProfile[] getProfilesWithoutVendor(Set vendors) {
        ArrayList<IProfile> result = new ArrayList<IProfile>();
        IProfile[] profiles = this.profiles.toArray(new IProfile[this.profiles.size()]);

        for (int i = 0; i < profiles.length; i++) {
            if (!vendors.contains(profiles[i].getVendor())) {
                result.add(profiles[i]);
            }
        }

        return result.toArray(new IProfile[result.size()]);
    }

    private String vendorString(Set<IVendor> vendors) {
        if (vendors.size() == 0) {
            return "";
        }

        String vendorList = Util.join(vendors.toArray(new IVendor[vendors.size()]), ", ");
        String vendorCaption = vendors.size() == 1 ? "vendor: " : "vendors: ";

        return vendorCaption + vendorList;
    }

    private String profileString(IProfile[] profiles) {
        if (profiles.length == 0) {
            return "";
        }

        String profileList = Util.join(profiles, ", ");
        String profileCaption = profiles.length == 1 ? "profile: " : "profiles: ";

        return profileCaption + profileList;
    }

    @Override
	public String toString() {
        String vendors = vendorString(getVendorsWithAllProfilesAccepted());
        String profiles = profileString(getProfilesWithoutVendor(getVendorsWithAllProfilesAccepted()));

        String delim = (vendors.length() > 0 && profiles.length() > 0) ? "; " : "";

        return (required ? "Required " : "Disallowed ") + vendors + delim + profiles;
    }

    @Override
	public void saveState(IMemento memento) {
        Set<IVendor> vendors = getVendorsWithAllProfilesAccepted();
        memento.putString("vendors", Util.join(vendors.toArray(new IVendor[vendors.size()]), ","));
        memento.putString("profiles", Util.join(getProfilesWithoutVendor(vendors), ","));
        memento.putInteger("require", required ? REQUIRE : DISALLOW);
    }

    public void clear() {
        profiles.clear();
    }

    public IProfile[] getProfiles() {
        return profiles.toArray(new IProfile[profiles.size()]);
    }

    @Override
	public String getFactoryId() {
        return ProfileFilterFactory.ID;
    }


}
