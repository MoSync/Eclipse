package com.mobilesorcery.sdk.core;

public class CUtil {

	private CUtil() { }
	
	/**
	 * Returns whether an identifier is a valid C identifier
	 * @param identifier The identifer to test
	 * @return {@code true} if valid, {@code false} otherwise
	 */
	public static boolean isValidCIdentifier(String identifier) {
		if (identifier.length() == 0) {
			return false;
		}
		for (int i = 0; i < identifier.length(); i++) {
			char ch = identifier.charAt(i);
			if (!idNonDigit(ch) && !idDigit(ch)) {
				return false;
			}
			if (i == 0 && idDigit(ch)) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean idNonDigit(char ch) {
		return (ch == '_' || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch >= 'Z'));
	}
	
	private static boolean idDigit(char ch) {
		return ch >= '0' && ch <= '9';
	}
}
