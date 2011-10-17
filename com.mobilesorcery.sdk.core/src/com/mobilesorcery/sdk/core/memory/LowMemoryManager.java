package com.mobilesorcery.sdk.core.memory;

import javax.management.*;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Based on an older Java Specialists issue:
 * http://www.javaspecialists.eu/archive/Issue092.html
 *
 * Original code written by Dr. Heinz M. Kabutz.
 */
public class LowMemoryManager implements NotificationListener {
	private final CopyOnWriteArrayList<MemoryLowListener> listeners = new CopyOnWriteArrayList<MemoryLowListener>();
	private final IdentityHashMap<MemoryLowListener, Integer> notificationCounts = new IdentityHashMap<MemoryLowListener, Integer>();
	private final double thresholdPercentage;
	private boolean active = false;

	public LowMemoryManager(double thresholdPercentage) {
		this.thresholdPercentage = thresholdPercentage;
		addDefaultMonitoredPools();
		setPercentageUsageThreshold(thresholdPercentage);
	}

	public void addMemoryLowListener(MemoryLowListener listener, int maxNotificationCount) {
		if (listeners.isEmpty()) {
			activate();
		}
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
		notificationCounts.put(listener, maxNotificationCount == 0 ? Integer.MAX_VALUE : maxNotificationCount);
	}

	public void removeMemoryLowListener(MemoryLowListener listener) {
		listeners.remove(listener);
		notificationCounts.remove(listener);
		if (listeners.isEmpty()) {
			deactivate();
		}
	}

	private void activate() {
		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		NotificationEmitter emitter = (NotificationEmitter) mbean;
		emitter.addNotificationListener(this, null, null);
		active = true;
	}

	private void deactivate() {
		if (active) {
			MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
			NotificationEmitter emitter = (NotificationEmitter) mbean;
			try {
				emitter.removeNotificationListener(this);
			} catch (ListenerNotFoundException e) {
				// Ignore & log
				CoreMoSyncPlugin.getDefault().log(e);
			}
		}
	}

	private final ArrayList<MemoryPoolMXBean> monitoredPools = new ArrayList<MemoryPoolMXBean>();

	public void setPercentageUsageThreshold(double percentage) {
		for (MemoryPoolMXBean monitoredPool : monitoredPools) {
			long maxMemory = monitoredPool.getUsage().getMax();
			long threshold = (long) (maxMemory * thresholdPercentage);
			monitoredPool.setUsageThreshold(threshold);
		}
	}

	/**
	 * Tenured Space Pool can be determined by it being of type HEAP and by it
	 * being possible to set the usage threshold.
	 */
	private void addDefaultMonitoredPools() {
		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			if (pool.isUsageThresholdSupported()) {
				monitoredPools.add(pool);
				if (CoreMoSyncPlugin.getDefault().isDebugging()) {
					CoreMoSyncPlugin.trace("Monitoring memory pool {0}", pool.getName());
				}
			}
		}
	}

	@Override
	public synchronized void handleNotification(Notification notification, Object handback) {
		if (notification.getType().equals(
				MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
			for (MemoryLowListener listener : listeners) {
				Integer notificationCount = notificationCounts.get(listener);
				int newNotificationCount = notificationCount == null ? 0 : notificationCount - 1;
				if (newNotificationCount < 0) {
					removeMemoryLowListener(listener);
				} else {
					notificationCounts.put(listener, newNotificationCount);
					listener.memoryUsageLow();
				}
			}
		}
	}

	public List<MemoryPoolMXBean> getMonitoredPools() {
		return Collections.unmodifiableList(monitoredPools);
	}

	public boolean isUsageExceeded(MemoryPoolMXBean pool) {
		return pool.isUsageThresholdSupported() && pool.isUsageThresholdExceeded();
	}

}