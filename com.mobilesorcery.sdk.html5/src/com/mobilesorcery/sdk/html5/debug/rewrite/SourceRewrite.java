package com.mobilesorcery.sdk.html5.debug.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.wst.jsdt.core.dom.ASTNode;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.debug.Position;

public class SourceRewrite implements IRewrite {

	private int currentPosition;
	private String source;
	private final TreeMap<Integer, List<String>> insertions = new TreeMap<Integer, List<String>>();
	private int offset;

	public SourceRewrite(String source) {
		this(source, null);
	}
	
	public SourceRewrite(String source, ASTNode node) {
		offset = node == null ? 0 : node.getStartPosition();
		int length = node == null ? source.length() : node.getLength();
		this.source = source.substring(offset, offset + length);
	}

	@Override
	public void seek(Position position) {
		seek(position.getPosition());
	}
	
	@Override
	public void seek(int position) {
		this.currentPosition = position;
	}
	
	public int skip(int delta) {
		this.currentPosition += delta;
		return currentPosition;
	}

	@Override
	public void insert(String insertion) {
		List<String> insertionsForPosition = insertions.get(currentPosition);
		if (insertionsForPosition == null) {
			insertionsForPosition = new ArrayList<String>();
			insertions.put(currentPosition, insertionsForPosition);
		}

		insertionsForPosition.add(insertion);
	}

	public String rewrite() {
		StringBuffer result = new StringBuffer();
		int prevPos = offset;
		int posDelta = 0;
		for (Map.Entry<Integer, List<String>> insertion : insertions
				.entrySet()) {
			Integer pos = insertion.getKey();
			List<String> insertionsAtPos = insertion.getValue();
			result.append(source.substring(prevPos - offset, pos - offset));
			String insertionStr = Util.join(insertionsAtPos.toArray(), "");
			result.append(insertionStr);
			prevPos = pos;
			posDelta += insertionStr.length();
			//movedSourceMap.put(prevPos, posDelta);
		}
		result.append(source.substring(prevPos - offset));
		return result.toString();
	}

}
