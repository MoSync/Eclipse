package com.mobilesorcery.sdk.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mobilesorcery.sdk.core.LineReader.LineAdapter;


public class CollectingLineHandler extends LineAdapter {

	List<String> lines = new ArrayList<String>();
	private boolean stopped;
	private CountDownLatch stopLatch = new CountDownLatch(1);
	private final int maxLines;
	private Process process;

	public CollectingLineHandler() {
		this(1024);
	}

	public CollectingLineHandler(int maxLines) {
		this.maxLines = maxLines;
	}

	@Override
	public synchronized void newLine(String line) {
		if (lines.size() <= maxLines) {
			lines.add(line);
		}
	}

	@Override
	public void start(Process process) {
		this.process = process;
	}
	
	public Process getProcess() {
		return process;
	}
	
	@Override
	public void stop(IOException e) {
		stopped = true;
		stopLatch.countDown();
	}

	public synchronized List<String> getLines() {
		return new ArrayList<String>(lines);
	}

	public synchronized String getFirstLine() {
		return lines.size() == 0 ? "" : lines.get(0);
	}

	public boolean isStopped() {
		return stopped;
	}

	public void awaitStopped(int timeout, TimeUnit unit) throws InterruptedException {
		stopLatch.await(timeout, unit);
	}

}