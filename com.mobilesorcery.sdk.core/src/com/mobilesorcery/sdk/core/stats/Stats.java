package com.mobilesorcery.sdk.core.stats;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Util;

public class Stats  {

	private static final String STATS_PREF = "stats";

	private static Stats stats = new Stats();

	private Variables variables;

	private long started;

	/**
	 * The constructor
	 */
	private Stats() {
	}

	public static Stats getStats() {
		return stats;
	}

	public void start() {
		started = System.currentTimeMillis();
		initVariables(true);
	}

	private void initVariables(boolean load) {
		variables = new Variables();
		if (load) {
			loadState();
		}
		variables.get(TimeStamp.class, "ide.started").set(started);
		variables.get(TimeStamp.class, "time.elapsed").set();
	}

	public void stop() throws Exception {
		saveState();
	}

	private void saveState() {
		StringWriter output = new StringWriter();
		try {
			variables.write(output);
		} catch (IOException e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
		CoreMoSyncPlugin.getDefault().getPreferenceStore()
				.setValue(STATS_PREF, output.getBuffer().toString());
	}

	private void loadState() {
		String stats = CoreMoSyncPlugin.getDefault().getPreferenceStore().getString(STATS_PREF);
		try {
			if (!Util.isEmpty(stats)) {
				variables.read(new StringReader(stats));
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	public Variables getVariables() {
		return variables;
	}

	private void send() {
		final Variables variablesToSend = variables;
		initVariables(false);
		saveState();
		Thread sendThread = new Thread(new Runnable() {
			@Override
			public void run() {
				URLConnection connection;
				OutputStreamWriter writer = null;
				try {
					connection = getStatsUrl().openConnection();
					connection.setDoOutput(true);
					connection.setConnectTimeout(15000);
					writer = new OutputStreamWriter(
							connection.getOutputStream());
					variablesToSend.write(writer);
					writer.flush();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					Util.safeClose(writer);
				}
			}
		});
		sendThread.setDaemon(true);
		sendThread.start();
	}

	private URL getStatsUrl() {
		try {
			return new URL("http://www.mosync.com/stats");
		} catch (MalformedURLException e) {
			return null;
		}
	}
}
