package com.mobilesorcery.sdk.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.IProfileInfo;

public class ProfileInfoParser {

	public static IProfileInfo parse(IPath profileInfoPath,
			SLD sld, IProgressMonitor monitor) throws ParseException, IOException {
		File profileInfoFile = profileInfoPath.toFile();
		
		ProfileInfo result = new ProfileInfo();
		
        BufferedReader profileInfoReader = new BufferedReader(new FileReader(profileInfoFile));
        try {
            LineNumberReader profileInfoLines = new LineNumberReader(profileInfoReader);
            int currentLine = 0;
            for (String lineToParse = profileInfoLines.readLine(); lineToParse != null; lineToParse = profileInfoLines.readLine()) {
                lineToParse = lineToParse.trim();
                currentLine++;
                if (lineToParse.length() > 0) {
                	String[] addrAndCount = lineToParse.split(":", 2);
                	if (addrAndCount.length == 2) {
                		String addrStr = addrAndCount[0];
                		String countStr = addrAndCount[1];
                		int addr = SLDParser.parseInt(addrStr, 16, currentLine);
                		int count = SLDParser.parseInt(countStr, 10, currentLine);
                		
                		String file = sld.getFileName(addr);
                		int line = sld.getLine(addr);
                		
                		result.setCount(file, line, count);                		
                	}
                }
            }
        } finally {
            if (profileInfoFile != null) {
                profileInfoReader.close();
            }
        }

		return result;
	}
}
