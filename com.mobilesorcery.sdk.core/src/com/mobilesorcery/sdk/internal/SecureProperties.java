package com.mobilesorcery.sdk.internal;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.core.ISecurePropertyOwner;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.SecurePropertyException;
import com.mobilesorcery.sdk.core.Util;

/**
 * A utility class for encrypting/decrypting values. Encryption only occurs at
 * save time and decryption only at load time, and the decrypted value is always
 * stored in memory in cleartext.
 * 
 * @author Mattias Bybro
 * 
 */
public class SecureProperties implements ISecurePropertyOwner {

	private final static String NULL_VALUE = "__NULL__";

	public final static String CIPHER = "PBEWithMD5AndDES"; //$NON-NLS-1$

	public final static String KEY_FACTORY = "PBEWithMD5AndDES"; //$NON-NLS-1$

	private IPropertyOwner delegate;

	private HashMap<String, String> cache = new HashMap<String, String>();

	private PBEKeySpec password;

	private String suffix;

	public SecureProperties(IPropertyOwner delegate,
			IProvider<PBEKeySpec, String> passwordProvider,
			String suffix) {
		this.delegate = delegate;
		this.password = passwordProvider.get(null);
		this.suffix = suffix == null ? "" : suffix;
	}

	private Cipher createCipher(int mode, byte[] salt) throws GeneralSecurityException {
		if (password == null) {
			throw new GeneralSecurityException("No password found, cannot encrypt");
		}

		SecretKeyFactory keyFactory;
		keyFactory = SecretKeyFactory.getInstance(KEY_FACTORY);

		SecretKey key = keyFactory.generateSecret(password);

		PBEParameterSpec entropy = new PBEParameterSpec(salt, 8);

		Cipher cipher = Cipher.getInstance(CIPHER);
		cipher.init(mode, key, entropy);
		return cipher;
	}
	
	public static String generateRandomKey() throws GeneralSecurityException {
		SecureRandom rnd = new SecureRandom();
		byte[] result = new byte[16]; 
		rnd.nextBytes(result);
		return Util.toBase16(result);
	}
	
	private static byte[] generateSalt() {
		byte[] salt = new byte[8];
		SecureRandom random = new SecureRandom();
		random.nextBytes(salt);
		return salt;
	}

	public String encrypt(String value) throws GeneralSecurityException {
		if (Util.isEmpty(value)) {
			return null;
		}
		byte[] salt = generateSalt();
		Cipher c = createCipher(Cipher.ENCRYPT_MODE, salt);
		byte[] result = c.doFinal(value.getBytes(Charset.forName("UTF8")));
		return PropertyUtil.fromStrings(new String[] { Util.toBase16(salt), Util.toBase16(result) });
	}

	public String decrypt(String value) throws GeneralSecurityException {
		if (Util.isEmpty(value)) {
			return null;
		}
		String[] saltAndPepper = PropertyUtil.toStrings(value);
		if (saltAndPepper.length != 2) {
			throw new GeneralSecurityException("Invalid encrypted data (salt is missing)");
		}
		byte[] salt = Util.fromBase16(saltAndPepper[0]);
		byte[] toDecrypt = Util.fromBase16(saltAndPepper[1]);
		Cipher c = createCipher(Cipher.DECRYPT_MODE, salt);
		byte[] result = c.doFinal(toDecrypt);
		return new String(result, Charset.forName("UTF8"));
	}

	public boolean setSecureProperty(String key, String value)
			throws SecurePropertyException {
		try {
			String encrypted = encrypt(value);
			cache.put(key, value == null ? NULL_VALUE : value);
			return delegate.setProperty(key + suffix, encrypted);
		} catch (GeneralSecurityException e) {
			throw new SecurePropertyException("Unable to encrypt", e);
		}
	}

	public String getSecureProperty(String key) throws SecurePropertyException {
		String cached = cache.get(key);
		if (cached != null) {
			return cached == NULL_VALUE ? null : cached;
		}

		try {
			String encrypted = delegate.getProperty(key + suffix);
			String decrypted = encrypted == null ? NULL_VALUE : decrypt(encrypted);
			cache.put(key, decrypted);
			return decrypted;
		} catch (GeneralSecurityException e) {
			throw new SecurePropertyException("Unable to decrypt", e);
		}
	}


}
