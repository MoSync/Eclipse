/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core.security;

import java.util.List;

public interface IApplicationPermissions {

    public List<String> getRequestedPermissions(boolean includeChildren);

    public void resetRequestedPermissions(List<String> required);

    public void setRequestedPermission(String required, boolean set);

    public void setRequestedPermissions(List<String> required, boolean set);

    /**
     * <p>Returns <code>true</code> if the specified permission
     * is required.</p>
     * @param key
     * @return
     */
    public boolean isPermissionRequested(String key);

    public List<String> getAvailablePermissions(String parentPermission);

    public IApplicationPermissions createWorkingCopy();

    public void apply(IApplicationPermissions workingCopy);

}
