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
package com.mobilesorcery.sdk.builder.android;

import com.mobilesorcery.sdk.internal.builder.MoSyncIconBuilderVisitor;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.IProfile;

/*
	Built on the JavaMe packager code
*/
public class AndroidPackager extends AbstractPackager {

	public void createPackage(MoSyncProject project, IProfile targetProfile, IBuildResult buildResult) throws CoreException {
		
		DefaultPackager internal = new DefaultPackager(project, targetProfile, isFinalizerBuild());
		internal.setParameters(getParameters());
		internal.setParameter("D", shouldUseDebugRuntimes() ? "D" : ""); 

		try {
			File packageOutputDir = internal.resolveFile("%package-output-dir%");
			packageOutputDir.mkdirs();

			// Delete previous apk file if any
			File projectAPK = new File(internal.resolve("%package-output-dir%\\%project-name%.apk"));
			projectAPK.delete();
			
			String fixedName = project.getName().replace(' ', '_');
			
			// Create manifest file
			File manifest = new File(internal.resolve("%package-output-dir%\\AndroidManifest.xml"));
			createManifest(fixedName, manifest);

			// Create layout (main.xml) file
			File main_xml = new File(internal.resolve("%package-output-dir%\\res\\layout\\main.xml"));
			createMain(project.getName(), main_xml);
			
			// Create values (strings.xml) file, in this file the application name is written
			File strings_xml = new File(internal.resolve("%package-output-dir%\\res\\values\\strings.xml"));
			createStrings(project.getName(), strings_xml);
			
			File res = new File(internal.resolve("%package-output-dir%\\res\\raw\\resources"));
			res.getParentFile().mkdirs();
			
			File icon = new File(internal.resolve("%package-output-dir%\\res\\drawable\\icon.png"));
			icon.getParentFile().mkdirs();
			
			// Need to set execution dir, o/w commandline optiones doesn't understand what to do.
			internal.getExecutor().setExecutionDirectory(projectAPK.getParentFile().getParent());
			
			// Copy and rename the program and resource file to package/res/raw/
			internal.runCommandLine("cmd", "/c", "copy", "%compile-output-dir%\\program", "%package-output-dir%\\res\\raw\\program");
			File resources = new File(internal.resolve("%compile-output-dir%\\resources"));
			if (resources.exists()) {
				internal.runCommandLine("cmd", "/c", "copy", "%compile-output-dir%\\resources", "%package-output-dir%\\res\\raw\\resources");
			}
			else
			{
				String dummyResource = "dummy";
				DefaultPackager.writeFile(res, dummyResource);
			}
			
			// If there was an icon provided, add it, else use the default icon
			MoSyncIconBuilderVisitor visitor = new MoSyncIconBuilderVisitor();
			visitor.setProject(project.getWrappedProject());
			IResource[] iconFiles = visitor.getIconFiles();
			if (iconFiles.length > 0) {
				Object xObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_X");
				Object yObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_Y");
				if (xObj != null && yObj != null) {
					String sizeStr = ((Long) xObj) + "x" + ((Long) yObj);
					internal.runCommandLine("%mosync-bin%\\icon-injector", "-src", iconFiles[0].getLocation().toOSString(), 
						"-size", sizeStr, "-platform", "android", "-dst", "%package-output-dir%\\res\\drawable\\icon.png");
				}
			}
			else // Copy default icon
				internal.runCommandLine("cmd", "/c", "copy", "%mosync-bin%\\..\\etc\\icon.png", "%package-output-dir%\\res\\drawable\\icon.png");
			
			// build a resources.ap_ file using aapt tool			
			internal.runCommandLine("%mosync-bin%\\android\\aapt", "package", "-f", "-M", manifest.getAbsolutePath(), "-F",
				"%package-output-dir%\\resources.ap_", "-I", "%mosync-bin%\\android\\android-1.5.jar", "-S", 
				"%package-output-dir%\\res");
						
			// unzip the correct class zip
			File classes = new File(internal.resolve("%package-output-dir%\\classes\\class"));
			classes.getParentFile().mkdirs();
			
			internal.runCommandLine("%mosync-bin%\\unzip", "-q", "%runtime-dir%\\MoSyncRuntime%D%.zip","-d","%package-output-dir%\\classes");
			
			// run dx on class file, generating a dex file
			internal.runCommandLine("java","-jar","%mosync-bin%\\android\\dx.jar","--dex","--patch-string","com/mosync/java/android","com/mosync/app_"+fixedName,"--output=%package-output-dir%\\classes.dex","%package-output-dir%\\classes");
			
			// generate android package , add dex file and resources.ap_ using apkBuilder
			internal.runCommandLine("java", "-jar", "%mosync-bin%\\android\\apkbuilder.jar","%package-output-dir%\\%project-name%_unsigned.apk","-u","-z","%package-output-dir%\\resources.ap_","-f","%package-output-dir%\\classes.dex");
			
			// sign apk file using jarSigner
			internal.runCommandLine("java","-jar","%mosync-bin%\\android\\tools-stripped.jar","-keystore","%mosync-bin%\\..\\etc\\mosync.keystore","-storepass","default","-signedjar","%package-output-dir%\\%project-name%.apk","%package-output-dir%\\%project-name%_unsigned.apk","mosync.keystore");
			
			// Clean up!
			internal.runCommandLine("cmd", "/c", "rd","/s","/q","%package-output-dir%\\classes");
			internal.runCommandLine("cmd", "/c", "rd","/s","/q","%package-output-dir%\\res");
			internal.runCommandLine("cmd", "/c", "del","/q","%package-output-dir%\\classes.dex");
			internal.runCommandLine("cmd", "/c", "del","/q","%package-output-dir%\\resources.ap_");
			internal.runCommandLine("cmd", "/c", "del","/q","%package-output-dir%\\AndroidManifest.xml");
			
			buildResult.setBuildResult(projectAPK);
		}
		catch ( Exception e )
        {
            StringWriter s = new StringWriter( );
            PrintWriter  pr= new PrintWriter( s );
            e.printStackTrace( pr );
			
            // Return stack trace in case of error
            throw new CoreException( new Status( IStatus.ERROR, "com.mobilesorcery.builder.android", s.toString( ) ));
        }

	}

