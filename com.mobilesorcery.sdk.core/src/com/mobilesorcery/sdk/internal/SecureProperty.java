package com.mobilesorcery.sdk.internal;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.Util;

/**
 * A property that is encrypted.
 * 
 * @author Mattias Bybro TODO: Maybe extract this into IProperty?
 */
public class SecureProperty {

	private String NULL_VALUE = "__NULL__";
	
	public String CIPHER = "PBEWithMD5AndDES"; //$NON-NLS-1$

	public String KEY_FACTORY = "PBEWithMD5AndDES"; //$NON-NLS-1$

	private KeySpec password;

	private IPropertyOwner delegate;

	private String cachedValue;

	private String key;

	public SecureProperty(IPropertyOwner delegate, String key, PBEKeySpec password) {
		this.password = password;
		this.key = key;
		this.delegate = delegate;
	}
	
	private Cipher createCipher(int mode) {
		SecretKeyFactory keyFactory;
		try {
			keyFactory = SecretKeyFactory.getInstance(KEY_FACTORY);

			SecretKey key = keyFactory.generateSecret(password);

			byte[] salt = new byte[8];
			SecureRandom random = new SecureRandom();
			random.nextBytes(salt);
			PBEParameterSpec entropy = new PBEParameterSpec(salt, 12);

			Cipher cipher = Cipher.getInstance(CIPHER);
			cipher.init(mode, key, entropy);
			return cipher;
		} catch (Exception e) {
			return null;
		}
	}

	String encrypt(String value) throws GeneralSecurityException {
		if (value == null) {
			return null;
		}
		Cipher c = createCipher(Cipher.ENCRYPT_MODE);
		byte[] result = c.doFinal(value.getBytes(Charset.forName("UTF8")));
		return Util.toBase16(result);
	}

	String decrypt(String value) throws GeneralSecurityException {
		if (value == null) {
			return null;
		}
		byte[] toDecrypt = Util.fromBase16(value);
		Cipher c = createCipher(Cipher.DECRYPT_MODE);
		byte[] result;
		result = c.doFinal(toDecrypt);
		return new String(result, Charset.forName("UTF8"));
	}
	
	public void set(String value) throws GeneralSecurityException {
		cachedValue = value == null ? NULL_VALUE : value;
		String encrypted = value == null ? null : encrypt(value);
		delegate.setProperty(key, encrypted);
	}
	
	public String get() throws GeneralSecurityException {
		if (cachedValue == null) {
			cachedValue = decryptValue();
		} else if (cachedValue == NULL_VALUE) {
			return null;
		}
		
		return cachedValue;
	}

	private String decryptValue() throws GeneralSecurityException {
		String encrypted = delegate.getProperty(key);
		if (encrypted == null) {
			return NULL_VALUE;
		}
		return decrypt(encrypted);
	}
}
