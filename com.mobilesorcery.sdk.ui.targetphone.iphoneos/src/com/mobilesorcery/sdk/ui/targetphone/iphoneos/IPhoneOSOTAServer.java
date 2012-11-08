package com.mobilesorcery.sdk.ui.targetphone.iphoneos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.mobilesorcery.sdk.builder.iphoneos.PropertyInitializer;
import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.IBuildResult;
import com.mobilesorcery.sdk.core.IBuildState;
import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.templates.Template;

public class IPhoneOSOTAServer extends AbstractHandler {

	private static IPhoneOSOTAServer defaultServer;

	private Server server;
	private final IdentityHashMap<MoSyncProject, IBuildVariant> projects = new IdentityHashMap<MoSyncProject, IBuildVariant>();

	private CopyOnWriteArrayList<IPhoneOSOTAServerListener> listeners = new CopyOnWriteArrayList<IPhoneOSOTAServerListener>();

	public static IPhoneOSOTAServer getDefault() {
		if (defaultServer == null) {
			defaultServer = new IPhoneOSOTAServer();
		}
		return defaultServer;
	}

	public void offerProject(MoSyncProject project, IBuildVariant variant) throws IOException {
		try {
			startServer(project, variant);
		} catch (Exception e) {
			throw new IOException("Could not start server", e);
		}
	}
	
	public void addListener(IPhoneOSOTAServerListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(IPhoneOSOTAServerListener listener) {
		listeners.remove(listener);
	}

	private synchronized void startServer(MoSyncProject project, IBuildVariant variant)
			throws Exception {
		if (projects.isEmpty()) {
			server = new Server(getPort());
			server.setThreadPool(new QueuedThreadPool(5));
			server.setHandler(this);
			Connector connector = new SelectChannelConnector();
			connector.setPort(getPort());
			connector.setMaxIdleTime(120000);
			server.setConnectors(new Connector[] { connector });
			server.start();
		}
		IBuildVariant prevVariant = projects.put(project, variant);
		if (prevVariant != null && CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Warning: replaced variant for iOS OTA. {0}, {1}", project, variant);
		}
	}

	private synchronized void stopServer(MoSyncProject project)
			throws Exception {
		projects.remove(project);
		if (projects.isEmpty()) {
			server.stop();
		}
	}

	private int getPort() throws IOException {
		return IPhoneOSTransportPlugin.getDefault().getServerURL().getPort();
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		if (target.startsWith("/")) {
			target = target.substring(1);
		}
		
		String ext = Util.getExtension(target);
		String[] segments = target.split("/");
		String projectName = Util.getNameWithoutExtension(segments[0]);
		String fileName = segments[segments.length - 1];
		
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Device requested {0} for project {1} (extension {2})", target, projectName, ext);
		}
		
		if (Util.isEmpty(projectName)) {
			generateIndex(response);
		} else {
			IProject project = ResourcesPlugin.getPlugin().getWorkspace().getRoot().getProject(projectName);
			MoSyncProject mosyncProject = MoSyncProject.create(project);
			if (mosyncProject != null) {
				IBuildVariant variant = projects.get(mosyncProject);
				if (variant != null) {
					if ("plist".equals(ext)) {
						generatePlist(response, mosyncProject, variant);
					} else if ("mobileprovisioning".equals(ext)) {
						generateProvisioningFile(response, mosyncProject);
					} else if ("ipa".equals(ext)) {
						transferApp(response, mosyncProject, variant);
					} else if (ext.equals("png") && fileName.startsWith("icon")) {
						transferIcon(response, mosyncProject, variant);
					} else if (ext.equals("png") && fileName.startsWith("image")) {
						transferImage(response, mosyncProject, variant);
					}
				}
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		}
	}

	private void transferImage(HttpServletResponse response,
			MoSyncProject mosyncProject, IBuildVariant variant) throws IOException {
        URL defaultImageFileURL = FileLocator.find(IPhoneOSTransportPlugin.getDefault().getBundle(), new Path("/images/default-itunes-image512x512.png"), null);
        URL defaultImageFile = FileLocator.toFileURL(defaultImageFileURL);
		if (defaultImageFile != null) {
			transferFile(response, new File(defaultImageFile.getPath()));
		}
	}

	private void transferApp(HttpServletResponse response,
			MoSyncProject mosyncProject, IBuildVariant variant) throws IOException {
		for (IPhoneOSOTAServerListener listener : listeners) {
			listener.appRequested(mosyncProject);
		}
		
		IBuildState buildState = mosyncProject.getBuildState(variant);
		List<File> ipaFile = buildState.getBuildResult().getBuildResult().get(IBuildResult.MAIN);

		transferFile(response, ipaFile.isEmpty() ? null : ipaFile.get(0));
	}
	
	private void transferIcon(HttpServletResponse response,
			MoSyncProject mosyncProject, IBuildVariant variant) throws IOException {
		IPath output = MoSyncBuilder.getPackageOutputPath(mosyncProject.getWrappedProject(), variant);
		// We make use of internal knowledge!
		IPath iconFile = output.append(new Path("xcode-proj/Icon.png"));
		transferFile(response, iconFile.toFile());
	}
	
	private void transferFile(HttpServletResponse response, File file) throws IOException {
		response.setContentType("application/octet-stream");
		response.setStatus(HttpServletResponse.SC_OK);
		
		if (file != null && file.exists()) {
			FileInputStream input = new FileInputStream(file);
			Util.transfer(input, response.getOutputStream());
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		Util.safeClose(response.getOutputStream());
	}

	private void generateProvisioningFile(HttpServletResponse response,
			MoSyncProject project) throws IOException {
		response.setContentType("application/octet-stream");
		response.setStatus(HttpServletResponse.SC_OK);
		
		String provisioningFile = project.getProperty(PropertyInitializer.IOS_PROVISIONING_FILE);
		File absoluteProvisioningFile = Util.relativeTo(project.getWrappedProject().getLocation().toFile(), provisioningFile);
		FileInputStream input = new FileInputStream(absoluteProvisioningFile);
		try {
			Util.transfer(input, response.getOutputStream());
		} finally {
			Util.safeClose(input);
			Util.safeClose(response.getOutputStream());
		}
	}

	private void generatePlist(HttpServletResponse response, MoSyncProject mosyncProject, IBuildVariant variant) throws IOException {
		response.setContentType("text/xml");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);
		
		Template plistTemplate = new Template(getClass().getResource("/templates/dist.plist.template"));
		Map<String, String> map = new HashMap<String, String>();
		DefaultPackager dp = new DefaultPackager(mosyncProject, variant);
		map.putAll(dp.getParameters().toMap());
		map.put("base-url", getBaseURL());
		map.put("project-name", mosyncProject.getName());
		map.put(DefaultPackager.APP_VENDOR_NAME_BUILD_PROP, mosyncProject.getProperty(DefaultPackager.APP_VENDOR_NAME_BUILD_PROP));
		map.put("iphoneos:bundle.id", mosyncProject.getProperty("iphone:bundle.id"));
		response.getWriter().write(plistTemplate.resolve(map));
		
		Util.safeClose(response.getWriter());
	}

	private void generateIndex(HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);
		PrintWriter output = response.getWriter();
		Template indexTemplate = new Template(getClass().getResource(
				"/templates/index.html.template"));
		StringBuffer projectList = new StringBuffer();
		int projectCount = 0;
		
		TreeMap<Long, MoSyncProject> sortedByBuildTime = new TreeMap<Long, MoSyncProject>();
		for (MoSyncProject project : projects.keySet()) {
			IBuildVariant variant = this.projects.get(project);
			IBuildState buildState = project.getBuildState(variant);
			IBuildResult buildResult = buildState.getBuildResult();
			if (buildResult != null) {
				long buildTimestamp =buildResult.getTimestamp();
				sortedByBuildTime.put(-buildTimestamp, project);
			}
		}
		
		for (MoSyncProject project : sortedByBuildTime.values()) {
			IBuildVariant variant = this.projects.get(project);
			IBuildState buildState = project.getBuildState(variant);
			List<File> ipaFile = buildState.getBuildResult().getBuildResult().get(IBuildResult.MAIN);
			if (!ipaFile.isEmpty() && ipaFile.get(0).exists()) {
				projectCount++;
				String projectName = project.getName();
				String url = URLEncoder.encode(getBaseURL(), "UTF-8");
				
				projectList.append("<div class=\"appitem\">");
				projectList.append(MessageFormat.format("<div class=\"r\"><img src=\"./{0}/icon57x57.png\"></div>", projectName));
				projectList.append("<div class=\"l\">");
				projectList.append(project.getName());
				projectList.append("</div><br/>");
	
				long buildTimestamp = buildState.getBuildResult().getTimestamp();
				boolean fairlyRecent = System.currentTimeMillis() - buildTimestamp < 18 * 3600 * 1000;
				DateFormat format = fairlyRecent ? DateFormat.getTimeInstance(DateFormat.SHORT) : DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
				String buildTime = format.format(new Date(buildTimestamp));
				long fileSize = ipaFile.get(0).length();
				projectList.append(MessageFormat.format(
						"<div class=\"q\"><a href=\"itms-services://?action=download-manifest&url={0}/{1}.plist\">Download App</a>" +
						"<small><br/>Build time: {2}" +
						"<br/>App size: {3}" +
						"<br/>Configuration: {4}</small></div>", url, projectName, buildTime, Util.dataSize(fileSize), variant.getConfigurationId()));
				projectList.append("<br/>");
				projectList.append("</div>");
			}
		}
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("project-list", projectList.toString());
		map.put("project-count", Integer.toString(projectCount));
		output.write(indexTemplate.resolve(map));
		
		Util.safeClose(output);
	}

	private String getBaseURL() throws IOException {
		InetAddress localHost = InetAddress.getLocalHost();
		String host = localHost.getHostAddress();
		return new URL("http", host, getPort(), "").toExternalForm();
	}

}
