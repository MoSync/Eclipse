package com.mobilesorcery.sdk.html5.debug.rewrite;

import org.eclipse.wst.jsdt.core.dom.ASTNode;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.html5.debug.Position;

public interface ISourceSupport {

	public String getSource(ASTNode node);

	public String getSource(int start, int end);
	
	public Position getPosition(ASTNode node, boolean before);

	public String getSource();
	
	public String getInstrumentedSource(IFilter<String> features, ASTNode node);
	
	//public String getInstrumentedSource(ASTNode node);

}
