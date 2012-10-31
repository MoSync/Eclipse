package com.mobilesorcery.sdk.core.build;

import com.mobilesorcery.sdk.core.Pair;

/**
 * An interface for allowing plugins to define their own {@code IBuildStepFactory}s
 * @author mattias.bybro@mosync.com
 *
 */
public interface IBuildStepFactoryExtension {

	enum Position { NONE, BEFORE, AFTER };
	
	public static final String EXTENSION_ID = "com.mobilesorcery.sdk.build.steps";

	/**
	 * Creates a new factory.
	 * @return
	 */
	IBuildStepFactory createFactory();
	
	/**
	 * Returns where the default factory should be inserted;
	 * either {@link Position#BEFORE} or {@link Position#AFTER}
	 * the factory represented by the returned {@code String}.
	 * @return {@link Position#NONE} if this factory
	 * has no default position. Returning {@code null} as the
	 * second argument indicates either 'first' position
	 * (if the position is {@link Position#BEFORE}) or
	 * 'last' position (if the position is {@link Position#AFTER}).
	 */
	Pair<Position, String> getDefaultPosition();
}
