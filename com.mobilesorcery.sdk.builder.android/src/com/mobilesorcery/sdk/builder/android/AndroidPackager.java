/*  Copyright (C) 2010 MoSync AB

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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;

import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.IconManager;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.core.security.IApplicationPermissions;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.DefaultMessageProvider;
import com.mobilesorcery.sdk.ui.UIUtils;

/*
	Built on the JavaMe packager code
*/
public class AndroidPackager 
extends AbstractPackager 
{
	public final static String ID = "com.mobilesorcery.sdk.build.android.packager";
	
	String m_aaptLoc;
	String m_unzipLoc;
	String m_iconInjecLoc;
	
	int m_AndroidVersion;
	
	public AndroidPackager ( )
	{
		MoSyncTool tool = MoSyncTool.getDefault( );
		m_unzipLoc      = tool.getBinary( "unzip" ).toOSString( );
		m_aaptLoc       = tool.getBinary( "android/aapt" ).toOSString( );		
	}
	
	public void createPackage ( MoSyncProject project, IBuildVariant variant, IBuildResult buildResult ) 
	throws CoreException 
	{
		// Create a console which can be used for debug messaging to the Eclispe console
		IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(MoSyncBuilder.CONSOLE_ID);
		
		DefaultPackager internal = new DefaultPackager(project, variant);
		IProfile targetProfile = variant.getProfile();
		String appName = internal.getParameters().get( DefaultPackager.APP_NAME );
		
		internal.setParameters(getParameters());
		internal.setParameter("D", shouldUseDebugRuntimes() ? "D" : ""); 
		
		m_AndroidVersion = getAndroidPlatformVersion(internal);
		
		try {
			
			if(m_AndroidVersion == -1) throw new Exception("No Available Android runtime " + internal.getParameters().get( DefaultPackager.PLATFORM_ID ).toString() );
			
			File mosyncBinDir = internal.resolveFile( "%mosync-bin%" );
			File compileOutDir = internal.resolveFile( "%compile-output-dir%" );
			File packageOutDir = internal.resolveFile( "%package-output-dir%" );
			packageOutDir.mkdirs();

			File defaultIcon = internal.resolveFile( "%mosync-bin%/../etc/icon.png" );
			
			// Delete previous apk file if any
			File projectAPK = new File( packageOutDir, appName + ".apk");
			projectAPK.delete();
			
			//String fixedName = project.getName().replace(' ', '_');
			String packageName = project.getProperty(PropertyInitializer.ANDROID_PACKAGE_NAME);
			
			// Create manifest file
			File manifest = new File( packageOutDir, "AndroidManifest.xml" );
			createManifest(project, new Version(internal.getParameters().get(DefaultPackager.APP_VERSION)), packageName, manifest);

			// Create layout (main.xml) file
			File main_xml = new File( packageOutDir, "res/layout/main.xml" );
			createMain(main_xml);
			
			// Create values (strings.xml) file, in this file the application name is written
			File strings_xml = new File( packageOutDir, "res/values/strings.xml" );
			createStrings(appName, strings_xml);
			
			//File res = new File( packageOutDir, "res/raw/resources" );
			//res.getParentFile().mkdirs();
			
			File assets = new File( packageOutDir, "add/assets" );
			assets.getParentFile().mkdirs();
			
			File icon = new File(packageOutDir, "res/drawable/icon.png" );
			icon.getParentFile().mkdirs();
			
			// Need to set execution dir, o/w commandline options doesn't understand what to do.
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
			
			// Use The IconManager built for Moblin to add icons to the Android package
			try
            {
                File f;
                File outDir = new File( internal.resolve( "%compile-output-dir%" ) );            	
                IconManager iconManager = new IconManager( internal,
	            						project );
          
                // Get the correct icon size
                // TODO: Make it better since this is quite the hack
                int screenWidth = ((Long)(targetProfile.getProperties().get( "MA_PROF_CONST_SCREENSIZE_X" ))).intValue();
				
				// screen 240x320, 240x400, 240x432  	-> 30x30
				// srceen 320x480					   	-> 40x40
				// screen 480x800, 480x854				-> 60x60
				
				int iconSize = 40;
				if(screenWidth<=240) iconSize = 30;
				else if(screenWidth>=480) iconSize = 60;
				
				
	            // Set PNG icons
	            if ( iconManager.hasIcon( "png" ) == true )
	            {
	                try 
		            {
		                f = new File( packageOutDir, "res/drawable/icon.png" );
		                if ( f.exists( ) == true )
		                	f.delete( );
		                if ( iconManager.inject( f, iconSize, iconSize, "png" ) == false )
		                {
		                	console.addMessage("Unable to inject the icon, using default!");
		                	setDefaultIcon(defaultIcon, packageOutDir);
		                }
		                else
		                {
		                	console.addMessage("Icon was injected succesfully!");
		                }
		            }
		            catch ( Exception e ) 
		            {
		            	buildResult.addError( e.getMessage( ) );
		            }
	            }
	            else
	            {
	            	console.addMessage("Using default icon!");
	            	setDefaultIcon(defaultIcon, packageOutDir);
	            }
            }
			catch ( Exception e ) 
            {
            	buildResult.addError( e.getMessage( ) );
            }
			
			// use the correct jar file for this build
			String androidJAR = "android/android-" + m_AndroidVersion + ".jar";
			
			// build a resources.ap_ file using aapt tool
			internal.runCommandLine( m_aaptLoc, 
					                "package", 
					                "-f", 
					                "-M", 
					                manifest.getAbsolutePath( ), 
					                "-F",
				                    new File( packageOutDir, "resources.ap_" ).getAbsolutePath( ), 
				                    "-I", 
				                    new File( mosyncBinDir, androidJAR ).getAbsolutePath( ),
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
					                 toByteCodePackageName(packageName),
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
            String keystoreCertInfoStr = project.getProperty(PropertyInitializer.ANDROID_KEYSTORE_CERT_INFO);
            KeystoreCertificateInfo keystoreCertInfo = null;
            
            try {
                keystoreCertInfo = KeystoreCertificateInfo.parseOne(keystoreCertInfoStr);

            } catch (IllegalArgumentException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid or missing certificate", e));
            }

            if (keystoreCertInfo != null) {
                String keystore = keystoreCertInfo.getKeystoreLocation();
                String alias = keystoreCertInfo.getAlias();
                String storepass = keystoreCertInfo.getKeystorePassword();
                if (Util.isEmpty(storepass)) {
                    throw new IllegalArgumentException("Keystore password missing");
                }
                
                String keypass = keystoreCertInfo.getKeyPassword();
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
            }
			// Clean up!
			recursiveDel( new File( packageOutDir, "classes" ) );
			recursiveDel( new File( packageOutDir, "res" ) );
			recursiveDel( new File( packageOutDir, "add" ) );
			recursiveDel( new File( packageOutDir, "addlib" ) );
			new File( packageOutDir, "classes.dex" ).delete( );
			new File( packageOutDir, "resources.ap_" ).delete( );
			new File( packageOutDir, "AndroidManifest.xml" ).delete( );
			
			buildResult.setBuildResult(projectAPK);
		} catch (CoreException e) {
		    throw e;
		} catch (Exception e) {
			console.addMessage("Build exception : " + e.toString());
            throw new CoreException(new Status(IStatus.ERROR, "com.mobilesorcery.builder.android", "Could not package for android platform", e));
        }

	}
	
	private int getAndroidPlatformVersion(DefaultPackager internal)
	{	
		// determine which Android version we are building for
		String platformID = internal.getParameters().get( DefaultPackager.PLATFORM_ID );
		if(platformID.startsWith("android_3")) return 3;
		else if(platformID.startsWith("android_4")) return 4;
		else if(platformID.startsWith("android_7")) return 7;
		
		return -1;
	}
	
	/**
	 * Sets the default Android icon for the application
	 * 
	 * @param defaultIcon		A file handle to the default icon			
	 * @param packageOutDir		The folder in which the Android package is stored in
	 */
	private void setDefaultIcon(File defaultIcon, File packageOutDir)
	{
		try
		{
			// Copy default icon
			Util.copyFile( new NullProgressMonitor( ), defaultIcon, new File( packageOutDir, "res/drawable/icon.png" ) );
		}
		catch(Exception e)
		{
			
		}
	}

	/**
	 * Get the password
	 * 
	 * @param project		The MoSync project
	 * @param propertyKey	The property key
	 * @return 				The password
	 */
	private String getPassword(MoSyncProject project, String propertyKey) {
        return UIUtils.getPassword(project, propertyKey);
    }

	/**
	 * Converts all the . in the package name with a / since this is what is excpected by the Android build tools
	 * 
	 * @param name	The package name with dots
	 * @return		The package name with slashes
	 */
    private String toByteCodePackageName(String name) {
        return name.replace('.', '/');
    }

    /**
     * Recursive delete of all the files and directories, starting from the given directory
     * 
     * @param p		The directory which the delete will start from
     */
    private void recursiveDel ( File p )
	{
		if ( p.isFile( ) == false )
		{
			for ( File f : p.listFiles( ) )
				recursiveDel( f );
		}
		
		p.delete( );
	}

    /**
     * Generates the Android manifest file
     * 
     * @param project			The project object
     * @param version			The application version
     * @param packageName		The package which the source code belongs to
     * @param manifest			The file handle to which the manifest will be written to
     * @throws IOException
     */
    private void createManifest(
    		MoSyncProject project, 
    		Version version, 
    		String packageName, 
    		File manifest) throws IOException {
		manifest.getParentFile().mkdirs();
		String manifest_string = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
		+"<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
			+"\tpackage=\"" + packageName + "\"\n"
			+"\tandroid:versionCode=\"" + project.getProperty(PropertyInitializer.ANDROID_VERSION_CODE) + "\"\n"
			+"\tandroid:versionName=\"" + version.toString() + "\">\n"
			+"\t<application android:icon=\"@drawable/icon\" android:label=\"@string/app_name\">\n"
				+"\t\t<activity android:name=\".MoSync\"\n"
					// Use portrait orientation as default. 
					+"\t\t\tandroid:screenOrientation=\"portrait\"\n"
					+"\t\t\tandroid:configChanges=\"keyboardHidden|orientation\"\n"
					+"\t\t\tandroid:launchMode=\"singleTask\"\n"
					+"\t\t\tandroid:label=\"@string/app_name\" >\n"
					+"\t\t\t<intent-filter>\n"
						+"\t\t\t\t<action android:name=\"android.intent.action.MAIN\" />\n"
						+"\t\t\t\t<category android:name=\"android.intent.category.LAUNCHER\" />\n"
					+"\t\t\t</intent-filter>\n"
				+"\t\t</activity>\n"
				+"\t\t<activity android:name=\".MoSyncPanicDialog\"\n"
					+"android:label=\"@string/app_name\">\n"
				+"\t\t</activity>\n"
				+"\t\t<activity android:name=\".TextBox\"\n"
	                  +"\t\t\tandroid:label=\"@string/app_name\">\n"
	        	+"\t\t</activity>\n"
//				+"\t\t<activity android:name=\".WebViewActivity\"\n"
//                	+"\t\t\tandroid:label=\"@string/app_name\">\n"
//				+"\t\t</activity>\n"
				+"\t\t<service android:name=\".MoSyncService\"/>\n"
				+createAutoStartXML(project)
			+"\t</application>\n";
			
		// Attribute targetSdkVersion is supported only 
		// on Android API level 4 and above.
		if (m_AndroidVersion >= 4) 
		{
			manifest_string += 
				"\t<uses-sdk android:minSdkVersion=\"3\" "
				+ "android:targetSdkVersion=\"7\" />\n";
		}
		else
		{
			manifest_string += 
				"\t<uses-sdk android:minSdkVersion=\"3\" />\n";
		}
		
		// The support-screens tag is supported only 
		// on Android API level 4 and above
		if (m_AndroidVersion >= 4) 
		{
			manifest_string += 
				"\t<supports-screens"
				+ "\t\tandroid:largeScreens=\"true\""
				+ "\t\tandroid:normalScreens=\"true\""
				+ "\t\tandroid:smallScreens=\"true\""
				+ "\t\tandroid:anyDensity=\"true\" />";
		}

		manifest_string += createPermissionXML(project);
		manifest_string += "</manifest>\n";
		DefaultPackager.writeFile(manifest, manifest_string);
	}
    
    /**
     * Generates the permissions needed by Android for the different MoSync permissions
     * 
     * @param project	The project file
     * @return			A string containing all the persmissions
     */
    private String createPermissionXML(MoSyncProject project) {
	
        StringBuffer result = new StringBuffer();
		
        IApplicationPermissions permissions = project.getPermissions();
		
		// Add permissions depending on project settings.
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.VIBRATE), "android.permission.VIBRATE");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.INTERNET), "android.permission.INTERNET");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.INTERNET), "android.permission.ACCESS_NETWORK_STATE");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.LOCATION_COARSE), "android.permission.ACCESS_COARSE_LOCATION");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.LOCATION_FINE), "android.permission.ACCESS_FINE_LOCATION");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.POWER_MANAGEMENT), "android.permission.BATTERY_STATS");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.CALENDAR_READ), "android.permission.READ_CALENDAR");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.CALENDAR_WRITE), "android.permission.WRITE_CALENDAR");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.CONTACTS_READ), "android.permission.READ_CONTACTS");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.CONTACTS_WRITE), "android.permission.WRITE_CONTACTS");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.SMS_READ), "android.permission.READ_SMS");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.SMS_SEND), "android.permission.SEND_SMS");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.SMS_RECEIVE), "android.permission.RECEIVE_SMS");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.CAMERA), "android.permission.CAMERA");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.HOMESCREEN), "android.permission.GET_TASKS");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.HOMESCREEN), "android.permission.SET_WALLPAPER");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.HOMESCREEN), "android.permission.SET_WALLPAPER_HINTS");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.HOMESCREEN), "com.android.launcher.permission.INSTALL_SHORTCUT");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.HOMESCREEN), "com.android.launcher.permission.UNINSTALL_SHORTCUT");
        addPermission(result, permissions.isPermissionRequested(ICommonPermissions.AUTOSTART), "android.permission.RECEIVE_BOOT_COMPLETED");

		// Only add this for android 1.6 and higher.
        if (m_AndroidVersion >= 4) 
        {
			addPermission(result, 
				permissions.isPermissionRequested(ICommonPermissions.FILE_STORAGE_WRITE), 
				"android.permission.WRITE_EXTERNAL_STORAGE");
        }
		
		// Only add this for android 2.0 and higher.
        if (m_AndroidVersion >= 7) 
        {
        	addPermission(result, 
				permissions.isPermissionRequested(ICommonPermissions.BLUETOOTH), 
				"android.permission.BLUETOOTH");
        	addPermission(result, 
				permissions.isPermissionRequested(ICommonPermissions.BLUETOOTH), 
				"android.permission.BLUETOOTH_ADMIN");
        }
        
        // Required to be able to log in debug runtime
        if(shouldUseDebugRuntimes() && !permissions.isPermissionRequested(ICommonPermissions.FILE_STORAGE_WRITE))
        {
        	addPermission(result, true, "android.permission.WRITE_EXTERNAL_STORAGE");
        }

        // Always add this.
        addPermission(result, true, "android.permission.READ_PHONE_STATE");
		
        return result.toString();
    }
    
    /**
     * Create XML definition for a BroadcastReceiver that is used to autostart
     * the application.
     * 
     * @param project	The project file object.
     * @return			A string with the BroadcastReceiver XML definition.
     */
    private String createAutoStartXML(MoSyncProject project) 
    {
        // Check if the auto start permission is set, if not, return an empty string.
        IApplicationPermissions permissions = project.getPermissions();
        if (!permissions.isPermissionRequested(ICommonPermissions.AUTOSTART))
        {
        	return "";
        }
        
        // Create the auto start definition.
		String autoStartXML = 
			"\t\t<receiver android:enabled=\"true\"\n" +
			"\t\t\tandroid:name=\".MoSyncAutoStart\"\n" +
			"\t\t\tandroid:permission=\"android.permission.RECEIVE_BOOT_COMPLETED\">\n" +
			"\t\t\t<intent-filter>\n" +
			"\t\t\t\t<action android:name=\"android.intent.action.BOOT_COMPLETED\" />\n" +
			"\t\t\t\t<category android:name=\"android.intent.category.DEFAULT\" />\n" +
			"\t\t\t</intent-filter>\n" +
			"\t\t</receiver>\n";
		
		return autoStartXML;
    }
    
    private void addPermission(StringBuffer result, boolean condition, String... androidPermissions) {
        if (condition) {
            for (int i = 0; i < androidPermissions.length; i++) {
                addAndroidPermission(result, androidPermissions[i]);
            }
        }
    }
    
    private void addAndroidPermission(StringBuffer result, String androidPermission) {
            result.append("<uses-permission android:name=\"" + androidPermission + "\" />\n");
    }

    /**
     * Generates the res/layout/main.xml file
     * 
     * @param main_xml		The file handle to the file which the file is written to
     * @throws IOException
     */
    private void createMain(File main_xml) throws IOException {
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
	
    /**
     *  Generates the res/values/strings.xml file
     * 
     * @param applicationName	The name of the application, which will be shown in Android
     * @param strings_xml		The file handle to the file which it's being written to
     * @throws IOException
     */
	private void createStrings(String applicationName, File strings_xml) throws IOException {
		strings_xml.getParentFile().mkdirs();

		String strings_xml_string = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
		+"<resources>\n"
			+"\t<string name=\"app_name\">" + applicationName + "</string>\n"
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
