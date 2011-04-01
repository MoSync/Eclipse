package com.mobilesorcery.sdk.core;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Platform;

/**
 * Simple property tester that determines if we are in experimental
 * mode or not.
 * 
 * @author fmattias
 */
public class IsExperimentalTester extends PropertyTester
{
	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue)
	{
		return Boolean.TRUE.equals( isExperimental( ) );
	}
	
    /**
     * <p>For development & debugging purposes.</p>
     * <p>Returns how this product's
     * experimental mode was changed at startup (using the
     * <code>-experimental:enable</code> or
     * <code>-experimental:disable</code>
     * command line arguments).
     * @return <code>Boolean.TRUE</code> if experimental mode
     * was enabled at startup, <code>Boolean.FALSE</code> if
     * it was disabled, <code>null</code> if neither.
     */
    public static Boolean isExperimental()
    {
    	List<String> argumentList = Arrays.asList( Platform.getApplicationArgs( ) );
    	if( argumentList.contains( "-experimental:enable" ) )
    	{
    		return true;
    	}
    	if( argumentList.contains("-experimental:disable") )
    	{
    		return false;
    	}
    	
    	return null;
    }
}
