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

    private TreeSet<IProfile> profiles = new TreeSet<IProfile>();
       
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
            setProfile(MoSyncTool.getDefault().getProfile(profileNames[i]), true);
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

    public boolean acceptProfile(IProfile profile) {
        boolean acceptIfRequired = profiles.contains(profile);
        return required == acceptIfRequired;
    }

    private Set<IVendor> getVendorsWithAllProfilesAccepted() {
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

    public String toString() {
        String vendors = vendorString(getVendorsWithAllProfilesAccepted());
        String profiles = profileString(getProfilesWithoutVendor(getVendorsWithAllProfilesAccepted()));

        String delim = (vendors.length() > 0 && profiles.length() > 0) ? "; " : "";

        return (required ? "Required " : "Disallowed ") + vendors + delim + profiles;
    }

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

    public String getFactoryId() {
        return ProfileFilterFactory.ID;
    }


}
