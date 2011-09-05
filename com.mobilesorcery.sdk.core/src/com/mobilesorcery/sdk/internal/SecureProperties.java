package com.mobilesorcery.sdk.internal;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

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

	/**
	 * A default suffix for properties to indicate them to be secure properties;
	 * secure properties are usually stored in the local project file.
	 */
	public static final String DEFAULT_SECURE_PROPERTY_SUFFIX = ".secure";

	private final IPropertyOwner delegate;

	private final HashMap<String, String> cache = new HashMap<String, String>();

	private IProvider<PBEKeySpec, String> passwordProvider;

	private PBEKeySpec password;

	private boolean passwordResolved = false;

	private final String suffix;

	/**
	 * Creates a new secure properties container.
	 * @param delegate
	 * @param passwordProvider A container for the encryption key; if
	 * this container returns {@code null}, no encryption will be
	 * performed
	 * @param suffix A suffix to add to property keys to indicate
	 * a secure property; if null orempty {@link #DEFAULT_SECURE_PROPERTY_SUFFIX}
	 * will be used.
	 * NOTE: We may want to refactor this.
	 */
	public SecureProperties(IPropertyOwner delegate,
			IProvider<PBEKeySpec, String> passwordProvider,
			String suffix) {
		this.delegate = delegate;
		this.passwordProvider = passwordProvider;
		this.suffix = Util.isEmpty(suffix) ? DEFAULT_SECURE_PROPERTY_SUFFIX : suffix;
	}

	private Cipher createCipher(int mode, byte[] salt) throws GeneralSecurityException {
		SecretKeyFactory keyFactory;
		keyFactory = SecretKeyFactory.getInstance(KEY_FACTORY);

		SecretKey key = keyFactory.generateSecret(password);

		PBEParameterSpec entropy = new PBEParameterSpec(salt, 8);

		Cipher cipher = Cipher.getInstance(CIPHER);
		cipher.init(mode, key, entropy);
		return cipher;
	}

	public static String generateRandomKey() throws GeneralSecurityException {
		return generateRandomKey(512);
	}

	public static String generateRandomKey(int bits) throws GeneralSecurityException {
		SecureRandom rnd = new SecureRandom();
		byte[] result = new byte[bits / 8];
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
		resolvePassword();

		if (Util.isEmpty(value) || password == null) {
			return value;
		}

		byte[] salt = generateSalt();
		Cipher c = createCipher(Cipher.ENCRYPT_MODE, salt);
		byte[] result = c.doFinal(value.getBytes(Charset.forName("UTF8")));
		return PropertyUtil.fromStrings(new String[] { Util.toBase16(salt), Util.toBase16(result) });
	}

	public String decrypt(String value) throws GeneralSecurityException {
		resolvePassword();

		if (Util.isEmpty(value) || password == null) {
			return value;
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

	@Override
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

	@Override
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

	@Override
	public void resetMasterPassword(IProvider<PBEKeySpec, String> newPasswordProvider) throws GeneralSecurityException, SecurePropertyException {
		// Make sure we have the old password!
		resolvePassword();
		Map<String, String> delegateProperties = delegate.getProperties();
		Map<String, String> decryptedProperties = new HashMap<String, String>();
		for (String key : delegateProperties.keySet()) {
			// Bah, overly complicated with all these suffices, will do for now.
			String secureKey = getSecurePropertyKey(key);
			if (secureKey != null) {
				String decrypted = decryptOrClear(delegateProperties.get(key));
				decryptedProperties.put(secureKey, decrypted);
			}
		}
		// Ok, no exception!
		this.passwordProvider = newPasswordProvider;
		this.passwordResolved = false;
		resolvePassword();
		this.cache.clear();
		for (String key : decryptedProperties.keySet()) {
			setSecureProperty(key, decryptedProperties.get(key));
		}
	}

	private String decryptOrClear(String value) {
		try {
			return decrypt(value);
		} catch (GeneralSecurityException e) {
			return null;
		}
	}

	private void resolvePassword() throws GeneralSecurityException {
		if (!passwordResolved) {
			// MUST be lazily evaluated, see bug report MOSYNCTWOSIX-115.
			password = passwordProvider.get(null);
			passwordResolved = true;
		}
	}

	private String getSecurePropertyKey(String key) {
		if (key.endsWith(suffix)) {
			return key.substring(0, key.length() - suffix.length());
		}
		return null;
	}

}
