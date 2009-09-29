package com.mobilesorcery.sdk.builder.s60;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.mobilesorcery.sdk.core.AbstractPackager;

public abstract class S60Packager extends AbstractPackager {

    static byte[] readFile(File file) throws IOException {
        DataInputStream input = null;
        byte[] buffer = new byte[(int) file.length()];
        
        try {
            input = new DataInputStream(new FileInputStream(file));
            input.readFully(buffer);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        
        return buffer;
    }
    
    static void writeFile(File file, byte[] buffer) throws IOException {
        FileOutputStream output = new FileOutputStream(file);        
        try {
            output.write(buffer);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    
    static int readInt(byte[] buffer, int offset) {
        return (buffer[offset] & 0xff) |
        ((buffer[offset + 1] & 0xff) << 8) |
        ((buffer[offset + 2] & 0xff) << 16) |
        ((buffer[offset + 3] & 0xff) << 24);
    }
    
    static void writeInt(int value, byte[] buffer, int offset) {
        buffer[offset + 3] = (byte)((value >> 24) & 0xff);
        buffer[offset + 2] = (byte)((value >> 16) & 0xff);
        buffer[offset + 1] = (byte)((value >> 8) & 0xff);
        buffer[offset] = (byte)(value & 0xff);
    }
}
