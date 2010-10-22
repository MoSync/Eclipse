package com.mobilesorcery.sdk.core;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

public interface ISLDInfo {

    public static final int UNKNOWN_LINE = -1;

    public abstract String getFileName(int addr);

    public abstract int getLine(int addr);

    public abstract void write(Writer writer) throws IOException;

    public abstract File getSLDFile();

    public String getFunction(int addr);

    public Collection<String> getAllFilenames();
    
    public Collection<String> getAllFunctions();
   
}