package com.mobilesorcery.sdk.html5.debug.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.BodyDeclaration;
import org.eclipse.wst.jsdt.core.dom.JSdoc;
import org.eclipse.wst.jsdt.core.dom.rewrite.ASTRewrite;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.Position;

/**
 * The base class for instrumenting JavaScript AST nodes
 * @author Mattias Bybro, mattias.bybro@mosync.com
 *
 */
public class NodeRewrite {

	private static final NodeRewrite NULL = new NodeRewrite(null, null);
	
	private ASTNode node;
	protected ISourceSupport rewriter;
	private ArrayList<NodeRewrite> rewrites = new ArrayList<NodeRewrite>();
	private HashMap<ASTNode, NodeRewrite> rewritesByNode = new HashMap<ASTNode, NodeRewrite>();

	private String blacklistReason = null;

	public NodeRewrite(ISourceSupport source, ASTNode node) {
		this.rewriter = source;
		this.node = node;
		if (node != null && (node.getFlags() & ASTNode.MALFORMED) != 0) {
			setBlacklisted("Unknown syntactical error.");	
		}
		// There is code that is ok that gets flagged MALFORMED, so we can't
		// really throw an exception here.
		//if (node != null && (node.getFlags() & ASTNode.MALFORMED) != 0) {
		//	System.err.println("MALFORMED!" + node);//throw new IllegalArgumentException();
		//}
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
		if (!Html5Plugin.getDefault().isFeatureSupported(feature)) {
			return false;
		}
		if (features == null) {
			// TODO: return whatever is enabled for this rewrite
			return true;
		} else {
			return features.accept(feature);	
		}
	}

	public Position getPosition(ASTNode node, boolean before) {
		if (node == null) {
			return null;
		}
		return rewriter.getPosition(node, before);
	}
	
	public NodeRewrite getRewrite(ASTNode node) {
		NodeRewrite rewrite = rewritesByNode.get(node);
		return rewrite == null ? NodeRewrite.NULL : rewrite;
	}
	
	public void rewrite(IFilter<String> features, IRewrite rewrite) {
		defaultRewrite(features, rewrite);
	}
	
	protected void defaultRewrite(IFilter<String> features, IRewrite rewrite) {;
		TreeMap<Integer, NodeRewrite> sortedByPosition = new TreeMap<Integer, NodeRewrite>();
		for (NodeRewrite rewriter : rewrites) {
			sortedByPosition.put(getPosition(rewriter.getNode(), true).getPosition(), rewriter);
		}
		
		for (Map.Entry<Integer, NodeRewrite> rewriteEntry : sortedByPosition.entrySet()) {
			NodeRewrite nodeRewrite = rewriteEntry.getValue();
			if (nodeRewrite.isBlacklisted() == null) {
				nodeRewrite.rewrite(features, rewrite);
			}
		}

		/*if (rewrites.isEmpty()) {
			return getSource(node);
		}
		TreeMap<Integer, NodeRewrite> sortedByPosition = new TreeMap<Integer, NodeRewrite>();
		for (NodeRewrite rewriter : rewrites) {
			sortedByPosition.put(getPosition(rewriter.getNode(), true).getPosition(), rewriter);
		}
		
		ASTNode replacedNode = getNode();
		Position startPosition = getPosition(replacedNode,true);
		Position endPosition = getPosition(replacedNode, false);
		
		StringBuffer result = new StringBuffer();
		String source = getSource();
		
		int start = replacedNode == null ? 0 : startPosition.getPosition();
		int length = replacedNode == null ? source.length() : endPosition.getPosition() - startPosition.getPosition();
		int lastPos = start;
		
		for (Integer position : sortedByPosition.keySet()) {
			if (lastPos > position) {
				lastPos = position; //throw new IllegalStateException();
			}
			String originalSnippet = source.substring(lastPos, position);
			result.append(originalSnippet);
			NodeRewrite rewrite = sortedByPosition.get(position);
			String rewrittenSnippet = rewrite.rewrite(features);
			result.append(rewrittenSnippet);
			REWRITES += originalSnippet.length() + rewrittenSnippet.length();
			ASTNode rewriteNode = rewrite.getNode();
			int replacedLength = getPosition(rewriteNode, false).getPosition() - getPosition(rewriteNode, true).getPosition();
			lastPos = position + replacedLength;
		}
		if (lastPos < start + length) {
			REWRITES += start + length - lastPos;
			result.append(source.substring(lastPos, start + length));
		}
		System.err.println("OOPS: " + REWRITES);
		return result.toString();*/
	}

	protected String getSource(ASTNode node) {
		Assert.isNotNull(node);
		int start = getPosition(node, true).getPosition();
		int end = getPosition(node, false).getPosition();
		return rewriter.getSource(start, end);
	}
	
	protected String getSource() {
		return rewriter.getSource();
	}
	
	protected void setBlacklisted(String reason) {
		this.blacklistReason = reason;
	}
	
	public String isBlacklisted() {
		return blacklistReason;
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
