package com.mobilesorcery.sdk.builder.android.launch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mobilesorcery.sdk.core.LineReader.ILineHandler;

public class CollectingLineHandler implements ILineHandler {

	ArrayList<String> lines = new ArrayList<String>();
	private boolean stopped;

	public void newLine(String line) {
		lines.add(line);
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