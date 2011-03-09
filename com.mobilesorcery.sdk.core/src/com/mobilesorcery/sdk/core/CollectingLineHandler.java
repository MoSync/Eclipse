package com.mobilesorcery.sdk.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mobilesorcery.sdk.core.LineReader.LineAdapter;


public class CollectingLineHandler extends LineAdapter {

	ArrayList<String> lines = new ArrayList<String>();
	private boolean stopped;
	private int maxLines;

	public CollectingLineHandler() {
		this(1024);
	}
	
	public CollectingLineHandler(int maxLines) {
		this.maxLines = maxLines;
	}
	
	public void newLine(String line) {
		if (lines.size() <= maxLines) {
			lines.add(line);
		}
		System.out.println(line);
	}

	public void stop(IOException e) {
		stopped = true;
	}

	public List<String> getLines() {
		return lines;
	}
	
	public boolean isStopped() {
		return stopped;
	}

}