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
package com.mobilesorcery.sdk.internal;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.mobilesorcery.sdk.core.ISLDInfo;

public class SLDInfoImpl implements ISLDInfo {

    private TreeMap<Integer, String> fileTable = new TreeMap<Integer, String>();
    private TreeMap<Integer, Integer> addrToFile = new TreeMap<Integer, Integer>();
    private TreeMap<Integer, Integer> addrToLine = new TreeMap<Integer, Integer>();
    public TreeMap<AddressRange, String> startAddrForFunc = new TreeMap<AddressRange, String>(AddressRange.START_COMPARATOR);
    private File file;

    SLDInfoImpl(File sldFile) {
        this.file = sldFile;
    }

    void addFile(int id, String name) {
        fileTable.put(id, name);
    }

    void addLocationForAddress(int addr, int lineInFile, int fileId) {
        addrToLine.put(addr, lineInFile);
        addrToFile.put(addr, fileId);
    }


    void addRangeForFunction(String functionName, AddressRange addrRange) {
        if (addrRange != null) {
            startAddrForFunc.put(addrRange, functionName);
        }
    }
    
    /* (non-Javadoc)
     * @see com.mobilesorcery.sdk.core.ISLDInfo#getFileName(int)
     */
    public String getFileName(int addr) {
        if (addrToFile.isEmpty()) {
            return null;
        }

        Entry<Integer, Integer> entry = addrToFile.floorEntry(addr);
        if (entry != null) {
            Integer fileId = entry.getValue();
            if (fileId != null) {
                return fileTable.get(fileId);
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see com.mobilesorcery.sdk.core.ISLDInfo#getLine(int)
     */
    public int getLine(int addr) {
        if (addrToLine.isEmpty()) {
            return UNKNOWN_LINE;
        }

        Entry<Integer, Integer> entry = addrToLine.floorEntry(addr - 1);
        if (entry != null) {
            Integer line = entry.getValue();
            if (line != null) {
                return line;
            }
        }

        return UNKNOWN_LINE;
    }

    public String getFunction(int addr) {
        AddressRange key = new AddressRange(addr, addr);
        Entry<AddressRange, String> closestEntry = startAddrForFunc.floorEntry(key);
        AddressRange closestAddressRange = closestEntry.getKey();
        return closestAddressRange != null && closestAddressRange.inRange(addr) ? closestEntry.getValue() : null;
    }
    
    /* (non-Javadoc)
     * @see com.mobilesorcery.sdk.core.ISLDInfo#write(java.io.Writer)
     */
    public void write(Writer writer) throws IOException {
        writer.write(SLDParser.FILE_MARKER);
        writer.write('\n');
        for (Iterator<Integer> files = fileTable.keySet().iterator(); files.hasNext();) {
            Integer fileId = files.next();
            String filename = fileTable.get(fileId);
            writer.write(fileId + ":" + filename);
            writer.write('\n');
        }

        writer.write(SLDParser.SLD_MARKER);
        writer.write('\n');

        for (Iterator<Integer> addrs = addrToFile.keySet().iterator(); addrs.hasNext();) {
            Integer addr = addrs.next();
            int line = addrToLine.get(addr);
            int file = addrToFile.get(addr);

            writer.write(Integer.toHexString(addr) + ":" + line + ":" + file);
            writer.write("\n");
        }
    }

    /* (non-Javadoc)
     * @see com.mobilesorcery.sdk.core.ISLDInfo#getSLDFile()
     */
    public File getSLDFile() {
        return file;
    }

	public Collection<String> getAllFilenames() {
		return Collections.unmodifiableCollection(fileTable.values());
	}

	public Collection<String> getAllFunctions() {
		return Collections.unmodifiableCollection(startAddrForFunc.values());	
	}

 
}
