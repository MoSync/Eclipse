package com.mobilesorcery.sdk.core.stats;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;

/**
 * <p>Handles usage statistics. <b>Implementation note:</b> only one eclipse instance will be used for sending stats;
 * if another instance is started and run concurrently, no stats will be sent by it.</p>
 * We may change this behaviour in the future.
 * @author Mattias Bybro
 *
 */
public class Stats {

	private static final String SEND_INTERVAL_PROP = "send.interval";

	public static final long DISABLE_SEND = 0;

	public static final long UNASSIGNED_SEND_INTERVAL = -1;

	public static final long DEFAULT_SEND_INTERVAL = TimeUnit.MILLISECONDS
			.convert(1, TimeUnit.DAYS);

	private static final int MAX_UNSENT_SIZE = 3;

	private static Stats stats = new Stats();

	private Variables variables;

	private List<Variables> unsentVariables = new ArrayList<Variables>();

	private long lastSendTry;

	private long started;

	private long sendInterval;

	private Timer sendTimer;

	private FileLock statsLock;

	private FileOutputStream statsLockFileStream;

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
			setSendInterval(Long.parseLong(MoSyncTool.getDefault().getProperty(
					SEND_INTERVAL_PROP)), true);
		} catch (Exception e) {
			// Ignore.
			setSendInterval(UNASSIGNED_SEND_INTERVAL, true);
		}
	}

	private void initVariables(boolean load) {
		variables = new Variables();
		if (load) {
			loadState(false);
		}
		variables.get(TimeStamp.class, "ide.started").set(started);
		variables.get(TimeStamp.class, "stats.collection.started").set();
	}

	public void stop() throws Exception {
		saveState();
		if (statsLock != null) {
			statsLock.release();
		}
		Util.safeClose(statsLockFileStream);
	}

	private IPath getStatsLocation() {
		return MoSyncTool.getDefault().getMoSyncHome().append("etc/stats");
	}

	private void saveState() {
		if (!anotherIDEIsRunning()) {
			try {
				File unsentFile = getStatsLocation().append("unsent.json").toFile();
				Util.writeToFile(unsentFile, toString(unsentVariables));
				File statsFile = getStatsLocation().append("current.json").toFile();
				Util.writeToFile(statsFile, toString(Arrays.asList(variables)));
				File lastSendTryFile = getStatsLocation().append("timestamp")
						.toFile();
				Util.writeToFile(lastSendTryFile, Long.toString(lastSendTry));
			} catch (Exception e) {
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}
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

	private void loadState(boolean force) {
		if (force || !anotherIDEIsRunning()) {
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				CoreMoSyncPlugin.trace("Loading previously saved usage statistics.");
			}
			try {
				File unsentFile = getStatsLocation().append("unsent.json")
						.toFile();
				String unsent = unsentFile.exists() ? Util.readFile(unsentFile
						.getAbsolutePath()) : null;
				File statsFile = getStatsLocation().append("current.json")
						.toFile();
				String stats = unsentFile.exists() ? Util.readFile(statsFile
						.getAbsolutePath()) : null;
				File lastSendTryFile = getStatsLocation().append("timestamp")
						.toFile();
				String lastSendTryStr = lastSendTryFile.exists() ? Util
						.readFile(lastSendTryFile.getAbsolutePath()) : null;
				lastSendTry = System.currentTimeMillis();
				try {
					if (!Util.isEmpty(lastSendTryStr)) {
						lastSendTry = Long.parseLong(lastSendTryStr.trim());
					}
				} catch (Exception e) {
					// Ignore.
				}

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
	}

	public Variables getVariables() {
		return variables;
	}

	private void send() {
		if (sendInterval == DISABLE_SEND
				|| sendInterval == UNASSIGNED_SEND_INTERVAL) {
			return;
		}
		// There may be several workspaces -- but only one is meant for sending.
		if (anotherIDEIsRunning()) {
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

	public boolean anotherIDEIsRunning() {
		// If we already have the lock, then we know we're it
		// Otherwise, we will try to get the file lock.
		// The lock is released once the stop method has been called.
		if (statsLock != null) {
			return false;
		}
		try {
			statsLockFileStream = null;
			File lockFile = getStatsLocation().append(".lock").toFile();
			if (!lockFile.exists()) {
				lockFile.getParentFile().mkdirs();
			}
			statsLockFileStream = new FileOutputStream(lockFile);
			statsLock = statsLockFileStream.getChannel().tryLock();
			if (CoreMoSyncPlugin.getDefault().isDebugging()) {
				String lockState = statsLock == null ? "Could not lock" : "Locked";
				CoreMoSyncPlugin.trace(lockState + " stats file @" + new Date());
			}
			if (statsLock != null) {
				loadState(true);
			}
			return statsLock == null;
		} catch (Exception e) {
			return true; // Ok, maybe not but we don't know
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
		setSendInterval(sendInterval, false);
	}

	private void setSendInterval(long sendInterval, boolean init) {
		long previousInterval = this.sendInterval;
		if (previousInterval != sendInterval) {
			MoSyncTool.getDefault().setProperty(SEND_INTERVAL_PROP,
					Long.toString(sendInterval));
			this.sendInterval = sendInterval;
		}
		boolean wasDisabled = previousInterval == 0;
		stopTimer();
		if (sendInterval == DISABLE_SEND) {
			clear();
		} else {
			if (wasDisabled && !init) {
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
			long delay = Math.max(0, interval - timeSinceLastSendTry());
			sendTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					send();
				}
			}, delay, interval);
		}
	}

	private void stopTimer() {
		if (sendTimer != null) {
			sendTimer.cancel();
		}
	}
}
