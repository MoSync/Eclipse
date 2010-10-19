package com.mobilesorcery.sdk.profiling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.mobilesorcery.sdk.core.ISLDInfo;
import com.mobilesorcery.sdk.core.ParseException;
import com.mobilesorcery.sdk.core.SLD;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile;
import com.mobilesorcery.sdk.core.SectionedPropertiesFile.Section.Entry;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiling.emulator.ProfilingSession;
import com.mobilesorcery.sdk.profiling.internal.ProfilingDataParser;

public class ProfilingSessionParser {

	public IProfilingSession parse(File input) throws IOException, ParseException {
        FileInputStream inputStream = new FileInputStream(input);
        try {
            return parse(inputStream);
        } finally {
            Util.safeClose(inputStream);
        }
    }
    
    public IProfilingSession parse(InputStream input) throws IOException, ParseException {
    	ZipInputStream zipInput = new ZipInputStream(input);
    	byte[] profilingDataBuffer = null;
    	ISLDInfo sld = null;
    	SectionedPropertiesFile properties = null;
    	for (ZipEntry entry = zipInput.getNextEntry(); entry != null; entry = zipInput.getNextEntry()) {
    		String entryName = entry.getName();
    		if (entryName.endsWith("fp.xml")) {
    			profilingDataBuffer = readEntireStream(zipInput);
    		} else if (entryName.endsWith("sld.tab")) {
    			sld = SLD.parseSLDInfo(zipInput, null);
    		} else if (entryName.endsWith("manifest.mf")) {
    			properties = parseMetaData(zipInput);
    		}
    		
    		zipInput.closeEntry();
    	}
    	
    	if (sld == null || profilingDataBuffer == null || properties == null) {
    		throw new IOException("Invalid profile session data -- no SLD/profiling data/metadata");
    	}
    	
    	IInvocation invocation = parseProfilingData(new ByteArrayInputStream(profilingDataBuffer), sld);
    	
    	return constructSession(properties, invocation);
    }

    public void unparse(IProfilingSession session, File output) throws IOException {
    	OutputStream writer = new FileOutputStream(output);
    	try {
    		unparse(session, writer);
    	} finally {
    		Util.safeClose(writer);
    	}
    }


	public void unparse(List<IProfilingSession> sessions, File output) throws IOException {
		if (sessions.size() != 1) {
			throw new IOException("Current only supports saving ONE session");
		}
		
		unparse(sessions.get(0), output);
	}

    public void unparse(IProfilingSession session, OutputStream output) throws IOException {
    	SLD sld = (SLD) session.getAdapter(SLD.class);
    	ISLDInfo info = sld == null ? null : sld.parseSLD();
    	if (info == null) {
    		throw new IOException("No SLD information available -- cannot save session");
    	}
    	
    	File fpFile = session.getProfilingFile();
    	if (fpFile == null || !fpFile.exists()) {
    		throw new IOException("No profiling information available -- cannot save session");
    	}
    	
    	ZipOutputStream zipOutput = new ZipOutputStream(output);
    	ZipEntry sldEntry = new ZipEntry("/sld.tab");
    	zipOutput.putNextEntry(sldEntry);

    	File sldFile = info.getSLDFile();
    	dumpEntireFile(sldFile, zipOutput);
    	
    	ZipEntry fpEntry = new ZipEntry("/fp.xml");
    	zipOutput.putNextEntry(fpEntry);
    	dumpEntireFile(fpFile, zipOutput);
    	
    	SectionedPropertiesFile properties = SectionedPropertiesFile.create();
    	properties.getDefaultSection().addEntry(new Entry("name", session.getName()));
    	properties.getDefaultSection().addEntry(new Entry("startTime", Long.toString(session.getStartTime().getTimeInMillis())));
    	ZipEntry propsEntry = new ZipEntry("manifest.mf");
    	zipOutput.putNextEntry(propsEntry);
    	zipOutput.write(properties.toString().getBytes("UTF-8"));
    	zipOutput.closeEntry();
    	
    	zipOutput.close();
    }
    
	private void dumpEntireFile(File file, ZipOutputStream zipOutput) throws IOException {
    	FileInputStream input = new FileInputStream(file);
    	try {
    		zipOutput.write(readEntireStream(input));
    		zipOutput.closeEntry();
    	} finally {
    		Util.safeClose(input);
    	}
	}

	private ProfilingSession constructSession(SectionedPropertiesFile properties, IInvocation invocation) {
    	Map<String, String> profilingProps = properties.getDefaultSection().getEntriesAsMap();
    	String name = profilingProps.get("name");
    	String startTimeStr = profilingProps.get("startTime");
    	Calendar startTime = Calendar.getInstance();
    	if (startTimeStr != null) {
    		try {
    			startTime.setTimeInMillis(Long.parseLong(startTimeStr));
    		} catch (NumberFormatException e) {
    			// Just ignore.
    		}
    	}
    	
    	ProfilingSession session = new ProfilingSession(name, startTime);
    	session.setInvocation(invocation);
    	return session;
	}

	private SectionedPropertiesFile parseMetaData(InputStream input) throws IOException {
		SectionedPropertiesFile properties = SectionedPropertiesFile.parse(new InputStreamReader(input));
		return properties;
	}

	private byte[] readEntireStream(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[65536];
		for (int read = input.read(buffer); read != -1; read = input.read(buffer)) {
			output.write(buffer, 0, read);
		}
		return output.toByteArray();
	}

	private IInvocation parseProfilingData(InputStream input, ISLDInfo sld) throws IOException, ParseException {
		ProfilingDataParser parser = new ProfilingDataParser();
		return parser.parse(input, sld);
	}
}
