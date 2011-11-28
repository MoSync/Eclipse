package com.mobilesorcery.sdk.builder.winmobilecs;

import java.io.File;

import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.PackageToolPackager;
import com.mobilesorcery.sdk.profiles.IProfile;

public class WinMobileCSPackager extends PackageToolPackager {

	@Override
	public File computeBuildResult(MoSyncProject project, IBuildVariant variant) {
		DefaultPackager internal = new DefaultPackager(project, variant);
		File csProjFile = new File(
				internal.resolve("%package-output-dir%/project/mosync.csproj"));
		// Please note: not a full app yet!
		return csProjFile;
	}

	@Override
	public String getGenerateMode(IProfile profile) {
		return BUILD_GEN_CS_MODE;
	}

}
