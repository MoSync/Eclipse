package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.jsdt.debug.core.jsdi.ObjectReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.Value;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;

public class ReloadObjectReference extends ReloadValue implements ObjectReference {

	private String ctr;
	private final List properties = new ArrayList<ReloadProperty>();
	private final String repr;
	private final String className;
	private final Number oid;

	protected ReloadObjectReference(ReloadVirtualMachine vm, String repr, String className, Number oid) {
		super(vm);
		this.repr = repr;
		this.className = className;
		this.oid = oid;
	}

	@Override
	public String valueString() {
		return repr;
	}

	@Override
	public String className() {
		return className;
	}

	@Override
	public Value constructor() {
		return virtualMachine().mirrorOfNull();
	}

	@Override
	public Value prototype() {
		return virtualMachine().mirrorOfNull();
	}

	@Override
	public List properties() {
		return properties;
	}

	@Override
	public Number id() {
		return oid == null ? 0 : oid;
	}

	public void addProperty(ReloadProperty property) {
		properties.add(property);
	}

}
