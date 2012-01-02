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
package com.mobilesorcery.sdk.builder.winmobile;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PackageToolPackager;

public class WinMobilePackager extends PackageToolPackager {

	@Override
	public Map<String, List<File>> computeBuildResult(MoSyncProject project, IBuildVariant variant) {
		DefaultPackager internal = new DefaultPackager(project, variant);
		File cabFile = new File(internal.resolve("%package-output-dir%/%app-name%.cab"));
		return createBuildResult(cabFile);
	}

}