	private void createManifest(String projectName, File manifest) throws IOException {
		manifest.getParentFile().mkdirs();

		String manifest_string = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
		+"<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
			+"\tpackage=\"com.mosync.app_"+projectName+"\"\n"
			+"\tandroid:versionCode=\"1\"\n"
			+"\tandroid:versionName=\"1.0\">\n"
			+"\t<application android:icon=\"@drawable/icon\" android:label=\"@string/app_name\">\n"
				+"\t\t<activity android:name=\".MoSync\"\n"
					+"\t\t\tandroid:label=\"@string/app_name\">\n"
					+"\t\t\t<intent-filter>\n"
						+"\t\t\t\t<action android:name=\"android.intent.action.MAIN\" />\n"
						+"\t\t\t\t<category android:name=\"android.intent.category.LAUNCHER\" />\n"
					+"\t\t\t</intent-filter>\n"
				+"\t\t</activity>\n"
			+"\t</application>\n"
			+"\t<uses-sdk android:minSdkVersion=\"3\" />\n"
			+"\t<uses-permission android:name=\"android.permission.VIBRATE\" />\n"
			+"\t<uses-permission android:name=\"android.permission.INTERNET\" />\n" 
		+"</manifest>\n";
		DefaultPackager.writeFile(manifest, manifest_string);
	}
	
	private void createMain(String projectName, File main_xml) throws IOException {
		main_xml.getParentFile().mkdirs();

		String main_xml_string = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
		+"<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
			+"\tandroid:orientation=\"vertical\"\n"
			+"\tandroid:layout_width=\"fill_parent\"\n"
			+"\tandroid:layout_height=\"fill_parent\"\n"
		+">\n"
		+"</LinearLayout>\n";
		DefaultPackager.writeFile(main_xml, main_xml_string);
	}
	
	private void createStrings(String projectName, File strings_xml) throws IOException {
		strings_xml.getParentFile().mkdirs();

		String strings_xml_string = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
		+"<resources>\n"
			+"\t<string name=\"app_name\">"+projectName+"</string>\n"
		+"</resources>\n";
		DefaultPackager.writeFile(strings_xml, strings_xml_string);
	}
	
}
