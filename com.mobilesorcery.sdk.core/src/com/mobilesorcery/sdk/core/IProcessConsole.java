package com.mobilesorcery.sdk.core;

import com.mobilesorcery.sdk.core.LineReader.ILineHandler;

/**
 * <p>A presentation-agnostic console interface.</p>
 * <p>Clients may choose to handle calls to this interface
 * any way they wish - and callers to this interface should
 * not assume any specific behaviour.</p>
 * 
 * @author Mattias Bybro, mattias.bybro@purplescout.se
 *
 */
public interface IProcessConsole {

	/**
	 * Attaches this process console to a running process,
	 * and outputs the process' input, output, and error streams
	 * @param process
	 */
	public void attachProcess(Process process);

	/**
	 * Attaches this process console to a running process,
	 * and outputs the process' input, output, and error streams.
	 * A line handler takes care of 'intelligently' handling
	 * both output/error streams.
	 * @param process
	 * @param delegate
	 */
	public void attachProcess(Process process, final ILineHandler delegate);

	/**
	 * Attaches this process console to a running process,
	 * and outputs the process' input, output, and error streams.
	 * Two line handlers take care of 'intelligently' handling
	 * both the output and error streams.
	 * @param process
	 * @param delegate
	 */
	public void attachProcess(Process process,
			final ILineHandler stdoutDelegate, final ILineHandler stderrDelegate);

	/**
	 * Adds a message to the console, intended for
	 * the user to see.
	 * @param line
	 */
	public void addMessage(String line);

	/**
	 * A hint to clear the console. Subclasses are allowed to ignore this instruction.
	 */
	public void clear();

    /**
     * <p>This method is called to indicate a
     * new session, execution, whatever -- the
     * implementation determines what actions need
     * to be taken (like clearing the console window)</p>
     * <p>The default implementation does nothing.</p>
     */
	public void prepare();

}