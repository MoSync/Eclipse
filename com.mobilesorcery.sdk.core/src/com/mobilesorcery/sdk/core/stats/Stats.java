package com.mobilesorcery.sdk.core.stats;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.framework.BundleContext;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

public class Stats {

	private static final String STATS_PREF = "stats";

	private static final String UNSENT_STATS_PREF = "stats.unsent";

	private static final String LAST_SEND_TRY_PREF = "last.send";

	private static final String SEND_INTERVAL_PROP = "send.interval";

	public static final long DISABLE_SEND = 0;

	public static final long UNASSIGNED_SEND_INTERVAL = -1;

	public static final long DEFAULT_SEND_INTERVAL = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);

	private static final int MAX_UNSENT_SIZE = 3;

	private static Stats stats = new Stats();

	private Variables variables;

	private List<Variables> unsentVariables = new ArrayList<Variables>();

	private long lastSendTry;

	private long started;

	private long sendInterval;

	private Timer sendTimer;

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
		try {
			setSendInterval(Long.parseLong(MoSyncTool.getDefault().getProperty(SEND_INTERVAL_PROP)));
		} catch (Exception e) {
			// Ignore.
			setSendInterval(UNASSIGNED_SEND_INTERVAL);
		}
	}

	private void initVariables(boolean load) {
		variables = new Variables();
		if (load) {
			loadState();
		}
		variables.get(TimeStamp.class, "ide.started").set(started);
		variables.get(TimeStamp.class, "stats.collection.started").set();
	}

	public void stop() throws Exception {
		saveState();
	}

	private void saveState() {
		IPreferenceStore store = CoreMoSyncPlugin.getDefault().getPreferenceStore();
		store.setValue(STATS_PREF, toString(Arrays.asList(variables)));
		synchronized (unsentVariables) {
			store.setValue(UNSENT_STATS_PREF, toString(unsentVariables));
		}
		store.setValue(LAST_SEND_TRY_PREF, lastSendTry);
	}

	private String toString(List<Variables> variableList) {
		JSONArray array = new JSONArray();
		for (Variables v : variableList) {
			JSONObject o = new JSONObject();
			v.write(o);
			array.add(o);
		}
		return array.toJSONString();
	}

	private List<Variables> fromString(String str) throws Exception {
		ArrayList<Variables> vars = new ArrayList<Variables>();
		JSONArray array = (JSONArray) new JSONParser().parse(str);
		for (int i = 0; i < array.size(); i++) {
			JSONObject o = (JSONObject) array.get(i);
			Variables v = new Variables();
			v.read(o);
			vars.add(v);
		}
		return vars;
	}

	private void loadState() {
		IPreferenceStore store = CoreMoSyncPlugin.getDefault().getPreferenceStore();
		String stats = store.getString(STATS_PREF);
		String unsent = store.getString(UNSENT_STATS_PREF);
		lastSendTry = store.getLong(LAST_SEND_TRY_PREF);
		try {
			if (!Util.isEmpty(stats)) {
				List<Variables> variableList = fromString(stats);
				if (variableList.size() > 0) {
					variables = variableList.get(0);
				}
			}
			if (!Util.isEmpty(unsent)) {
				unsentVariables = fromString(unsent);
			}
		} catch (Exception e) {
			CoreMoSyncPlugin.getDefault().log(e);
		}
	}

	public Variables getVariables() {
		return variables;
	}

	private void send() {
		if (sendInterval == DISABLE_SEND || sendInterval == UNASSIGNED_SEND_INTERVAL) {
			return;
		}
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace("Sending stats at " + new Date());
		}
		final Variables variablesToSend = variables;
		variablesToSend.get(TimeStamp.class, "send.time").set();
		initVariables(false);
		try {
			lastSendTry = System.currentTimeMillis();
			ArrayList<Variables> sendThese = new ArrayList<Variables>();
			sendThese.addAll(unsentVariables);
			sendThese.add(variablesToSend);
			String stats = Stats.this.toString(sendThese);
			CoreMoSyncPlugin.getDefault().getUpdater().sendStats(stats);
			// Now clear!
			unsentVariables.clear();
			saveState();
		} catch (Exception e) {
			// If failed, log and retain these variables until next try...
			// And we'll try at next startup regardless.
			addToUnsent(variablesToSend);
			e.printStackTrace();
		}
	}

	public String getContentsToSend() {
		ArrayList<Variables> sendThese = new ArrayList<Variables>();
		sendThese.addAll(unsentVariables);
		sendThese.add(variables);
		return toString(sendThese);
	}

	private void clear() {
		clearUnsent();
		initVariables(false);
		saveState();
	}

	private void addToUnsent(Variables v) {
		synchronized (unsentVariables) {
			unsentVariables.add(v);
			if (unsentVariables.size() > MAX_UNSENT_SIZE) {
				unsentVariables.remove(0);
			}
		}
	}

	private void clearUnsent() {
		synchronized (unsentVariables) {
			unsentVariables.clear();
		}
	}

	public long getSendInterval() {
		return sendInterval;
	}

	public void setSendInterval(long sendInterval) {
		long previousInterval = this.sendInterval;
		if (previousInterval != sendInterval) {
			MoSyncTool.getDefault().setProperty(SEND_INTERVAL_PROP, Long.toString(sendInterval));
			this.sendInterval = sendInterval;
		}
		boolean wasDisabled = previousInterval == 0;
		stopTimer();
		if (sendInterval == DISABLE_SEND) {
			clear();
		} else {
			if (wasDisabled) {
				// Don't try right away.
				lastSendTry = System.currentTimeMillis();
			}
			startTimer(sendInterval);
		}
	}

	private long timeSinceLastSendTry() {
		return System.currentTimeMillis() - lastSendTry;
	}

	private void startTimer(long interval) {
		stopTimer();
		if (interval > 0) {
			sendTimer = new Timer();
			sendTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					send();
				}
			}, Math.max(0, interval - timeSinceLastSendTry()), interval);
		}
	}

	private void stopTimer() {
		if (sendTimer != null) {
			sendTimer.cancel();
		}
	}


}
