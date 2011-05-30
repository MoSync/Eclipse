package com.mobilesorcery.sdk.builder.java;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IMessageProvider;

import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.ISecurePropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.PreferenceStorePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.SecurePropertyException;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;

/**
 * <p>
 * A utility class for handling certificates stored in a key store.
 * </p>
 * 
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 * 
 */
public class KeystoreCertificateInfo {

	public static final String KEYSTORE_LOCATION_SUFFIX = ".location";
	public static final String ALIAS_SUFFIX = ".alias";
	public static final String KEYSTORE_PWD_SUFFIX = ".store.pwd";
	public static final String KEY_PWD_SUFFIX = ".key.pwd";
	public static final String PASSWORDS_IN_CLEARTEXT = ".enc.pwd";
	
    private String keystoreLocation;
    private String alias;
    private String keystorePassword;
    private String keyPassword;
	private boolean shouldEncryptPasswords = true;
	private SecurePropertyException exception;

    public KeystoreCertificateInfo() {
    	
    }
    
    public KeystoreCertificateInfo(String keystoreLocation, String alias, String keystorePassword, String keyPassword, boolean shouldEncryptPasswords) {
        this.keystoreLocation = keystoreLocation;
        this.alias = alias;
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
        this.shouldEncryptPasswords = shouldEncryptPasswords;
        
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
    
    public boolean shouldEncryptPasswords() {
    	return shouldEncryptPasswords;
    }
    
    public IMessageProvider validate() {
    	String msg = null;
    	int type = IMessageProvider.NONE;
    	if (exception != null) {
    		msg = "Could not read encrypted passwords (this happens for shared projects, for security reasons).";
    		type = IMessageProvider.WARNING;
    	} else if (Util.isEmpty(keyPassword) || Util.isEmpty(keystorePassword)) {
    		msg = "Empty passwords are not allowed (shared projects do not share passwords for security reasons)";
    		type = IMessageProvider.WARNING;
    	}
    	return new DefaultMessageProvider(msg, type);
    }
    
    /**
     * <p>Loads this {@link KeystoreCertificateInfo} with
     * information from non-encrypted and encrypted storages (passwords are encrypted).</p>
     * <p>If decryption failed (for example due to no wrong password),
     * the {@link #validate()} method will return a non-<code>null</code> error message.</p>
     * @param baseKey the key that is used as a base to store the information; the properties
     * actually used will be <code>baseKey</code> + a suffix for each property + an ordinal marker.
     * @param storage
     * @param secureStorage 
     */
    public static List<KeystoreCertificateInfo> load(String baseKey, IPropertyOwner storage, ISecurePropertyOwner secureStorage) {
    	int ordinal = 0;
    	boolean mayHaveMore = true;
    	ArrayList<KeystoreCertificateInfo> result = new ArrayList<KeystoreCertificateInfo>();
    	while (mayHaveMore) {
    		KeystoreCertificateInfo info = new KeystoreCertificateInfo();
    		mayHaveMore = info.load(baseKey, storage, secureStorage, ordinal);
    		if (mayHaveMore) {
    			result.add(info);
    		}
    		ordinal++;
    	}
    	return result;
	}
    
    public static KeystoreCertificateInfo loadOne(String baseKey, IPropertyOwner storage, ISecurePropertyOwner secureStorage) {
    	List<KeystoreCertificateInfo> result = load(baseKey, storage, secureStorage);
    	return result.size() > 0 ? result.get(0) : null;
    }
    
    private boolean load(String baseKey, IPropertyOwner storage, ISecurePropertyOwner secureStorage, int ordinal) {
      	keystoreLocation = storage.getProperty(baseKey + KEYSTORE_LOCATION_SUFFIX + "." + ordinal);
      	alias = storage.getProperty(baseKey + ALIAS_SUFFIX + "." + ordinal);
      	shouldEncryptPasswords = !PropertyUtil.getBoolean(storage, baseKey + PASSWORDS_IN_CLEARTEXT + "." + ordinal);
      	
      	if (shouldEncryptPasswords) {
      		try {
      			keystorePassword = secureStorage.getSecureProperty(baseKey + KEYSTORE_PWD_SUFFIX + "." + ordinal);
      			keyPassword = secureStorage.getSecureProperty(baseKey + KEY_PWD_SUFFIX + "." + ordinal);
      		} catch (SecurePropertyException e) {
      			exception = e;
      		}
      	} else {
      		keystorePassword = storage.getProperty(baseKey + KEYSTORE_PWD_SUFFIX + "." + ordinal);
      		keyPassword = storage.getProperty(baseKey + KEY_PWD_SUFFIX + "." + ordinal);      		
      	}
      	
      	return !(Util.isEmpty(keystoreLocation) && Util.isEmpty(alias) && Util.isEmpty(keyPassword) && Util.isEmpty(keystorePassword));
    }
    
    public static void store(List<KeystoreCertificateInfo> infoList, String baseKey, IPropertyOwner storage, ISecurePropertyOwner secureStorage) throws SecurePropertyException {
    	List<KeystoreCertificateInfo> oldies = load(baseKey, storage, secureStorage);
    	int ordinal = 0;
    	for (KeystoreCertificateInfo info : infoList) {
    		info.store(baseKey, storage, secureStorage, ordinal);
    		ordinal++;
    	}
    	int oldCount = oldies.size();
    	for (int i = ordinal; i < oldCount; i++) {
    		clear(baseKey, storage, secureStorage, i, oldies.get(i));
    	}
    }
    
    private static void clear(String baseKey, IPropertyOwner storage,
			ISecurePropertyOwner secureStorage, int ordinal, KeystoreCertificateInfo info) throws SecurePropertyException {
		info.alias = null;
		info.keyPassword = null;
		info.keystoreLocation = null;
		info.keystorePassword = null;
		// But we keep the 'shouldEncryptPasswords' attribute
		info.store(baseKey, storage, secureStorage, ordinal);
	}

	public void store(String baseKey, IPropertyOwner storage, ISecurePropertyOwner secureStorage) throws SecurePropertyException {
    	store(baseKey, storage, secureStorage, 0);
    }
    
    private void store(String baseKey, IPropertyOwner storage, ISecurePropertyOwner secureStorage, int ordinal) throws SecurePropertyException {
       	PropertyUtil.setBoolean(storage, baseKey + PASSWORDS_IN_CLEARTEXT + "." + ordinal, !shouldEncryptPasswords);
    	storage.setProperty(baseKey + KEYSTORE_LOCATION_SUFFIX  + "." + ordinal, keystoreLocation);
    	storage.setProperty(baseKey + ALIAS_SUFFIX + "." + ordinal, alias);
    	if (shouldEncryptPasswords) {
       		secureStorage.setSecureProperty(baseKey + KEYSTORE_PWD_SUFFIX + "." + ordinal, keystorePassword);
    		secureStorage.setSecureProperty(baseKey + KEY_PWD_SUFFIX + "." + ordinal, keyPassword);
    	} else {
       		storage.setProperty(baseKey + KEYSTORE_PWD_SUFFIX + "." + ordinal, keystorePassword);
    		storage.setProperty(baseKey + KEY_PWD_SUFFIX + "." + ordinal, keyPassword);   		
    	}
    }

    public static KeystoreCertificateInfo createDefault() {
        String defaultKeystore = MoSyncTool.getDefault().getMoSyncHome().append("etc/mosync.keystore").toOSString();
        KeystoreCertificateInfo defaultInfo = new KeystoreCertificateInfo(defaultKeystore, "mosync.keystore", "default", "default", false);
        return defaultInfo;
    }

}
