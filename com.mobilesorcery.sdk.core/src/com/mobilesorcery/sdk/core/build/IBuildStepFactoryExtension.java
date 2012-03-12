package com.mobilesorcery.sdk.core.build;

/**
 * An interface for allowing plugins to define their own {@code IBuildStepFactory}s
 * @author mattias.bybro@mosync.com
 *
 */
public interface IBuildStepFactoryExtension {

	public static final String EXTENSION_ID = "com.mobilesorcery.sdk.build.steps";

	IBuildStepFactory createFactory();
}
