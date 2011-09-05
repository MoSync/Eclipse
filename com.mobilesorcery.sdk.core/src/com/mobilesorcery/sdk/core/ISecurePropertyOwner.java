package com.mobilesorcery.sdk.core;

import java.security.GeneralSecurityException;

import javax.crypto.spec.PBEKeySpec;

import com.mobilesorcery.sdk.internal.SecureProperties;


/**
 * <p>An interface for secure properties.</p>
 * <p>To wrap an {@link IPropertyOwner}, use the {@link SecureProperties} class.</p>
 * @author Mattias Bybro
 *
 */
public interface ISecurePropertyOwner {

	public String getSecureProperty(String key) throws SecurePropertyException;

	public boolean setSecureProperty(String key, String value) throws SecurePropertyException;

	void resetMasterPassword(IProvider<PBEKeySpec, String> newPasswordProvider) throws SecurePropertyException, GeneralSecurityException;
}
