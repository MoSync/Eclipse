package com.mobilesorcery.sdk.core;

import com.mobilesorcery.sdk.profiles.IProfile;

public class BuildVariant implements IBuildVariant {

    private IProfile profile;
    private String cfgId;
    private boolean isFinalizerBuild;

    public BuildVariant(IProfile profile, String cfgId, boolean isFinalizerBuild) {
        this.profile = profile;
        this.cfgId = cfgId;
        this.isFinalizerBuild = isFinalizerBuild;
    }
    
    public String getConfigurationId() {
        return cfgId;
    }

    public IProfile getProfile() {
        return profile;
    }

    public boolean isFinalizerBuild() {
        return isFinalizerBuild;
    }
    
    public boolean equals(Object o) {
        if (o instanceof IBuildVariant) {
            IBuildVariant bv = (IBuildVariant) o;
            return Util.equals(bv.getProfile(), getProfile()) &&
                   Util.equals(bv.getConfigurationId(), getConfigurationId()) &&
                   bv.isFinalizerBuild() == isFinalizerBuild();
        }
        
        return false;
    }

    public int hashCode() {
        int profileHC = profile == null ? 0 : profile.hashCode();
        int cfgHC = cfgId == null ? 0 : cfgId.hashCode();
        int finHC = new Boolean(isFinalizerBuild).hashCode();
        return profileHC ^ cfgHC ^ finHC;
    }
}
