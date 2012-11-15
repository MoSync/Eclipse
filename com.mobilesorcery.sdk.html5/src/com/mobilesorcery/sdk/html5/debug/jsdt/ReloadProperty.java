package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.wst.jsdt.debug.core.jsdi.Property;
import org.eclipse.wst.jsdt.debug.core.jsdi.Value;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadProperty extends ReloadMirror implements Property {

	protected final String name;
	private final ReloadStackFrame frame;
	protected final ReloadProperty parent;

	ReloadProperty(ReloadVirtualMachine vm, ReloadStackFrame frame, ReloadProperty parent, String name) {
		super(vm);
		this.frame = frame;
		this.parent = parent;
		this.name = name;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Value value() {
		return frame.getValue(this);
	}
	
	protected String getSymbolToEvaluate() {
		String evalName = "arguments".equals(name) ? "____arguments" : name;
		if (parent != null) {
			return parent.getSymbolToEvaluate() + "." + evalName;
		}
		return evalName;
	}

	public List<String> getIntrinsicProperties() throws InterruptedException, TimeoutException {
		ArrayList<String> result = new ArrayList();
		if ("arguments".equals(name)) {
			result.add("length");
			String evalName = getSymbolToEvaluate();
			Object evaledLength = frame.thread().evaluate(evalName + ".length");
			if (evaledLength instanceof Number) {
				int length = ((Number) evaledLength).intValue();
				for (int i = 0; i < length; i++) {
					result.add(Integer.toString(i));
				}
			}
		}
		return result;
	}

}
