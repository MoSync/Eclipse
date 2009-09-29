package com.mobilesorcery.sdk.profiles;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>A parser for profile information files - by convention located in
 * the <code>maprofile.h</code> file in the profile directory.</p>
 * 
 * @author Mattias Bybro, mattias.bybro@purplescout.com/mattias@bybro.com
 *
 */
public class ProfileParser {

    public IProfile parseInfoFile(IVendor vendor, String profileName, File profileInfoFile, File platformInfoFile) throws IOException {
        FileReader profileInfo = new FileReader(profileInfoFile);
        FileReader platformInfo = new FileReader(platformInfoFile);
        try {
            return parseInfoFile(vendor, profileName, profileInfo, platformInfo);
        } finally {
            if (profileInfo != null) profileInfo.close();
            if (platformInfo != null) platformInfo.close();
        }
    }

    public IProfile parseInfoFile(IVendor vendor, String profileName, Reader profileInfo, Reader platformInfo) throws IOException {
        Profile profile = new Profile(vendor, profileName);        
        Map<String, Object> properties = profile.getModifiableProperties();
        LineNumberReader reader = new LineNumberReader(profileInfo);
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.trim();
            if (line.startsWith("#define")) {
                // Case 1, 2: value as string or integer
                // #define IDENTIFIER rest-of-line
                // Case 3: flag
                // #define IDENTIFIER
                String[] components = line.split("\\s+", 3);
                if (components.length > 1) {
                    String identifier = components[1];
                    if (components.length > 2) {
                        String restOfLine = components[2];
                        Long longValue = parseCInteger(restOfLine);
                        properties.put(identifier, longValue == null ? restOfLine : longValue);
                    } else {                    
                        properties.put(identifier, true);
                    }
                }
                
                
            }
        }
        
        updatePlatform(profile, platformInfo);
        return profile; 
    }
    
    private void updatePlatform(Profile profile, Reader platformInfo) throws IOException {
        LineNumberReader reader = new LineNumberReader(platformInfo);
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.trim().length() != 0) {
                profile.setPlatform(line.trim());
            }
        }
    }

    /**
     * Parses the <code>definitions.txt</code> file that contains
     * a user-friendly description of each feature flag.
     * @param reader
     * @return
     * @throws IOException 
     */
    public Map<String, String> parseFeatureDescriptionFile(File featureDescriptionsFile) throws IOException {
        FileReader featureDescriptions = new FileReader(featureDescriptionsFile);
        try {
            return parseFeatureDescriptionFile(featureDescriptions);
        } finally {
            if (featureDescriptions != null) featureDescriptions.close();
        }
    }
    
    public Map<String, String> parseFeatureDescriptionFile(Reader featureDescriptions) throws IOException {
        Map<String, String> result = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        
        LineNumberReader reader = new LineNumberReader(featureDescriptions);
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            String[] components = line.split(",", 2);
            if (components.length == 2) {
                String feature = components[0].trim();
                String description = components[1].trim();
                result.put(feature, description);
            }
        }
        
        return result;
    }

    /**
     * Simple method to parse simple decimal numbers 
     * returns <code>null</code> if not a number
     * @param restOfLine
     * @return
     */
    private Long parseCInteger(String str) {
        try {
            return Long.parseLong(str, 10);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
