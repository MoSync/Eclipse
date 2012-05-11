package com.mobilesorcery.sdk.html5.debug.rewrite;

import com.mobilesorcery.sdk.html5.debug.Position;

public interface IRewrite {

	public void seek(Position position);
	public void insert(String insertion);
	
}
