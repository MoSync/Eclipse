//package com.mobilesorcery.sdk.html5.live;
//
//import java.util.HashSet;
//
//import org.eclipse.core.resources.IProject;
//import org.eclipse.core.resources.IResource;
//import org.eclipse.core.resources.IResourceChangeEvent;
//import org.eclipse.core.resources.IResourceChangeListener;
//import org.eclipse.core.resources.IResourceDelta;
//import org.eclipse.core.resources.IResourceDeltaVisitor;
//import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.core.runtime.CoreException;
//
//public class EmulatorLiveSession implements IResourceChangeListener {
//
//	private final LiveServer server;
//
//	public EmulatorLiveSession() {
//		server = new LiveServer();
//	}
//
//	public void start() throws Exception {
//		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
//		server.startServer();
//	}
//
//	public void stop() throws Exception {
//		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
//		server.stopServer();
//	}
//
//	@Override
//	public void resourceChanged(IResourceChangeEvent event) {
//		IResourceDelta delta = event.getDelta();
//		final HashSet<IProject> projects = new HashSet<IProject>();
//		IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
//			@Override
//			public boolean visit(IResourceDelta delta) throws CoreException {
//				IResource resource = delta.getResource();
//				if (resource != null) {
//					projects.add(resource.getProject());
//				}
//				return true;
//			}
//		};
//
//		//delta.accept(visitor);
//		server.setDirty(projects);
//	}
//
//	public void registerClient(LiveClient client) {
//
//	}
//
//	public void unregisterClient(LiveClient client) {
//
//	}
//
//}
