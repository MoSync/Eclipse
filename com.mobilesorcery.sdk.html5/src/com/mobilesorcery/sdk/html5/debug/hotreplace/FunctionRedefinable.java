package com.mobilesorcery.sdk.html5.debug.hotreplace;

import java.util.List;

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.debug.core.jsdi.StackFrame;

import com.mobilesorcery.sdk.core.IProvider;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.RedefineException;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadStackFrame;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadThreadReference;
import com.mobilesorcery.sdk.html5.debug.jsdt.ReloadVirtualMachine;

public class FunctionRedefinable extends ASTRedefinable {

	private Boolean isAnonymous;

	public FunctionRedefinable(IRedefinable parent, IProvider<String, ASTNode> source,
			ReloadVirtualMachine vm, ASTNode node) {
		super(parent, source, vm, node);
		this.isAnonymous = isAnonymous();
	}

	@Override
	public boolean canRedefine(IRedefinable toBeRedefined, boolean inPlace) {
		boolean canRedefine = canRedefine(toBeRedefined, inPlace);
		if (isAnonymous() && !inPlace) {
			// Is this the only anonymous function?
			List<IRedefinable> childrenToBeRedefined = toBeRedefined
					.getParent().getChildren();
			List<IRedefinable> siblings = getParent().getChildren();
			canRedefine &= (countAnonymousFunctions(childrenToBeRedefined)[0] <= 1 && countAnonymousFunctions(siblings)[0] <= 1);
		}
		if (isAnonymous() || inPlace) {
			canRedefine &= inPlace;
		}
		return canRedefine;
	}

	private int[] countAnonymousFunctions(List<IRedefinable> children) {
		int[] result = new int[2];
		int ix = 0;
		for (IRedefinable child : children) {
			if (child instanceof FunctionRedefinable
					&& ((FunctionRedefinable) child).isAnonymous()) {
				result[0]++;
			}
			if (child == this) {
				result[1] = ix;
			}
			ix++;
		}
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

	@Override
	public void redefine(IRedefinable toBeRedefined, boolean inPlace)
			throws RedefineException {
		FunctionDeclaration decl = getFunctionDeclaration();
		try {
			if (canRedefine(toBeRedefined, inPlace)) {
				vm.evaluate(getSource(decl));
				if (inPlace) {
					Block body = decl.getBody();
					ReloadThreadReference thread = vm.mainThread();
					int frameToDropTo = thread.frameCount() - 1;
					StackFrame frame = thread.frame(frameToDropTo);
					String expression = "MoSyncDebugProtocol.obsoleteStackTop();"
							+ getSource(body);
					// +1 will make the drop to frame request cleared from the
					// client,
					// the expression will instead trigger step request
					frame.evaluate(ReloadStackFrame
							.createDropToFrameExpression(frameToDropTo + 1,
									expression));
				}
			}
		} catch (Exception e) {
			throw RedefineException.wrap(e);
		}
	}

	@Override
	public String key() {
		if (isAnonymous()) {
			int index = countAnonymousFunctions(getChildren())[1];
			return "<" + index + ">";
		} else {
			return getFunctionDeclaration().getName().getIdentifier();
		}
	}
}
