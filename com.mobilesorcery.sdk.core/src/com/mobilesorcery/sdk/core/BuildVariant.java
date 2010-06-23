package com.mobilesorcery.sdk.core;

import com.mobilesorcery.sdk.profiles.IProfile;

public class BuildVariant implements IBuildVariant {

    private IProfile profile;
    private String cfgId;
    private boolean isFinalizerBuild;
    
    private final static String NULL_CFG = "@null";

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

    /**
     * <p>Returns an <code>IBuildVariant</code> object from a given
     * string of this format: <code>profile, cfgid[,finalize|normal]</code></p>
     * @param string
     * @return <code>null</code> if there was no profile with the given id, or if
     * <code>variantStr</code> was null, or if the given string was otherwise malformed
     */
    public static IBuildVariant parse(String variantStr) {
        if (variantStr == null) {
            return null;
        }
        
        String[] variantComponents = PropertyUtil.toStrings(variantStr);
        if (variantComponents.length == 2 || variantComponents.length == 3) {
            String profileStr = variantComponents[0];
            IProfile profile = MoSyncTool.getDefault().getProfile(profileStr);
            String cfgId = variantComponents[1];
            if (NULL_CFG.equals(cfgId)) {
                cfgId = null;
            }
            boolean isFinalizerBuild = variantComponents.length == 3 && "Finalizer".equals(variantComponents[2]);
            if (profile != null) {
                return new BuildVariant(profile, cfgId, isFinalizerBuild);
            }
        }
        
        return null;
    }
    
    public static String toString(IBuildVariant variant) {
        String profileStr = MoSyncTool.toString(variant.getProfile());
        String cfgId = variant.getConfigurationId();
        if (cfgId == null) {
            cfgId = NULL_CFG;
        }
        return PropertyUtil.fromStrings(new String[] { profileStr, cfgId, variant.isFinalizerBuild() ? "Finalizer" : "Non-finalizer" });
    }
    
    public String toString() {
        return toString(this);
    }
}
