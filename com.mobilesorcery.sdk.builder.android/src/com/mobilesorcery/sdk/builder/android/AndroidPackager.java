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
import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.PasswordDialog;

/*
	Built on the JavaMe packager code
*/
public class AndroidPackager 
extends AbstractPackager 
{
	String m_aaptLoc;
	String m_unzipLoc;
	String m_iconInjecLoc;
	
	
	public AndroidPackager ( )
	{
		MoSyncTool tool = MoSyncTool.getDefault( );
		m_unzipLoc      = tool.getBinary( "unzip" ).toOSString( );
		m_aaptLoc       = tool.getBinary( "android/aapt" ).toOSString( );		
		m_iconInjecLoc  = tool.getBinary( "icon-injector" ).toOSString( );
	}
	
	
	public void createPackage ( MoSyncProject project, IBuildVariant variant, IBuildResult buildResult ) 
	throws CoreException 
	{
		DefaultPackager internal = new DefaultPackager(project, variant);
		IProfile targetProfile = variant.getProfile();
		String appName = internal.resolve( "%app-name%" );
		
		internal.setParameters(getParameters());
		internal.setParameter("D", shouldUseDebugRuntimes() ? "D" : ""); 

		try {
			File mosyncBinDir = internal.resolveFile( "%mosync-bin%" );
			File compileOutDir = internal.resolveFile( "%compile-output-dir%" );
			File packageOutDir = internal.resolveFile( "%package-output-dir%" );
			packageOutDir.mkdirs();

			// Delete previous apk file if any
			File projectAPK = new File( packageOutDir, appName + ".apk");
			projectAPK.delete();
			
			//String fixedName = project.getName().replace(' ', '_');
			
			// Create manifest file
			File manifest = new File( packageOutDir, "AndroidManifest.xml" );
			createManifest(project, new Version(internal.getParameters().get(DefaultPackager.APP_VERSION)), manifest);

			// Create layout (main.xml) file
			File main_xml = new File( packageOutDir, "res/layout/main.xml" );
			createMain(project.getName(), main_xml);
			
			// Create values (strings.xml) file, in this file the application name is written
			File strings_xml = new File( packageOutDir, "res/values/strings.xml" );
			createStrings(project.getName(), strings_xml);
			
			//File res = new File( packageOutDir, "res/raw/resources" );
			//res.getParentFile().mkdirs();
			
			File assets = new File( packageOutDir, "add/assets" );
			assets.getParentFile().mkdirs();
			
			File icon = new File(packageOutDir, "res/drawable/icon.png" );
			icon.getParentFile().mkdirs();
			
			// Need to set execution dir, o/w commandline optiones doesn't understand what to do.
			internal.getExecutor().setExecutionDirectory(projectAPK.getParentFile().getParent());
			
			// Copy and rename the program and resource file to package/res/raw/
			Util.copyFile( new NullProgressMonitor( ), 
					       new File( compileOutDir, "program" ), 
					       new File( packageOutDir, "add/assets/program.mp3" ) );
			
			File resources = new File( compileOutDir, "resources" );
			if ( resources.exists( ) == true ) 
			{
				Util.copyFile( new NullProgressMonitor( ), resources, new File( packageOutDir, "add/assets/resources.mp3" ) );
			}
			
			// If there was an icon provided, add it, else use the default icon
			MoSyncIconBuilderVisitor visitor = new MoSyncIconBuilderVisitor();
			visitor.setProject(project.getWrappedProject());
			IResource[] iconFiles = visitor.getIconFiles();
			if (iconFiles.length > 0) {
				Object xObj = targetProfile.getProperties().get( "MA_PROF_CONST_ICONSIZE_X" );
				Object yObj = targetProfile.getProperties().get( "MA_PROF_CONST_ICONSIZE_Y" );
				if (xObj != null && yObj != null) 
				{
					String sizeStr = ((Long) xObj) + "x" + ((Long) yObj);
					internal.runCommandLine( m_iconInjecLoc, 
							                 "-src", 
							                 iconFiles[0].getLocation().toOSString( ), 
						                     "-size", 
						                     sizeStr, 
						                     "-platform", 
						                     "android", 
						                     "-dst", 
						                     new File( packageOutDir, "res/drawable/icon.png" ).getAbsolutePath( ) );
				}
			}
			else 
			{
				// Copy default icon
				Util.copyFile( new NullProgressMonitor( ), 
						       internal.resolveFile( "%mosync-bin%/../etc/icon.png" ), 
						       new File( packageOutDir, "res/drawable/icon.png" ) );
			}
				
			
			// build a resources.ap_ file using aapt tool
			internal.runCommandLine( m_aaptLoc, 
					                "package", 
					                "-f", 
					                "-M", 
					                manifest.getAbsolutePath( ), 
					                "-F",
				                    new File( packageOutDir, "resources.ap_" ).getAbsolutePath( ), 
				                    "-I", 
				                    new File( mosyncBinDir, "android/android-1.5.jar" ).getAbsolutePath( ),
				                    "-S", 
									new File ( packageOutDir, "res" ).getAbsolutePath( ),
									"-0",
									"-A",
									new File ( packageOutDir, "add" ).getAbsolutePath( ) );
						
			// unzip the correct class zip
			File classes = new File( packageOutDir, "classes/class" );
			classes.getParentFile().mkdirs();
			
			internal.runCommandLine( m_unzipLoc, 
					                 "-q", 
					                 internal.resolveFile( "%runtime-dir%/MoSyncRuntime%D%.zip" ).getCanonicalPath(),
					                 "-d",
					                 new File ( packageOutDir, "classes" ).getAbsolutePath( ) );
									 
		
			// move the library file away from the classes directory where it was 
			File library = new File( packageOutDir, "addlib/armeabi" );
			library.getParentFile().mkdirs();
		
			Util.copyFile( new NullProgressMonitor( ), 
						       new File( packageOutDir, "classes/libmosync.so" ), 
						       new File( packageOutDir, "addlib/armeabi/libmosync.so" ) );

			new File( packageOutDir, "classes/libmosync.so" ).delete( );
		
			
			
			// run dx on class file, generating a dex file
			internal.runCommandLine( "java",
					                 "-jar",
					                 new File( mosyncBinDir, "android/dx.jar" ).getAbsolutePath( ),
					                 "--dex",
					                 "--patch-string",
					                 "com/mosync/java/android",
					                 toByteCodePackageName(project.getProperty(PropertyInitializer.ANDROID_PACKAGE_NAME)),
					                 "--output=" + new File( packageOutDir, "classes.dex" ).getAbsolutePath( ),
					                 new File( packageOutDir, "classes" ).getAbsolutePath( ) );
			
			// generate android package , add dex file and resources.ap_ using apkBuilder
			internal.runCommandLine( "java", 
					                 "-jar", 
					                 new File( mosyncBinDir, "android/apkbuilder.jar" ).getAbsolutePath( ), 
					                 internal.resolveFile( "%package-output-dir%/%app-name%_unsigned.apk" ).getAbsolutePath( ),
					                 "-u",
					                 "-z",
					                 new File( packageOutDir, "resources.ap_" ).getAbsolutePath( ),
					                 "-f",
					                 new File( packageOutDir, "classes.dex" ).getAbsolutePath( ),
									 "-nf",
									 new File( packageOutDir, "addlib" ).getAbsolutePath( ) );
			
			// sign apk file using jarSigner
            String keystore = project.getProperty(PropertyInitializer.ANDROID_KEYSTORE);
            String alias = project.getProperty(PropertyInitializer.ANDROID_ALIAS);
            String storepass = getPassword(project, PropertyInitializer.ANDROID_PASS_STORE);
            if (Util.isEmpty(storepass)) {
                throw new IllegalArgumentException("Keystore password missing");
            }
            
            String keypass = getPassword(project, PropertyInitializer.ANDROID_PASS_KEY);
            if (Util.isEmpty(keypass)) {
                throw new IllegalArgumentException("Keystore password missing");
            }
            
            String[] jarSignerCommandLine = new String[] 
            {
                "java", 
                "-jar", 
                new File( mosyncBinDir, "android/tools-stripped.jar" ).getAbsolutePath( ),
                "-keystore", 
                keystore, 
                "-storepass", 
                storepass, 
                "-keypass", 
                keypass,
                "-signedjar", 
                internal.resolveFile( "%package-output-dir%/%app-name%.apk" ).getAbsolutePath( ),
                internal.resolveFile( "%package-output-dir%/%app-name%_unsigned.apk" ).getAbsolutePath( ), 
                alias
            };
            
			internal.runCommandLine(jarSignerCommandLine, "*** COMMAND LINE WITHHELD, CONTAINS PASSWORDS ***");
			
			// Clean up!
			recursiveDel( new File( packageOutDir, "classes" ) );
			recursiveDel( new File( packageOutDir, "res" ) );
			recursiveDel( new File( packageOutDir, "add" ) );
			recursiveDel( new File( packageOutDir, "addlib" ) );
			new File( packageOutDir, "classes.dex" ).delete( );
			new File( packageOutDir, "resources.ap_" ).delete( );
			new File( packageOutDir, "AndroidManifest.xml" ).delete( );
			
			buildResult.setBuildResult(projectAPK);
		}
		catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, "com.mobilesorcery.builder.android", "Could not package for android platform", e));
        }

	}
	
	private String toByteCodePackageName(String name) {
        return name.replace('.', '/');
    }


    private void recursiveDel ( File p )
	{
		if ( p.isFile( ) == false )
		{
			for ( File f : p.listFiles( ) )
				recursiveDel( f );
		}
		
		p.delete( );
	}

	private String getPassword(final MoSyncProject project, final String propertyKey) {
        String pwd = project.getProperty(propertyKey);
        if (Util.isEmpty(pwd)) {
            final String[] result = new String[1];
            Display display = PlatformUI.getWorkbench().getDisplay();
            display.syncExec(new Runnable() {
                public void run() {
                    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                    PasswordDialog dialog = new PasswordDialog(shell);
                    if (dialog.open() == PasswordDialog.OK) {
                        result[0] = dialog.getPassword();
                        if (dialog.shouldRememberPassword()) {
                            project.setProperty(propertyKey, result[0]);
                        }
                    }
                }
            });
            return result[0];
        } else {
            return pwd;
        }
    }

    private void createManifest(MoSyncProject project, Version version, File manifest) throws IOException {
		manifest.getParentFile().mkdirs();
		String projectName = project.getName();
		
		String manifest_string = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
		+"<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
			+"\tpackage=\"com.mosync.app_"+projectName+"\"\n"
			+"\tandroid:versionCode=\"" + project.getProperty(PropertyInitializer.ANDROID_VERSION_CODE) + "\"\n"
			+"\tandroid:versionName=\"" + version.toString() + "\">\n"
			+"\t<application android:icon=\"@drawable/icon\" android:label=\"@string/app_name\">\n"
				+"\t\t<activity android:name=\".MoSync\"\n"
					+"\t\t\tandroid:label=\"@string/app_name\">\n"
					+"\t\t\t<intent-filter>\n"
						+"\t\t\t\t<action android:name=\"android.intent.action.MAIN\" />\n"
						+"\t\t\t\t<category android:name=\"android.intent.category.LAUNCHER\" />\n"
					+"\t\t\t</intent-filter>\n"
				+"\t\t</activity>\n"
				+"<activity android:name=\".MoSyncPanicDialog\"\n"
                  +"android:label=\"@string/app_name\">\n"
				+"</activity>\n"
			+"\t</application>\n"
			+"\t<uses-sdk android:minSdkVersion=\"3\" />\n"
			+"\t<uses-permission android:name=\"android.permission.VIBRATE\" />\n"
			+"\t<uses-permission android:name=\"android.permission.INTERNET\" />\n"
			+"\t<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" />\n"
			+"\t<uses-permission android:name=\"android.permission.READ_PHONE_STATE\" />\n"

/* UNSUPPORTED ON ANDROIND 1.5 Cupcake			
			+"\t<supports-screens"
				+"\t\tandroid:largeScreens=\"true\""
				+"\t\tandroid:normalScreens=\"true\""
				+"\t\tandroid:smallScreens=\"true\""
				+"\t\tandroid:anyDensity=\"true\" />"
*/				
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
	
	/**
	 * Validates a package name (is it a proper android package name?)
	 * @param packageName
	 * @return
	 */
	public static IMessageProvider validatePackageName(String packageName) {
        String[] packageParts = packageName.split("\\.");
        if (packageParts.length < 2) {
            return new DefaultMessageProvider("Android packages must have at least two parts (eg com.test)", IMessageProvider.ERROR);
        }
        
        for (int i = 0; i < packageParts.length; i++) {
            String error = validatePackagePart(packageParts[i]);
            if (error != null) {
                return new DefaultMessageProvider(error, IMessageProvider.ERROR);
            }
        }
        
        return DefaultMessageProvider.EMPTY;
	}

    private static String validatePackagePart(String packagePart) {
        char[] packagePartCh = packagePart.toCharArray();
        if (packagePart.length() == 0) {
            return "Package segment cannot be empty";
        }

        char invalidChar = Character.isJavaIdentifierStart(packagePartCh[0]) ? '\0' : packagePartCh[0];
        
        for (int i = 1; i < packagePartCh.length; i++) {
            invalidChar = Character.isJavaIdentifierPart(packagePartCh[i]) ? '\0' : packagePartCh[i];
        }
        
        if (invalidChar != '\0') {
            return MessageFormat.format("Invalid package character: {0}", invalidChar);
        }
        
        return null;
    }
	
}
