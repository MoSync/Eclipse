/**
 * 
 */
package com.mobilesorcery.sdk.fontsupport.internal.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mobilesorcery.sdk.core.Pair;

public class BMFontInfo {
	private int version;
	
	private List<BMFontInfoBlock> blocks = new ArrayList<BMFontInfoBlock>();
	
	private HashMap<Pair<Integer, Integer>, Integer> kernings = new HashMap<Pair<Integer, Integer>, Integer>();
	
	public int getVersion() {
		return version;
	}
	
	public BMFontInfoBlock getFirst(int type) {
		for (int i = 0; i < blocks.size(); i++) {
			if (blocks.get(i).isType(type)) {
				return blocks.get(i);
			}
		}
		
		return null;
	}

	protected void setKerning(int first, int second, int amount) {
		Pair kerningPair = new Pair(first, second);
		kernings.put(kerningPair, amount);
	}
	
	public int getKerning(int first, int second) {
		Integer result = kernings.get(new Pair(first, second));
		return result == null ? 0 : result;
	}
	
	public boolean hasKernings() {
		return !kernings.isEmpty();
	}

	void setVersion(int version) {
		this.version = version;
	}

	public void addBlock(BMFontInfoBlock block) {
		blocks.add(block);
	}
	
}