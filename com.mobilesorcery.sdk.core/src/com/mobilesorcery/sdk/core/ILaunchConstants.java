/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core;

public interface ILaunchConstants {

	/**
	 * The launch property representing the project's name
	 */
    String PROJECT = "project";

	/**
	 * The screen height launch property
	 */
    String SCREEN_SIZE_HEIGHT = "screen.height";

	/**
	 * The screen width launch property
	 */
    String SCREEN_SIZE_WIDTH = "screen.width";

    String SCREEN_SIZE_OF_TARGET = "screen.use.target";

	/**
	 * The launch property deciding whether to automatically change
	 * the build configuration before launch
	 */
	String AUTO_CHANGE_CONFIG = "auto.change.config.2";

	/**
	 * The launch property deciding which build configuration to automatically change
	 * to before launch
	 */
	String BUILD_CONFIG = "build.config";

	/**
	 * The launch property deciding whether to automatically change
	 * the build configuration before a <i>debug</i> launch
	 */
	String AUTO_CHANGE_CONFIG_DEBUG = AUTO_CHANGE_CONFIG + ".debug.2";

	/**
	 * The launch property deciding which build configuration to automatically change
	 * to before a <i>debug</i> launch
	 */
	String BUILD_CONFIG_DEBUG= BUILD_CONFIG + ".debug";

	/**
	 * The id of the launch delegate that should launch the app
	 */
	String LAUNCH_DELEGATE_ID = "launch.delegate";

	/**
	 * An attribute indicating the launch should take place on an actual device.
	 */
	String ON_DEVICE = "on.device";

}
