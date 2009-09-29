package com.mobilesorcery.sdk.profiles;

import java.util.Map;

import com.mobilesorcery.sdk.core.Filter;
import com.mobilesorcery.sdk.core.IPackager;

public interface IProfile {

    /**
     * The property key for horizontal screen size
     */
    String SCREEN_SIZE_X = "MA_PROF_CONST_SCREENSIZE_X";

    /**
     * The property key for vertical screen size
     */
    String SCREEN_SIZE_Y = "MA_PROF_CONST_SCREENSIZE_Y";

    /**
     * Returns the vendor of this handset profile
     * @return
     */
    IVendor getVendor();
    
    /**
     * Returns the name of this handset profile
     * @return
     */
    String getName();
    
    
    /**
     * Returns a map of properties, a property
     * may have any of theses types
     * <ul>
     * <li>String
     * <li>Long
     * <li>Boolean
     * </ul>
     */
    Map<String, Object> getProperties();
    
    /**
     * Returns a filtered map of properties
     * @param filter
     * @return
     */
    Map<String, Object> getProperties(Filter<String> filter);
    
    /**
     * Returns the platform of this profile,
     * eg <code>runtime/java/4</code>
     * @return
     */
    String getPlatform();
    
    IPackager getPackager();
    
    /**
     * Returns a flag indicating whether this device is an emulator
     * or not.
     * @return
     */
    boolean isEmulator();
}
