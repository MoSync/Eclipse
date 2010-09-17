package com.mobilesorcery.sdk.builder.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mobilesorcery.sdk.core.MoSyncTool;

/**
 * <p>
 * A utility class for handling certificates stored in a key store.
 * </p>
 * 
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 * 
 */
public class KeystoreCertificateInfo {

    private String keystoreLocation;
    private String alias;
    private String keystorePassword;
    private String keyPassword;

    public KeystoreCertificateInfo(String keystoreLocation, String alias, String keystorePassword, String keyPassword) {
        this.keystoreLocation = keystoreLocation;
        this.alias = alias;
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
    }
    
    public String getKeystoreLocation() {
        return keystoreLocation;
    }

    public void setKeystoreLocation(String keystoreLocation) {
        this.keystoreLocation = keystoreLocation;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public static List<KeystoreCertificateInfo> parseList(String input) throws IllegalArgumentException {
        List<KeystoreCertificateInfo> resultList = new ArrayList<KeystoreCertificateInfo>();
        JSONParser parser = new JSONParser();
        try {
            JSONArray keystoreCertificateInfos = (JSONArray) parser.parse(input);
            for (int i = 0; i < keystoreCertificateInfos.size(); i++) {
                JSONObject keystoreCertificateInfo = (JSONObject) keystoreCertificateInfos.get(i);
                String keystore = (String) keystoreCertificateInfo.get("keystore");
                String alias = (String) keystoreCertificateInfo.get("alias");
                String keystorePass = (String) keystoreCertificateInfo.get("keystorePass");
                String keyPass = (String) keystoreCertificateInfo.get("keyPass");
                KeystoreCertificateInfo result = new KeystoreCertificateInfo(keystore, alias, keystorePass, keyPass);
                resultList.add(result);
            }
            
            return resultList;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid keystore certificate info", e);
        }
    }
    
    public static KeystoreCertificateInfo parseOne(String input) {
        List<KeystoreCertificateInfo> result = parseList(input);
        if (result.size() < 1) {
            return null;
        }
        return result.get(0);
    }
    
    public static String unparse(KeystoreCertificateInfo input) {
        return unparse(Arrays.asList(input));
    }
    
    public static String unparse(List<KeystoreCertificateInfo> input) {
        JSONArray result = new JSONArray();
        for (KeystoreCertificateInfo info : input) {
            JSONObject unparsedInfo = new JSONObject();
            unparsedInfo.put("keystore", info.getKeystoreLocation());
            unparsedInfo.put("alias", info.getAlias());
            unparsedInfo.put("keystorePass", info.getKeystorePassword());
            unparsedInfo.put("keyPass", info.getKeyPassword());
            result.add(unparsedInfo);
        }
        return result.toString();
    }
    
    public static KeystoreCertificateInfo createDefault() {
        String defaultKeystore = MoSyncTool.getDefault().getMoSyncHome().append("etc/mosync.keystore").toOSString();
        KeystoreCertificateInfo defaultInfo = new KeystoreCertificateInfo(defaultKeystore, "mosync.keystore", "default", "default");
        return defaultInfo;
    }
}
