package com.mobilesorcery.sdk.html5.debug;

import java.util.Map.Entry;
import java.util.TreeMap;

public class LineMap {

	private final TreeMap<Integer, Integer> lines = new TreeMap<Integer, Integer>();

	public LineMap(String text) {
		char[] chars = text.toCharArray();
		int startOfLine = 0;
		int line = 1;
		for (int i = 0; i < chars.length; i++) {
			char ch = chars[i];
			char peek = i < chars.length - 1 ? chars[i + 1] : '\0';
			if (ch == '\r' || ch == '\n') {
				line = addLine(startOfLine, line);
				if (peek == '\n' && ch == '\r') {
					i++;
				}
				startOfLine = i + 1;
			}
		}

	}

	private int addLine(int startIx, int line) {
		lines.put(startIx, line);
		return line + 1;
	}

	public int getLine(int pos) {
		Integer line = lines.floorKey(pos);
		if (line == null) {
			return 1;
		}
		return lines.get(line);
	}
	
	public int getColumn(int pos) {
		Entry<Integer, Integer> entry = lines.floorEntry(pos);
		if (entry == null) {
			return pos;
		}
		return lines.get(entry.getKey()) - pos;
	}

	public int getLineCount() {
		return lines.size() + 1;
	}

}
