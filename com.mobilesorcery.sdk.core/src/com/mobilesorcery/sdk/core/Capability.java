package com.mobilesorcery.sdk.core;

public class Capability implements ICapability, Comparable<ICapability> {

	private final String name;
	private final Object value;
	private final String type;
	private final CapabilityFragmentation fragmentation;
	private final CapabilityState state;

	public Capability(String name, CapabilityState state, String type, Object value, CapabilityFragmentation fragmentation) {
		this.name = name;
		this.state = state;
		this.type = type;
		this.value = value;
		this.fragmentation = fragmentation;
	}

	public Capability(String name, CapabilityState state) {
		this(name, state, "", Boolean.TRUE, CapabilityFragmentation.RUNTIME);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public CapabilityFragmentation getFragmentation() {
		return fragmentation;
	}

	@Override
	public CapabilityState getState() {
		return state;
	}

	@Override
	public int compareTo(ICapability o) {
		return name.compareTo(o.getName());
	}

}
