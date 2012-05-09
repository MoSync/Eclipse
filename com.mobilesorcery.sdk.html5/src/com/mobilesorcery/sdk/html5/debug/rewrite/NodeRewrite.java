package com.mobilesorcery.sdk.html5.debug.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.JSdoc;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.html5.debug.Position;

public class NodeRewrite {

	protected ASTNode node;
	protected ISourceSupport rewriter;
	private ArrayList<NodeRewrite> rewrites = new ArrayList<NodeRewrite>();
	private HashMap<ASTNode, NodeRewrite> rewritesByNode = new HashMap<ASTNode, NodeRewrite>();

	public NodeRewrite(ISourceSupport rewriter, ASTNode node) {
		this.rewriter = rewriter;
		this.node = node;
	}
	
	public void addChild(NodeRewrite rewrite) {
		if (rewrite == this) {
			throw new IllegalArgumentException();
		}
		if (!isAncestor(node, rewrite.getNode())) {
			throw new IllegalArgumentException();
		}
		this.rewrites.add(rewrite);
		this.rewritesByNode.put(rewrite.getNode(), rewrite);
	}

	private boolean isAncestor(ASTNode ancestor, ASTNode node) {
		if (node == null) {
			return false;
		}
		if (ancestor == null) {
			return true;
		}
		if (ancestor == node.getParent()) {
			return true;
		} else {
			return isAncestor(ancestor, node.getParent());
		}
	}

	public ASTNode getNode() {
		return node;
	}

	public boolean supports(IFilter<String> features, String feature) {
		if (features == null) {
			return true;
		} else {
			return features.accept(feature);	
		}
		
	}

	public Position getPosition(ASTNode node, boolean before) {
		return rewriter.getPosition(node, before);
	}
	
	public String getRewrite(ASTNode node) {
		NodeRewrite rewrite = rewritesByNode.get(node);
		if (rewrite == null) {
			return rewriter.getSource(node);
		} else {
			return rewrite.rewrite();
		}
	}
	
	public String rewrite(IFilter<String> features) {
		return defaultRewrite(features);
	}
	
	public String rewrite() {
		return rewrite(null);
	}
	
	protected String defaultRewrite(IFilter<String> features) {
		TreeMap<Integer, NodeRewrite> sortedByPosition = new TreeMap<Integer, NodeRewrite>();
		for (NodeRewrite rewriter : rewrites) {
			sortedByPosition.put(getPosition(rewriter.getNode(), true).getPosition(), rewriter);
		}
		StringBuffer result = new StringBuffer();
		String source = getSource();
		int start = node == null ? 0 : node.getStartPosition();
		int length = node == null ? source.length() : node.getLength();
		int lastPos = start;
		for (Integer position : sortedByPosition.keySet()) {
			// To handle js docs!
			lastPos = lastPos > position ? position : lastPos;
			result.append(source.substring(lastPos, position));
			NodeRewrite rewrite = sortedByPosition.get(position);
			result.append(rewrite.rewrite(features));
			int replacedLength = rewrite.getNode().getLength();
			lastPos = position + replacedLength;
		}
		result.append(source.substring(lastPos, start + length));
		return result.toString();
	}

	private String getSource() {
		return rewriter.getSource();
	}

	public static IFilter<String> include(final String... features) {
		return new IFilter<String>() {
			@Override
			public boolean accept(String feature) {
				for (int i = 0; i < features.length; i++) {
					if (features[i].equals(feature)) {
						return true;
					}
				}
				return false;
			}			
		};
	}

}
