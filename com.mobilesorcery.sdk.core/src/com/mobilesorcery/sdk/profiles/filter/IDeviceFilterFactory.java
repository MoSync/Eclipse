package com.mobilesorcery.sdk.profiles.filter;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.profiles.IDeviceFilter;

/**
 * <p>Constructs an <code>IDeviceFilter</code> from
 * an <code>IMemento</code>.</p>
 * <p>(Similar to an <code>IElementFactory</code>,
 * but does not require any UI plugin dependencies.)</p>
 * @author Mattias Bybro
 *
 */
public interface IDeviceFilterFactory {

	IDeviceFilter createFilter(IMemento child);

}
