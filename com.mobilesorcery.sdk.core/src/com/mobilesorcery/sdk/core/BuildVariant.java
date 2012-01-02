package com.mobilesorcery.sdk.core;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.mobilesorcery.sdk.profiles.IProfile;

public class BuildVariant implements IBuildVariant {

    private IProfile profile;
    private String cfgId;
	private TreeMap<String, String> specifiers = new TreeMap<String,String>();

    private final static String NULL_CFG = "@null";

    public BuildVariant(IProfile profile, String cfgId) {
    	this(profile, cfgId, null);
    }

    public BuildVariant(IProfile profile, IBuildConfiguration cfg) {
        this(profile, cfg == null ? null : cfg.getId());
    }

    public BuildVariant(IProfile profile, String cfgId, Map<String, String> specifiers) {
        this.profile = profile;
        this.cfgId = cfgId;
        this.specifiers = new TreeMap<String,String>();
        if (specifiers != null) {
        	specifiers.putAll(specifiers);
        }
	}

    public BuildVariant(IBuildVariant prototype) {
    	this(prototype.getProfile(), prototype.getConfigurationId(), prototype.getSpecifiers());
    }

	public void setSpecifier(String specifier, String value) {
    	if (value != null) {
    		specifiers.put(specifier, value);
    	} else {
    		specifiers.remove(specifier);
    	}
    }

    private BuildVariant copy() {
		BuildVariant result = new BuildVariant(profile, cfgId);
		result.specifiers = new TreeMap<String, String>(specifiers);
		return result;
	}

	@Override
	public String getConfigurationId() {
        return cfgId;
    }

	public void setConfigurationId(String cfgId) {
		this.cfgId = cfgId;
	}

    @Override
	public IProfile getProfile() {
        return profile;
    }

    public void setProfile(IProfile profile) {
    	this.profile = profile;
    }

	@Override
	public SortedMap<String, String> getSpecifiers() {
		return specifiers;
	}

    @Override
	public boolean equals(Object o) {
        if (o instanceof IBuildVariant) {
            IBuildVariant bv = (IBuildVariant) o;
            return Util.equals(bv.getProfile(), getProfile()) &&
                   Util.equals(bv.getConfigurationId(), getConfigurationId()) &&
                   Util.equals(bv.getSpecifiers(), getSpecifiers());
        }

        return false;
    }

    @Override
	public int hashCode() {
        int profileHC = profile == null ? 0 : profile.hashCode();
        int cfgHC = cfgId == null ? 0 : cfgId.hashCode();
        int specHC = specifiers == null ? 0 : specifiers.hashCode();
        return profileHC ^ cfgHC ^ specHC;
    }

    /**
     * <p>Returns an <code>IBuildVariant</code> object from a given
     * string of this format: <code>profile, cfgid, finalize|normal</code></p>
     * @param string
     * @return <code>null</code> if there was no profile with the given id, or if
     * <code>variantStr</code> was null, or if the given string was otherwise malformed
     */
    public static IBuildVariant parse(String variantStr) {
        if (variantStr == null) {
            return null;
        }

        String[] variantComponents = PropertyUtil.toStrings(variantStr);
        // Minor hack for legacy profiles.
        int profileManagerType = MoSyncTool.DEFAULT_PROFILE_TYPE;
        if (variantStr.startsWith("*")) {
        	variantStr = variantStr.substring(1);
        	profileManagerType = MoSyncTool.LEGACY_PROFILE_TYPE;
        }


        if (variantComponents.length == 2 || variantComponents.length == 3) {
            String profileStr = variantComponents[0];
            IProfile profile = MoSyncTool.getDefault().
            		getProfileManager(profileManagerType).getProfile(profileStr);
            String cfgId = variantComponents[1];
            if (NULL_CFG.equals(cfgId)) {
                cfgId = null;
            }
        	SortedMap<String, String> specifiers = parseSpecifiers(variantComponents.length > 2 ? variantComponents[2] : null);

            if (profile != null) {
                return new BuildVariant(profile, cfgId, specifiers);
            }
        }

        return null;
    }

    private static SortedMap<String, String> parseSpecifiers(String specifierStr) {
		TreeMap<String, String> result = new TreeMap<String,String>();
		if (specifierStr != null) {
	    	String[] specifierPairs = PropertyUtil.toStrings(specifierStr);
			for (String specifierPair : specifierPairs) {
				String[] specifier = PropertyUtil.toStrings(specifierPair);
				if (specifier.length > 0) {
					result.put(specifier[0], specifier.length > 1 ? specifier[1] : "");
				}
			}
		}

		return result;
	}

	public static String toString(IBuildVariant variant) {
        String profileStr = MoSyncTool.toString(variant.getProfile());
        String cfgId = variant.getConfigurationId();
        if (cfgId == null) {
            cfgId = NULL_CFG;
        }
        String specifierPairs = toString(variant.getSpecifiers());
        return PropertyUtil.fromStrings(new String[] { profileStr, cfgId, specifierPairs });
    }

    private static String toString(SortedMap<String, String> specifiers) {
		ArrayList<String> serializedPairs = new ArrayList<String>();
    	for (Map.Entry<String, String> specifier : specifiers.entrySet()) {
			 serializedPairs.add(PropertyUtil.fromStrings(new String[] { specifier.getKey(), specifier.getValue() }));
		}
    	return PropertyUtil.fromStrings(serializedPairs.toArray(new String[0]));
	}

	@Override
	public String toString() {
        return toString(this);
    }

}
