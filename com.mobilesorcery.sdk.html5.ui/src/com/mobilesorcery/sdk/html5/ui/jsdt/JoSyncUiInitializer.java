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
//package com.mobilesorcery.sdk.html5.ui.jsdt;
//
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.Path;
//import org.eclipse.jface.resource.ImageDescriptor;
//import org.eclipse.wst.jsdt.core.IJavaScriptProject;
//import org.eclipse.wst.jsdt.internal.ui.IJsGlobalScopeContainerInitializerExtension;
//
///**
// * @author Mattias Bybro
// *
// */
//public class JoSyncUiInitializer implements IJsGlobalScopeContainerInitializerExtension {
//	@Override
//	public ImageDescriptor getImage(IPath containerPath, String element, IJavaScriptProject project) {
//		if (containerPath == null) {
//			return null;
//		}
//		String requestedContainerPath = new Path(element).lastSegment();
//		if ((element != null) && requestedContainerPath.equals(new String(FireFoxLibInitializer.LIBRARY_FILE_NAMES[0]))) {
//			return null;
//		}
//
//		return Html5Ui.createFromFile(this.getClass(), "FireFoxSmall.gif");
//		// System.out.println("Unimplemented
//		// method:BasicBrowserLibraryJsGlobalScopeContainerInitializer.getImage");
//		// return null;
//	}
//
//}
