package com.mobilesorcery.sdk.internal;

import java.io.IOException;

import com.mobilesorcery.sdk.core.IUpdater;

public class HeadlessUpdater implements IUpdater {

	private static final IUpdater INSTANCE = new HeadlessUpdater();

	private HeadlessUpdater() { }

	public final static IUpdater getInstance() {
		return INSTANCE;
	}

	@Override
	public void update(boolean isStartedByUser) {

	}

	@Override
	public void register(boolean isStartedByUser) {

	}

	@Override
	public void sendStats(String stats) throws IOException {

	}

	@Override
	public void dispose() {

	}

}
