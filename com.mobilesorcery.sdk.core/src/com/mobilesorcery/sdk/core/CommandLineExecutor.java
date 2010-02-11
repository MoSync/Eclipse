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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;

import com.mobilesorcery.sdk.core.LineReader.ILineHandler;



public class CommandLineExecutor {

	private static final int MAX_DEPTH = 100;
	private ArrayList<String[]> lines = new ArrayList<String[]>();
	private CascadingProperties parameters;
	private String dir;
	private Process currentProcess;
	private boolean killed = false;
	private String consoleName;
	private ILineHandler stdoutHandler;
	private ILineHandler stderrHandler;

	public CommandLineExecutor(String consoleName) {
		this.consoleName = consoleName;
	}

	/**
	 * Adds a <emph>parameterized</emph> command line
	 * 
	 * @param line
	 */
	public void addCommandLine(String[] line) {
		lines.add(line);
	}

	/**
	 * Convenience method for running exactly one line.
	 * 
	 * @param commandLine
	 * @throws IOException
	 */
	public void runCommandLine(String[] commandLine) throws IOException {
		lines.clear();
		lines.add(commandLine);
		execute();
		lines.clear();
	}
	
	/**
	 * Convenience method for running exactly one line.
	 * 
	 * @param commandLine
	 * @return Exit code of the process
	 * @throws IOException
	 */
	public int runCommandLineWithRes ( String[] commandLine ) 
	throws IOException 
	{
		int res;
		lines.clear();
		lines.add(commandLine);
		res = execute();
		lines.clear();
		
		return res;
	}	

	public void setParameters(CascadingProperties parameters) {
		this.parameters = parameters;
	}

	public void setExecutionDirectory(String dir) {
		this.dir = dir;
	}

	public int execute() throws IOException {
		IProcessConsole console = createConsole();

		for (int i = 0; !killed && i < lines.size(); i++) {
			String[] line = lines.get(i);
			String[] resolvedLine = new String[line.length];
			for (int j = 0; j < resolvedLine.length; j++) {
				resolvedLine[j] = replace(line[j], parameters);
			}

			String mergedCommandLine = mergeCommandLine(resolvedLine);
			console.addMessage(mergedCommandLine);
			if (dir == null) {
			    currentProcess = Runtime.getRuntime().exec(mergedCommandLine);
			} else {
			    currentProcess = Runtime.getRuntime().exec(mergedCommandLine, null, new File(dir));
			}
			
			console.attachProcess(currentProcess, stdoutHandler, stderrHandler);
			try {
				return currentProcess.waitFor();
			} catch (InterruptedException e) {
				throw new IOException("Process interrupted.");
			}
		}
		
		return 0;
	}

	private String mergeCommandLine(String[] commandLine) {
		StringBuffer correctCommandLine = new StringBuffer();
		for (int i = 0; i < commandLine.length; i++) {
			if (i > 0) {
				correctCommandLine.append(" ");
			}
			correctCommandLine.append(assertQuoted(commandLine[i]));
		}
		return correctCommandLine.toString();
	}

	private String assertQuoted(String str) {
		if (str.indexOf(' ') != -1 || str.indexOf('\t') != -1) {
			return "\"" + str + "\"";
		}

		return str;
	}

	public void kill() {
		if (currentProcess == null) {
			currentProcess.destroy();
		}
	}

	public static String replace(String originalString, CascadingProperties parameters) {
		int tries = 0;
		String last = "";
		String result = originalString;
		while (!result.equals(last)) {
			tries++;
			last = result;
			result = replaceOne(result, parameters);
			if (tries > MAX_DEPTH) {
				return originalString; // Circular dependencies, but we want no exception thrown.
			}
		}
		
		return result;
	}
	
	/**
	 * Replaces and string in %'s with a parameter in <code>parameters</code>.
	 * 
	 * @param originalString
	 * @param parameters
	 * @return
	 */
	public static String replaceOne(String originalString, CascadingProperties parameters) {
		if (parameters == null) {
			return originalString;
		}

		String result = originalString;
		for (Iterator<String> parameterKeys = parameters.keySet().iterator(); parameterKeys.hasNext();) {
			String parameterKey = parameterKeys.next();
			result = result.replaceAll("%" + parameterKey + "%", Matcher.quoteReplacement(parameters.get(parameterKey)));
		}

		return result;
	}

	public IProcessConsole createConsole() {
		return CoreMoSyncPlugin.getDefault().createConsole(consoleName);
	}

	public void setLineHandlers(ILineHandler stdoutHandler,
			ILineHandler stderrHandler) {
		this.stdoutHandler = stdoutHandler;
		this.stderrHandler = stderrHandler;
	}

}
