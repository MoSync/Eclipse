package com.mobilesorcery.sdk.internal.convert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.IBuildConfiguration;
import com.mobilesorcery.sdk.core.IPropertyOwner;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncProject.IConverter;
import com.mobilesorcery.sdk.core.PropertyUtil;
import com.mobilesorcery.sdk.core.Version;

public class MoSyncProjectConverter1_8 implements IConverter {

	public final static Version VERSION = new Version("1.8");

	private static IConverter instance = new MoSyncProjectConverter1_8();

	public static IConverter getInstance() {
		return instance;
	}

	@Override
	public void convert(MoSyncProject project) throws CoreException {
		if (!VERSION.isNewer(project.getFormatVersion()))
			return;

		// reset all compiler flags, include paths and libraries.
		for (String cfg : project.getBuildConfigurations()) {
			IPropertyOwner cfgProperties = MoSyncBuilder.getPropertyOwner(project, cfg);

			IPath[] libs = PropertyUtil.getPaths(cfgProperties, MoSyncBuilder.ADDITIONAL_LIBRARIES);
			boolean hasMastd = false;
			boolean hasNewlib = false;
			boolean hasStlport = false;
			boolean debug = false;
			for (IPath lib : libs) {
				String lc = lib.toString().toLowerCase();
				if(lc.contains("mastd.lib"))
					hasMastd = true;
				if(lc.contains("mastdd.lib"))
					hasMastd = debug = true;
				if(lc.contains("newlib.lib"))
					hasNewlib = true;
				if(lc.contains("newlibd.lib"))
					hasNewlib = debug = true;
				if(lc.contains("stlport.lib"))
					hasStlport = true;
				if(lc.contains("stlportd.lib"))
					hasStlport = debug = true;
			}
			if(cfg.equals(IBuildConfiguration.DEBUG_TYPE))
				debug = true;
			if(hasMastd || hasNewlib || hasStlport) {
				int neg = 0;
				if(hasMastd)
					neg++;
				if(hasNewlib)
					neg++;
				if(hasStlport)
					neg++;
				IPath[] fixedLibs = new IPath[libs.length - neg];
				for (int i=0, j=0; i<libs.length; i++) {
					IPath lib = libs[i];
					String ls = lib.toString();
					String lc = ls.toLowerCase();
					if(lc.contains("mastd.lib") || lc.contains("mastdd.lib") ||
						lc.contains("newlib.lib") || lc.contains("newlibd.lib") ||
						lc.contains("stlport.lib") || lc.contains("stlport.lib"))
						continue;
					if(debug) {
						lib = new Path(ls.replace("d.lib", ".lib").replace("D.lib", ".lib"));
					}
					fixedLibs[j++] = lib;
				}
				PropertyUtil.setPaths(cfgProperties, MoSyncBuilder.ADDITIONAL_LIBRARIES, fixedLibs);

				if(cfgProperties.getProperty(MoSyncBuilder.STANDARD_LIBRARIES) == null) {
					if(hasStlport) {
						cfgProperties.setProperty(MoSyncBuilder.STANDARD_LIBRARIES, MoSyncBuilder.STANDARD_LIBRARIES_STL);
					} else if(hasNewlib) {
						cfgProperties.setProperty(MoSyncBuilder.STANDARD_LIBRARIES, MoSyncBuilder.STANDARD_LIBRARIES_LIBC);
					} else {
						cfgProperties.setProperty(MoSyncBuilder.STANDARD_LIBRARIES, MoSyncBuilder.STANDARD_LIBRARIES_MASTD);
					}
				}

				cfgProperties.setProperty(MoSyncBuilder.ADDITIONAL_INCLUDE_PATHS, "");
				cfgProperties.setProperty(MoSyncBuilder.ADDITIONAL_LIBRARY_PATHS, "");

				PropertyUtil.setBoolean(cfgProperties, MoSyncBuilder.IGNORE_DEFAULT_INCLUDE_PATHS, false);
				PropertyUtil.setBoolean(cfgProperties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARIES, false);
				PropertyUtil.setBoolean(cfgProperties, MoSyncBuilder.IGNORE_DEFAULT_LIBRARY_PATHS, false);
			}

			cfgProperties.setProperty(MoSyncBuilder.EXTRA_COMPILER_SWITCHES, debug ? "" : "-O2 -fomit-frame-pointer");
			cfgProperties.setProperty(MoSyncBuilder.EXTRA_LINK_SWITCHES, "");
			cfgProperties.setProperty(MoSyncBuilder.GCC_WARNINGS, "30");	// Werror, Wall, Wextra & moar.
		}
	}
}
