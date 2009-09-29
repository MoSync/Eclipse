package com.mobilesorcery.sdk.core;

/**
 * <p>An interface for initializing properties in a decoupled manner.</p>
 * <p>An <code>IPropertyInitializer</code> always returns a <emph>context</emph>
 * in which it is applicable; like "mosync projects" -- then this property initializer
 * only affects mosync projects.
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IPropertyInitializer extends IPropertyInitializerDelegate {
	public static final IPropertyInitializer NULL = new IPropertyInitializer() {
		public String getDefaultValue(IPropertyOwner p, String key) {
			return "";
		}

		public String getContext() {
			return null;
		}

		public String getPrefix() {
			return null;
		}	
	};
	
	/**
	 * The prefix that should be prepended to all keys to avoid
	 * naming conflicts. (Think of it as a namespace.)
	 * @return
	 */
	public String getPrefix();
	
	/**
	 * The context in which this initializer applies.
	 * @return
	 */
	public String getContext();
	 
}
