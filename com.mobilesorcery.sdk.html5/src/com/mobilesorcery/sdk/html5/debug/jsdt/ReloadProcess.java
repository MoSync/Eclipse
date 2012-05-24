package com.mobilesorcery.sdk.html5.debug.jsdt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.live.JSODDServer;

public class ReloadProcess extends PlatformObject implements IProcess {

	private boolean terminated = false;
	private final ILaunch launch;
	private final String name;

	public ReloadProcess(ILaunch launch, String name) {
		this.launch = launch;
		this.name = name;
	}

	public void start() throws DebugException {
		try {
			Html5Plugin.getDefault().getReloadServer().startServer(this);
		} catch (CoreException e) {
			terminated = true;
			throw new DebugException(e.getStatus());
		}
	}

	@Override
	public boolean canTerminate() {
		return !isTerminated();
	}

	@Override
	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public void terminate() throws DebugException {
		try {
			Html5Plugin.getDefault().getReloadServer().stopServer(this);
		} catch (CoreException e) {
			throw new DebugException(e.getStatus());
		} finally {
			terminated = true;
		}
	}

	@Override
	public String getLabel() {
		return name;
	}

	@Override
	public ILaunch getLaunch() {
		return launch;
	}

	@Override
	public IStreamsProxy getStreamsProxy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(String key, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getAttribute(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getExitValue() throws DebugException {
		return 0;
	}

	public JSODDServer getReloadServer() {
		return Html5Plugin.getDefault().getReloadServer();
	}

}
