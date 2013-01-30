package com.mobilesorcery.sdk.builder.iphoneos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.core.AbstractTool;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.LineReader.ILineHandler;
import com.mobilesorcery.sdk.core.LineReader.LineAdapter;

public class XCodeSelect extends AbstractTool {

	private static XCodeSelect instance = new XCodeSelect();

	public static XCodeSelect getInstance() {
		return instance;
	}
	
	private XCodeSelect() {
		super(null);
	}

	@Override
	protected String getToolName() {
		return "xcode-select";
	}
		
	public String getCurrentXCodePath() throws CoreException {
		CollectingLineHandler handler = new CollectingLineHandler();
		ArrayList<String> commandLine = new ArrayList<String>();
		commandLine.add("xcode-select");
		commandLine.add("--print-path");
		execute(commandLine.toArray(new String[0]), handler, handler, false);
		try {
			handler.awaitStopped(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return handler.getFirstLine();
	}

}
