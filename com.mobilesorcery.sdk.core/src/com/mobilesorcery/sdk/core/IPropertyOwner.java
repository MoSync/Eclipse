package com.mobilesorcery.sdk.core;

public interface IPropertyOwner {

	/**
	 * 
	 * @param key
	 * @param value null removes property
	 * @return true iff the property was set or removed
	 */
    public boolean setProperty(String key, String value);
    public String getProperty(String key);
    public String getDefaultProperty(String key);
    
    /**
     * See IPropertyInitializer.
     * @return
     */
	public String getContext();
	
	/**
	 * This method forces a property to be set,
	 * without notifying any listeners or
	 * checking for value changes. 
	 * @param key
	 * @param value
	 */
	public void initProperty(String key, String value);
}
