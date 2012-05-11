package com.mobilesorcery.sdk.html5.debug.hotreplace;

import java.util.List;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.debug.core.jsdi.StackFrame;

import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.IRedefiner;
import com.mobilesorcery.sdk.html5.debug.JSODDSupport;
import com.mobilesorcery.sdk.html5.debug.RedefineException;
import com.mobilesorcery.sdk.html5.debug.RedefinitionResult;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadStackFrame;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadThreadReference;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.debug.rewrite.ISourceSupport;
import com.mobilesorcery.sdk.html5.debug.rewrite.NodeRewrite;

public class FunctionRedefinable extends ASTRedefinable {

	private Boolean isAnonymous;
	private String subkey;

	public FunctionRedefinable(IRedefinable parent, ISourceSupport source, ASTNode node) {
		super(parent, source, node);
		this.isAnonymous = isAnonymous();
	}

	private int[] countAnonymousFunctions(List<IRedefinable> children) {
		int[] result = new int[2];
		int count = 0;
		for (IRedefinable child : children) {
			if (child instanceof FunctionRedefinable
					&& ((FunctionRedefinable) child).isAnonymous()) {
				count++;
			}
			if (child == this) {
				result[1] = count;
			}
		}
		result[0] = count;
		return result;
	}

	private boolean isAnonymous() {
		if (isAnonymous == null) {
			isAnonymous = getFunctionDeclaration().getName() == null;
		}
		return isAnonymous;
	}

	private FunctionDeclaration getFunctionDeclaration() {
		return (FunctionDeclaration) getNode();
	}

	public RedefinitionResult canRedefine(IRedefinable replacement) {
		if (isAnonymous()) {
			// Is this the only anonymous function? That's a nice heurisitic for assuming it's the 'same' function.
			List<IRedefinable> childrenToBeRedefined = replacement.getParent().getChildren();
			List<IRedefinable> siblings = getParent().getChildren();
			if (countAnonymousFunctions(childrenToBeRedefined)[0] > 1 || countAnonymousFunctions(siblings)[0] > 1) {
				return RedefinitionResult.fail("Cannot replace anonymous functions if there is more than one anonymous function in the same scope.");
			}
		}
		return RedefinitionResult.ok();
	}

	public String getFunctionSource() {
		return getInstrumentedSource(NodeRewrite.include(JSODDSupport.LINE_BREAKPOINTS), getFunctionDeclaration());
	}
	
	@Override
	public String key() {
		if (subkey == null) {
			if (isAnonymous()) {
				int index = countAnonymousFunctions(getParent().getChildren())[1];
				subkey =  "<" + index + ">";
			} else {
				subkey = getFunctionName();
			}
		}
		
		return constructKey(subkey);
	}

	public String getFunctionName() {
		return isAnonymous() ? Html5Plugin.ANONYMOUS_FUNCTION :
		getFunctionDeclaration().getName().getIdentifier();
	}

}
