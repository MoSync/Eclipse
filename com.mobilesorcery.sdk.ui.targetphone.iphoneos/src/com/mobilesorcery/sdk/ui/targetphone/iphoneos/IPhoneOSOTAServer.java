package com.mobilesorcery.sdk.ui.targetphone.iphoneos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpRetryException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
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
		String projectName = Util.getNameWithoutExtension(target);
		
		if (Util.isEmpty(projectName)) {
			generateIndex(response);
		} else {
			IProject project = ResourcesPlugin.getPlugin().getWorkspace().getRoot().getProject(projectName);
			MoSyncProject mosyncProject = MoSyncProject.create(project);
			if (mosyncProject != null) {
				IBuildVariant variant = projects.get(mosyncProject);
				if (variant != null) {
					if ("plist".equals(ext)) {
						generatePlist(response, mosyncProject);
					}
					if ("mobileprovisioning".equals(ext)) {
						generateProvisioningFile(response, mosyncProject);
					}
					if ("ipa".equals(ext)) {
						transferApp(response, mosyncProject, variant);
					}
				}
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		}
		
		response.flushBuffer();
		Util.safeClose(response.getOutputStream());
	}

	private void transferApp(HttpServletResponse response,
			MoSyncProject mosyncProject, IBuildVariant variant) throws IOException {
		for (IPhoneOSOTAServerListener listener : listeners) {
			listener.appRequested(mosyncProject);
		}
		
		response.setContentType("application/octet-stream");
		
		IBuildState buildState = mosyncProject.getBuildState(variant);
		List<File> ipaFile = buildState.getBuildResult().getBuildResult().get(IBuildResult.MAIN);
		if (!ipaFile.isEmpty()) {
			FileInputStream input = new FileInputStream(ipaFile.get(0));
			Util.transfer(input, response.getOutputStream());
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private void generateProvisioningFile(HttpServletResponse response,
			MoSyncProject project) throws IOException {
		response.setContentType("application/octet-stream");
		
		String provisioningFile = project.getProperty(PropertyInitializer.IOS_PROVISIONING_FILE);
		File absoluteProvisioningFile = Util.relativeTo(project.getWrappedProject().getLocation().toFile(), provisioningFile);
		FileInputStream input = new FileInputStream(absoluteProvisioningFile);
		try {
			Util.transfer(input, response.getOutputStream());
		} finally {
			Util.safeClose(input);
		}
	}

	private void generatePlist(HttpServletResponse response, MoSyncProject mosyncProject) throws IOException {
		response.setContentType("text/xml");
		
		Template plistTemplate = new Template(getClass().getResource("/templates/dist.plist.template"));
		Map<String, String> map = new HashMap<String, String>();
		map.put("base-url", getBaseURL());
		map.put("project-name", mosyncProject.getName());
		map.put(DefaultPackager.APP_VENDOR_NAME_BUILD_PROP, mosyncProject.getProperty(DefaultPackager.APP_VENDOR_NAME_BUILD_PROP));
		map.put("iphoneos:bundle.id", mosyncProject.getProperty("iphoneos:bundle.id"));
		response.getWriter().write(plistTemplate.resolve(map));
	}

	private void generateIndex(HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		PrintWriter output = response.getWriter();
		Template indexTemplate = new Template(getClass().getResource(
				"/templates/index.html.template"));
		StringBuffer projectList = new StringBuffer();
		for (MoSyncProject project : projects.keySet()) {
			String projectName = project.getName();
			projectList.append("<b>");
			projectList.append(project.getName());
			projectList.append("</b><br/>");
			projectList.append("<ul>");
			String url = getBaseURL();
			projectList.append(MessageFormat.format(
					"<li><a href=\"itms-services://?action=download-manifest&url={0}/{1}.plist\">App</a></li>", url, projectName));
			projectList
					.append(MessageFormat
							.format("<li><a href=\"{0}/{1}.mobileprovisioning\">Provisioning file</a></li>",
									url, projectName));
			projectList.append("</ul>");
			projectList.append("<hr/>");
		}
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("project-list", projectList.toString());
		output.write(indexTemplate.resolve(map));
	}

	private String getBaseURL() throws IOException {
		InetAddress localHost = InetAddress.getLocalHost();
		String host = localHost.getHostAddress();
		return new URL("http", host, getPort(), "").toExternalForm();
	}

}
