///*  Copyright (C) 2011 Mobile Sorcery AB
//
//    This program is free software; you can redistribute it and/or modify it
//    under the terms of the Eclipse Public License v1.0.
//
//    This program is distributed in the hope that it will be useful, but WITHOUT
//    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
//    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
//    more details.
//
//    You should have received a copy of the Eclipse Public License v1.0 along
//    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
//*/
//package com.mobilesorcery.sdk.html5.libraries;
//
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.Path;
//import org.eclipse.wst.jsdt.core.IJavaScriptProject;
//import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainer;
//import org.eclipse.wst.jsdt.core.IJsGlobalScopeContainerInitializer;
//import org.eclipse.wst.jsdt.core.JsGlobalScopeContainerInitializer;
//import org.eclipse.wst.jsdt.core.compiler.libraries.LibraryLocation;
//import org.eclipse.wst.jsdt.core.compiler.libraries.SystemLibraryLocation;
//
//
//public class JoSyncLibInitializer extends JsGlobalScopeContainerInitializer implements IJsGlobalScopeContainerInitializer {
//	protected static final String CONTAINER_ID = "com.mobilesorcery.sdk.html5.library.josync";
//	protected static final String CONTAINER_DESC = "JoSync Library";
//	protected static final char[][] LIBRARY_FILE_NAMES = {
//		{'F', 'i', 'r', 'e', 'F', 'o', 'x', '2', '.', '0', '.', '0', '.', '3', '.', 'j', 's'},
//		{'X', 'M', 'L', 'H', 't', 't', 'p', 'R', 'e', 'q', 'u', 'e', 's', 't', '.', 'j', 's'}
//	};
//	protected static final String PLUGIN_ID = "org.eclipse.wst.jsdt.support.firefox";
//
//
//	static class FireFoxLibLocation extends SystemLibraryLocation {
//		FireFoxLibLocation() {
//			super();
//		}
//
//
//		@Override
//		public char[][] getLibraryFileNames() {
//			return new char[][]{JoSyncLibInitializer.LIBRARY_FILE_NAMES[0]};
//		}
//
//
//		@Override
//		protected String getPluginId() {
//			return JoSyncLibInitializer.PLUGIN_ID;
//		}
//
//		private static LibraryLocation fInstance;
//
//		public static LibraryLocation getInstance(){
//			if(fInstance== null){
//				fInstance = new FireFoxLibLocation();
//			}
//			return fInstance;
//		}
//	}
//
//	@Override
//	public LibraryLocation getLibraryLocation() {
//		return FireFoxLibLocation.getInstance();
//	}
//
//	@Override
//	public String getDescription(IPath containerPath, IJavaScriptProject project) {
//		return JoSyncLibInitializer.CONTAINER_DESC;
//	}
//
//	@Override
//	public String getDescription() {
//		return JoSyncLibInitializer.CONTAINER_DESC;
//	}
//
//
//	@Override
//	public IPath getPath() {
//		return new Path(JoSyncLibInitializer.CONTAINER_ID);
//	}
//
//	@Override
//	public int getKind() {
//		return IJsGlobalScopeContainer.K_APPLICATION;
//	}
//
//
//	@Override
//	public boolean canUpdateJsGlobalScopeContainer(IPath containerPath, IJavaScriptProject project) {
//		return true;
//	}
//
//	@Override
//	public String[] containerSuperTypes() {
//		return new String[]{ "josync" };
//	}
//
//}
