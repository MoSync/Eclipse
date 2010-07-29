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
package com.mobilesorcery.sdk.builder.java;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.mobilesorcery.sdk.core.AbstractPackager;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;
import com.mobilesorcery.sdk.internal.builder.MoSyncIconBuilderVisitor;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.UIUtils;

public class JavaPackager extends AbstractPackager {
    private String m_zipLoc;
    private String m_iconInjectorLoc;

    public JavaPackager() {
        MoSyncTool tool = MoSyncTool.getDefault();
        m_zipLoc = tool.getBinary("zip").toOSString();
        m_iconInjectorLoc = tool.getBinary("icon-injector").toOSString();

    }

    public void createPackage(MoSyncProject project, IBuildVariant variant, IBuildResult buildResult) throws CoreException {
        DefaultPackager internal = new DefaultPackager(project, variant);
        IProfile targetProfile = variant.getProfile();
        File runtimeDir = new File(internal.resolve("%runtime-dir%"));
        File compileOut = new File(internal.resolve("%compile-output-dir%"));

        internal.setParameters(getParameters());
        internal.setParameter("D", shouldUseDebugRuntimes() ? "D" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        try {
            File packageOutputDir = internal.resolveFile("%package-output-dir%"); //$NON-NLS-1$
            packageOutputDir.mkdirs();

            Version appVersion = new Version(internal.getParameters().get(DefaultPackager.APP_VERSION));

            File projectJar = new File(internal.resolve("%package-output-dir%/%app-name%.jar")); //$NON-NLS-1$
            File projectJad = new File(internal.resolve("%package-output-dir%/%app-name%.jad")); //$NON-NLS-1$

            projectJar.delete();
            projectJad.delete();

            String appVendorName = internal.getParameters().get(DefaultPackager.APP_VENDOR_NAME);
            File manifest = new File(internal.resolve("%compile-output-dir%/META-INF/manifest.mf")); //$NON-NLS-1$
            createManifest(variant, project.getName(), appVendorName, appVersion, manifest);

            // Need to set execution dir, o/w zip will not understand what we
            // really want.
            internal.getExecutor().setExecutionDirectory(manifest.getParentFile().getParent());

            {
                String runtime = internal.resolve("MoSyncRuntime%D%.jar");
                Util.copyFile(new NullProgressMonitor(), new File(runtimeDir, runtime), projectJar);
            }
            Util.copyFile(new NullProgressMonitor(), new File(runtimeDir, "config.h"), new File(packageOutputDir, "config.h"));

            internal.runCommandLine(m_zipLoc, "-j", "-9", projectJar.getAbsolutePath(), new File(compileOut, "program").getAbsolutePath());
            internal.runCommandLine(m_zipLoc, "-r", "-9", projectJar.getAbsolutePath(), "META-INF");

            File resources = new File(compileOut, "resources"); //$NON-NLS-1$
            if (resources.exists()) {
                internal.runCommandLine(m_zipLoc, "-j", "-9", projectJar.getAbsolutePath(), resources.getAbsolutePath());
            }

            createJAD(variant, project.getWrappedProject().getName(), appVendorName, appVersion, projectJad, projectJar);

            MoSyncIconBuilderVisitor visitor = new MoSyncIconBuilderVisitor();
            visitor.setProject(project.getWrappedProject());
            IResource[] iconFiles = visitor.getIconFiles();
            if (iconFiles.length > 0) {
                Object xObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_X"); //$NON-NLS-1$
                Object yObj = targetProfile.getProperties().get("MA_PROF_CONST_ICONSIZE_Y"); //$NON-NLS-1$
                if (xObj != null && yObj != null) {
                    String sizeStr = ((Long) xObj) + "x" + ((Long) yObj); //$NON-NLS-1$
                    internal.runCommandLine(m_iconInjectorLoc, "-src", iconFiles[0].getLocation().toOSString(), "-size", sizeStr, "-platform", "j2me", "-dst",
                            projectJar.getAbsolutePath());
                }
            }

            signPackage(internal, project, projectJad, projectJar);

            buildResult.setBuildResult(projectJar);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, "com.mobilesorcery.sdk.builder.java", Messages.JavaPackager_PackageError, e)); //$NON-NLS-1$
        }
    }

    private void signPackage(DefaultPackager internal, MoSyncProject project, File projectJad, File projectJar) throws IOException {
        List<KeystoreCertificateInfo> keystoreCertInfos = KeystoreCertificateInfo
                .parseList(project.getProperty(PropertyInitializer.JAVAME_KEYSTORE_CERT_INFOS));
        // sign jar file using jarSigner
        File mosyncBinDir = internal.resolveFile("%mosync-bin%");
        // File unsignedProjectJar = new File(projectJar.getParentFile(),
        // Util.getNameWithoutExtension(projectJar) + "_unsigned.jar");

        for (KeystoreCertificateInfo keystoreCertInfo : keystoreCertInfos) {
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

            // Util.copyFile(new NullProgressMonitor(), projectJar,
            // unsignedProjectJar);

            /*
             * String[] jarSignerCommandLine = new String[] { "java", "-jar",
             * new File( mosyncBinDir, "android/tools-stripped.jar"
             * ).getAbsolutePath( ), "-keystore", keystore, "-storepass",
             * storepass, "-keypass", keypass, "-signedjar",
             * projectJar.getAbsolutePath( ),
             * unsignedProjectJar.getAbsolutePath( ), alias };
             * 
             * internal.runCommandLine(jarSignerCommandLine,
             * "*** COMMAND LINE WITHHELD, CONTAINS PASSWORDS ***");
             */
            String[] addCertCommandLine = new String[] {
                    "java",
                    "-classpath",
                    new File(mosyncBinDir, "javame/JadTool.jar").getAbsolutePath() + File.pathSeparatorChar
                            + new File(mosyncBinDir, "javame/core__environment.jar").getAbsolutePath() + File.pathSeparatorChar
                            + new File(mosyncBinDir, "javame/core__properties.jar").getAbsolutePath(), "com.sun.midp.jadtool.JadTool", "-addcert", "-keystore",
                    keystore, "-alias", alias, "-inputjad", projectJad.getAbsolutePath(), "-outputjad",
                    projectJad.getAbsolutePath() };

            assertOk(internal.runCommandLine(addCertCommandLine, "*** COMMAND LINE WITHHELD, CONTAINS PASSWORDS ***"));

            String[] addJarSigCommandLine = new String[] {
                    "java",
                    "-classpath",
                    new File(mosyncBinDir, "javame/JadTool.jar").getAbsolutePath() + File.pathSeparatorChar
                            + new File(mosyncBinDir, "javame/core__environment.jar").getAbsolutePath() + File.pathSeparatorChar
                            + new File(mosyncBinDir, "javame/core__properties.jar").getAbsolutePath(), "com.sun.midp.jadtool.JadTool", "-addjarsig",
                    "-keystore", keystore, "-keypass", keypass, "-alias", alias, "-jarfile", projectJar.getAbsolutePath(), "-inputjad",
                    projectJad.getAbsolutePath(), "-outputjad", projectJad.getAbsolutePath() };

            assertOk(internal.runCommandLine(addJarSigCommandLine, "*** COMMAND LINE WITHHELD, CONTAINS PASSWORDS ***"));
        }
    }
    
    private void assertOk(int errorCode) {
        if (errorCode != 0) {
            throw new IllegalArgumentException("Tool execution failed");
        }
    }

    private void createManifest(IBuildVariant variant, String projectName, String vendorName, Version version, File manifest) throws IOException {
        manifest.getParentFile().mkdirs();

        DefaultPackager.writeFile(manifest, getManifest(variant, projectName, vendorName, version));
    }

    private void createJAD(IBuildVariant variant, String projectName, String vendorName, Version version, File jad, File jar) throws IOException {
        long jarSize = jar.length();
        String jarName = jar.getName();
        String jadString = getManifest(variant, projectName, vendorName, version)
                + "MIDlet-Jar-Size: " + Long.toString(jarSize) + "\n" + "MIDlet-Jar-URL: " + jarName + "\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        DefaultPackager.writeFile(jad, jadString);
    }

    private String getManifest(IBuildVariant variant, String projectName, String vendorName, Version version) {
        IProfile profile = variant.getProfile();
        boolean isCLDC_10 = profile != null && Boolean.TRUE.equals(profile.getProperties().get("MA_PROF_SUPPORT_CLDC_10"));
        return "Manifest-Version: 1.0\n" + "MIDlet-Vendor: " + vendorName + "\n" + "MIDlet-Name: " + projectName + "\n" + "MIDlet-1: " + projectName + ", " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                + projectName
                + ".png, MAMidlet\n" + "MIDlet-Version: " + version.asCanonicalString(Version.MICRO) + "\n" + "MicroEdition-Configuration: " + (isCLDC_10 ? "CLDC-1.0" : "CLDC-1.1") + "\nMicroEdition-Profile: MIDP-2.0\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

}
