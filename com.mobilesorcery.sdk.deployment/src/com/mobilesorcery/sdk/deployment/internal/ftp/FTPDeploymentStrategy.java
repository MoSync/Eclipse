package com.mobilesorcery.sdk.deployment.internal.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.mobilesorcery.sdk.core.CascadingProperties;
import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildSession;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.deployment.IDeploymentStrategy;
import com.mobilesorcery.sdk.profiles.IProfile;

public class FTPDeploymentStrategy implements IDeploymentStrategy {

	private final class TraceListener implements ProtocolCommandListener {

		private boolean active = true;

		public void protocolReplyReceived(ProtocolCommandEvent event) {
			if (!active) {
				return;
			}
			System.err.println("<- " + event.getMessage().trim());
		}

		public void protocolCommandSent(ProtocolCommandEvent event) {
			sent(event.getMessage().trim());
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		public void sent(String msg) {
			if (!active) {
				return;
			}

			System.err.println("->" + msg);
		}
	}

	public static final String FACTORY_ID = "FTP";

	private String host;
	private String password;
	private String username;

	private String remotePath;

	public FTPDeploymentStrategy() {
		remotePath = "%project-name%/%vendor%/%profile%/%package%";
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void deploy(MoSyncProject project, List<IProfile> profiles,
			IProgressMonitor monitor) throws Exception {
		monitor.beginTask("", profiles.size());
		int i = 0;
		FTPClient ftp = connect();
		try {
			for (IProfile profile : profiles) {
				i++;

				if (monitor.isCanceled()) {
					return;
				}

				monitor.setTaskName(MessageFormat.format(
						"Deploying {0} of {1} packages", i, profiles.size()));
				IBuildResult result = buildBeforeDeploy(project, profile, new NullProgressMonitor());
				monitor.subTask("Uploading");
				deploy(ftp, project, profile, result, new SubProgressMonitor(
						monitor, 1));
			}
		} finally {
			disconnect(ftp);
		}
	}

	private void disconnect(FTPClient ftp) throws IOException {
		if (ftp.isConnected()) {
			ftp.logout();
		}
		ftp.disconnect();
	}

	private FTPClient connect() throws IOException {
		FTPClient ftp = new FTPClient();
		ftp.connect(host);
		int reply = ftp.getReplyCode();
		TraceListener listener = new TraceListener();
		ftp.addProtocolCommandListener(listener);

		if (!FTPReply.isPositiveCompletion(reply)) {
			throw new IOException("FTP server refused connection.");
		}

		listener.setActive(false);
		listener.sent("*** LOGIN TRACE WITHHELD ***");
		if (!ftp.login(username, password)) {
			throw new IOException(MessageFormat.format(
					"Could not login to ftp server {0}", host));
		}
		listener.setActive(true);

		ftp.enterLocalPassiveMode();
		assertOk(ftp.setFileType(FTP.BINARY_FILE_TYPE),
				"Could not set file type to binary on FTP server");

		return ftp;
	}

	private IBuildResult buildBeforeDeploy(MoSyncProject project,
			IProfile profile, IProgressMonitor monitor) throws CoreException {
	    IBuildVariant variant = MoSyncBuilder.getFinalizerVariant(project, profile);
		IBuildSession session = MoSyncBuilder.createDefaultBuildSession(variant);
		
        IBuildResult buildResult = new MoSyncBuilder().build(project
				.getWrappedProject(), session, variant, null, monitor);
		return buildResult;
	}

	public void deploy(FTPClient ftp, MoSyncProject project, IProfile profile,
			IBuildResult result, IProgressMonitor monitor) throws Exception {
		InputStream localInputStream = null;
		try {
			IPath remotePath = getRemoteLocation(project, profile, result);
			localInputStream = createInputStream(project, profile, result);
			IPath changeDir = remotePath.removeLastSegments(1);
			ftp.changeWorkingDirectory("/");
			makeOrChangeDir(ftp, changeDir);
			monitor.beginTask("Uploading", 1);
			assertOk(ftp.storeFile(remotePath.lastSegment(), localInputStream),
					"Could not store file");
		} catch (IOException e) {
			throw new Exception("Could not deploy: " + e.getMessage(), e);
		} finally {
			Util.safeClose(localInputStream);
			monitor.done();
		}
	}

	private void makeOrChangeDir(FTPClient ftp, IPath path) throws IOException {
		String[] segments = path.segments();
		for (int i = 0; i < segments.length; i++) {
			if (!ftp.changeWorkingDirectory(segments[i])) {
				assertOk(ftp.makeDirectory(segments[i]), MessageFormat.format(
						"Could not create directory {0}", path.toString()));
				assertOk(ftp.changeWorkingDirectory(segments[i]), "Could not change directory");
			}
		}
	}

	private void assertOk(boolean cond, String errormsg) throws IOException {
		if (!cond) {
			throw new IOException(errormsg);
		}
	}

	public IPath getRemoteLocation(MoSyncProject project, IProfile profile,
			IBuildResult result) throws IOException {
		String vendorName = profile.getVendor().getName();
		String profileName = profile.getName();
		String projectName = project.getName();
		String packageName = getLocalFile(result).getName();
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(DefaultPackager.PROJECT_NAME, projectName);
		properties.put(DefaultPackager.VENDOR_NAME, vendorName);
		properties.put(DefaultPackager.PROFILE_NAME, profileName);
		properties.put("package", packageName);
		String remotePath = CommandLineExecutor.replace(getRemotePath(),
				new CascadingProperties(new Map[] { properties }));
		return new Path(remotePath);
	}

	public String getRemotePath() {
		return remotePath;
	}

	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	private File getLocalFile(IBuildResult result) throws IOException {
		File file = result.getBuildResult();
		if (file == null || !file.exists()) {
			throw new IOException("No package built/found");
		}

		return file;
	}

	private InputStream createInputStream(MoSyncProject project,
			IProfile profile, IBuildResult result) throws IOException {
		return new FileInputStream(getLocalFile(result));
	}

	public String getFactoryId() {
		return FACTORY_ID;
	}

	public String getPassword() {
		return password;
	}
}
