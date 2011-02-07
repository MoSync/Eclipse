package com.mobilesorcery.sdk.core;

import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

/**
 * <p>A simple security solution for projects that needs access
 * to vital system resources.</p>
 * <p>A special token is created for the project as well as the workspace,
 * and only if they match will privileged access be granted.</p>
 * <p>An obvious attack would be that B requests a project from W with this
 * special token and then resends an updated project with malicious code to W. However,
 * this is not meant to cover every security aspect but just to make sure
 * that there is <i>some</i> protection for the user from inadvertently downloading
 * a project from somewhere off the Internet that contains evil, nasty scripts.
 * We conjecture that most people wouldn't expect that kind of code to run just because
 * we imported an Eclipse project!</p>
 * @author Mattias Bybro
 *
 */
public class PrivilegedAccess {

	private static final String PRIVILEGED_ACCESS_TOKEN = "privileged.access.token";

	private static final PrivilegedAccess INSTANCE = new PrivilegedAccess();

	private boolean blanketAccess;
	
	private SecureRandom rnd;
	
	private PrivilegedAccess() {
		blanketAccess = CoreMoSyncPlugin.isHeadless() && Arrays.asList(Platform.getApplicationArgs()).contains("-allow-scripts:true");
		rnd = new SecureRandom();
	}

	public static PrivilegedAccess getInstance() {
		return INSTANCE;
	}
	
	public void assertAccess(MoSyncProject project) throws CoreException {
		if (!hasAccess(project)) {
	 		if (CoreMoSyncPlugin.isHeadless()) {
	 			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, 
	 					MessageFormat.format("Script execution not allowed for project {0}. For headless builds, set the command line parameter \"-allow-scripts:true\"", project.getName())));
	 		} else {
	 			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, 
	 					MessageFormat.format("Script execution not allowed for project {0}. Change this in Window > Preferences > MoSync Tool > Scripts", project.getName())));	 			
	 		}
		}
	}

	
	public boolean hasAccess(MoSyncProject project) {
		if (blanketAccess) {
			return true;
		}
		
		String prefName = computePrefName(project);
		String token = CoreMoSyncPlugin.getDefault().getPreferenceStore().getString(prefName);
		String projectToken = project.getProperty(PRIVILEGED_ACCESS_TOKEN);
		boolean result = token != null && token.equals(projectToken);
		if (!result) {
			// If we have different tokens just clear them
			ungrant(project);
		}
		
		return result;
	}
	
	public void grantAccess(MoSyncProject project, boolean grant) {
		if (hasAccess(project) ^ grant) {
			if (grant) {
				grant(project);
			} else {
				ungrant(project);
			}
		}
	}

	private void ungrant(MoSyncProject project) {
		String prefName = computePrefName(project);
		project.initProperty(PRIVILEGED_ACCESS_TOKEN, null, MoSyncProject.LOCAL_PROPERTY, true);
		CoreMoSyncPlugin.getDefault().getPreferenceStore().setToDefault(prefName);
	}

	private void grant(MoSyncProject project) {
		byte[] token = new byte[16];
		rnd.nextBytes(token);
		String tokenStr = Util.toBase16(token);
		project.initProperty(PRIVILEGED_ACCESS_TOKEN, tokenStr, MoSyncProject.LOCAL_PROPERTY, true);
		String prefName = computePrefName(project);
		CoreMoSyncPlugin.getDefault().getPreferenceStore().setValue(prefName, tokenStr);
	}
	

	private String computePrefName(MoSyncProject project) {
		return "privileged.access" + ":" + project.getName();
	}

}
