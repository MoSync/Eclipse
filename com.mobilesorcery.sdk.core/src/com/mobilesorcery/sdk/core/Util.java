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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.internal.ReverseComparator;

public class Util {

	private static final class ExtensionFileFilter implements FileFilter {
		private final String[] exts;

		private ExtensionFileFilter(String... ext) {
			this.exts = ext;
		}

		@Override
		public boolean accept(File pathname) {
			for (String ext : exts) {
				if (ext.equals(Util.getExtension(pathname))) {
					return true;
				}
			}
			return false;
		}
	}

	public static final char[] BASE16_CHARS = "0123456789ABCDEF".toCharArray();

	private static final int _1KB = 1024;
	private static final int _1MB = 1024 * 1024;
	private static final int _5KB = 5 * 1024;
	private static final int _5MB = 5 * 1024 * 1024;
	private static final int _1GB = 1024 * 1024 * 1024;

	private static final MessageFormat DATASIZE_FORMAT = new MessageFormat(
			"{0,number,#.0} {1}");

	private static final int MAX_DEPTH = 8;

	public static final int INFINITE_DEPTH = Short.MAX_VALUE;

	public static String join(String[] s, String delim) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < s.length; i++) {
			if (i > 0) {
				result.append(delim);
			}
			result.append(s[i]);
		}

		return result.toString();
	}

	public static String join(Object[] o, String delim) {
		if (o == null) {
			return "";
		}

		String[] toString = new String[o.length];
		for (int i = 0; i < toString.length; i++) {
			toString[i] = "" + o[i];
		}

		return join(toString, delim);
	}

	public static String join(String[] components, String delim, int start,
			int end) {
		String[] subarray = new String[end - start + 1];
		System.arraycopy(components, start, subarray, 0, subarray.length);
		return join(subarray, delim);
	}

	public static String[] ensureQuoted(Object[] obj) {
		String[] result = new String[obj.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = ensureQuoted(obj[i]);
		}

		return result;
	}

	public static String ensureQuoted(Object obj) {
		String str = obj == null ? "" : obj.toString();
		if (str.indexOf(' ') != -1 || str.indexOf('\t') != -1) {
			return '\"' + str + '\"';
		}

		return str;
	}

	public static String trimQuotes(Object str) {
		return trim(str, '\"');
	}

	static String trim(Object str, char ch) {
		if (str == null) {
			return null;
		}

		String result = str.toString();
		if (result.length() > 0 && result.charAt(0) == ch) {
			result = result.substring(1);
		}
		if (result.length() > 0 && result.charAt(result.length() - 1) == ch) {
			result = result.substring(0, result.length() - 1);
		}

		return result;
	}

	public static String fill(char c, int length) {
		char[] result = new char[length];
		Arrays.fill(result, c);
		return new String(result);
	}

	public static void unjar(File jar, File targetDir) throws IOException {
		unzip(jar, targetDir, true);
	}

	public static void unzip(File zip, File targetDir) throws IOException {
		unzip(zip, targetDir, false);
	}

	private static void unzip(File zip, File targetDir, boolean isJar) throws IOException {
		ZipInputStream input = createZipInputStream(new FileInputStream(zip), isJar);
		OutputStream currentOutput = null;
		targetDir.mkdirs();
		try {
			for (ZipEntry entry = input.getNextEntry(); entry != null; entry = input
					.getNextEntry()) {
				File currentFile = new File(targetDir, entry.getName());
				if (!entry.isDirectory()) {
					int readBytes = 0;
					byte[] buffer = new byte[512];
					currentFile.getParentFile().mkdirs();

					currentOutput = new FileOutputStream(currentFile);
					for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input
							.read(buffer, 0, buffer.length)) {
						currentOutput.write(buffer, 0, read);
						readBytes += read;
					}

					currentOutput.close();
				} else {
					currentFile.mkdirs();
				}

				currentFile.setLastModified(entry.getTime());
			}
		} finally {
			Util.safeClose(input);
			Util.safeClose(currentOutput);
		}

	}

	private static void zip(File sourceDir, File targetZip, boolean isJar) throws IOException {
		ZipOutputStream output = createZipOutputStream(new FileOutputStream(targetZip), isJar);
		try {
			for (File file : listFiles(sourceDir, true)) {
				if (!file.isDirectory()) {
					String relativePath = file.getAbsolutePath().substring(sourceDir.getAbsolutePath().length());
					ZipEntry entry = createZipEntry(relativePath, isJar);
					output.putNextEntry(entry);
					FileInputStream fileInput = new FileInputStream(file);
					try {
						transfer(fileInput, output);
					} finally {
						Util.safeClose(fileInput);
						output.closeEntry();
					}
				}
			}
		} finally {
			Util.safeClose(output);
		}
	}

	private static ZipOutputStream createZipOutputStream(OutputStream output, boolean isJar) throws IOException {
		return isJar ? new JarOutputStream(output) : new ZipOutputStream(output);
	}

	private static ZipInputStream createZipInputStream(InputStream input, boolean isJar) throws IOException {
		//return isJar ? new JarInputStream(input) : new ZipInputStream(input);
		// We usually want to unjar the manifest as well
		return new ZipInputStream(input);
	}

	private static ZipEntry createZipEntry(String relativePath, boolean isJar) {
		relativePath = relativePath.replace('\\', '/');
		if (relativePath.length() > 0 && relativePath.charAt(0) == '/') {
			relativePath = relativePath.substring(1);
		}
		return isJar ? new JarEntry(relativePath) : new ZipEntry(relativePath);
	}

	public static void jar(File sourceDir, File targetZip) throws IOException {
		zip(sourceDir, targetZip, true);
	}

	public static void zip(File sourceDir, File targetZip) throws IOException {
		zip(sourceDir, targetZip, false);
	}

	public static File[] listFiles(File directory, boolean recursive) {
		ArrayList<File> result = new ArrayList<File>();
		innerListFiles(directory, result, recursive);
		return result.toArray(new File[result.size()]);
	}

	private static void innerListFiles(File directory, ArrayList<File> result, boolean recursive) {
		File[] files = directory.listFiles();
		for (File file : files) {
			result.add(file);
			if (recursive && file.isDirectory()) {
				innerListFiles(file, result, true);
			}
		}
	}

	public static String[] parseCommandLine(String command) {
		ArrayList<String> result = new ArrayList<String>();
		StringBuffer current = new StringBuffer();
		char[] chars = command.toCharArray();
		boolean inQuote = false;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '\"') {
				inQuote = !inQuote;
			} else if (chars[i] == ' ') {
				if (!inQuote) {
					addIfNotEmpty(result, current);
					current = new StringBuffer();
				} else {
					current.append(chars[i]);
				}
			} else {
				current.append(chars[i]);
			}
		}

		addIfNotEmpty(result, current);

		return result.toArray(new String[result.size()]);
	}

	private static void addIfNotEmpty(ArrayList<String> result,
			StringBuffer current) {
		if (current.toString().trim().length() > 0) {
			result.add(current.toString());
		}
	}

	public static void mergeFiles(IProgressMonitor monitor, File[] src,
			File dest) throws IOException {
		dest.getParentFile().mkdirs();
		FileOutputStream output = new FileOutputStream(dest);
		try {
			mergeFiles(monitor, src, output);
		} finally {
			output.close();
		}
	}

	private static void mergeFiles(IProgressMonitor monitor, File[] src,
			OutputStream output) throws IOException {
		for (int i = 0; i < src.length; i++) {
			if (!src[i].exists()) {
				throw new FileNotFoundException(src[i].getAbsolutePath());
			}

			if (src[i].isDirectory()) {
				throw new IOException(src[i]
						+ " is a directory, cannot be copied");
			}

		}

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		monitor.beginTask("Copying...", src.length);

		for (int i = 0; i < src.length; i++) {
			monitor.setTaskName(MessageFormat.format("Copying {0}", src[i]
					.getName()));
			FileInputStream input = new FileInputStream(src[i]);
			try {
				transfer(input, output);
			} finally {
				input.close();
			}
			monitor.worked(1);
		}
	}

	public static void transfer(InputStream src, OutputStream dest) throws IOException {
		byte[] buffer = new byte[65536];
		for (int read = src.read(buffer); read != -1; read = src
				.read(buffer)) {
			dest.write(buffer, 0, read);
		}
	}

	public static void copy(IProgressMonitor monitor, File src, File dest, FileFilter filter) throws IOException {
		if (src.isDirectory()) {
			copyDir(monitor, src, dest, filter);
		} else {
			copyFile(monitor, src, dest);
		}
	}

	public static void copyFile(IProgressMonitor monitor, File src, File dest)
			throws IOException {
		if (dest.isDirectory()) {
			dest = new File(dest, src.getName());
		}
		mergeFiles(monitor, new File[] { src }, dest);
	}

	public static void copyDir(IProgressMonitor monitor, File srcDir,
			File destDir, FileFilter filter) throws IOException {
		copyDir(monitor, srcDir, destDir, filter, 0);
	}

	public static void copyDir(IProgressMonitor monitor, File srcDir,
			File destDir, FileFilter filter, int depth) throws IOException {
		if (depth > MAX_DEPTH) {
			return;
		}

		destDir.mkdirs();

		File[] files = srcDir.listFiles();
		monitor.beginTask("Recursive copy", IProgressMonitor.UNKNOWN);
		for (int i = 0; i < files.length; i++) {
			File src = new File(srcDir, files[i].getName());
			if (filter == null || filter.accept(src)) {
				File dest = new File(destDir, files[i].getName());
				if (files[i].isDirectory()) {
					copyDir(new NullProgressMonitor(), src, dest, filter,
							depth + 1);
				} else {
					copyFile(new NullProgressMonitor(), src, dest);
				}
			}
		}
	}

	public static byte[] fromBase16(String data) {
		boolean extraByte = data.length() % 2 == 1;
		byte[] result = new byte[data.length() / 2 + (extraByte ? 1 : 0)];

		for (int i = 0; i < result.length; i++) {
			int value = Integer.parseInt(data.substring(2 * i, Math.min(
					2 * i + 2, data.length())), 16);
			result[i] = (byte) (value & 0xff);
		}

		return result;
	}

	public static String toBase16(byte[] data) {
		return toBase16(data, 0, data.length);
	}

	public static String toBase16(byte[] data, int offset, int length) {
		char[] result = new char[length * 2];
		for (int i = 0; i < length; i++) {
			result[2 * i] = BASE16_CHARS[(data[offset + i] >> 4) & 0xf];
			result[2 * i + 1] = BASE16_CHARS[data[offset + i] & 0xf];
		}

		return new String(result);
	}

	/**
	 * Returns a simple string representation of data size, eg "23 bytes",
	 * "2 KB", "11 MB", etc
	 *
	 * @param size
	 * @return
	 */
	public static String dataSize(long size) {
		if (size < _5KB) {
			return size + " bytes";
		}

		String unit = "";
		float value = size;

		if (size > _1GB) {
			unit = "GB";
			value = value / _1GB;
		} else if (size > _5MB) {
			unit = "MB";
			value = value / _1MB;
		} else {
			unit = "KB";
			value = value / _1KB;
		}

		return DATASIZE_FORMAT.format(new Object[] { value, unit },
				new StringBuffer(30), new FieldPosition(0)).toString();
	}
	
	public static String elapsedTime(long ms) {
		String unit = "ms";
		long value = ms;
		long rem = 0;
		if (ms >= 3600000) {
			unit = "h";
			value = ms / 3600000;
			rem = ms % 3600000;
		} else if (ms >= 60000) {
			unit = "m";
			value = ms / 60000;
			rem = ms % 60000;
		} else if (ms >= 1000) {
			unit = "s";
			value = ms / 1000;
			rem = ms % 1000;
		}
		String minor = rem == 0 ? "" : " " + elapsedTime(rem);
		return value + unit + minor;
	}

	public static String getExtension(File file) {
		return getExtension(file.getName());
	}
	
	public static String getExtension(String filename) {
		if (filename == null) {
			return "";
		}
		int index = filename.lastIndexOf('.');
		if (index != -1) {
			return filename.substring(index + 1);
		} else {
			return "";
		}
	}

	public static String getNameWithoutExtension(File file) {
		return getNameWithoutExtension(file.getName());
	}
	
	public static String getNameWithoutExtension(String name) {
		if (name == null) {
			return "";
		}
		int index = name.lastIndexOf('.');
		if (index == -1) {
			return name;
		} else {
			return name.substring(0, index);
		}
	}

	/**
	 * Truncates a {@link String}, and if necessary adds an ellipsis to
	 * the end of the string.
	 * @param original
	 * @param ellipsis The string to use as an ellipsis, or {@code null} to use the default (...).
	 * @param maxLength If the length of the {@code original} {@link String} is greater than
	 * this value, an ellipsis will be added.
	 * @return A {@link String} that is guaranteed to be no longer than {@code maxLength}.
	 * @throws StringIndexOutOfBoundsException If the length of the ellipsis is greater
	 * than {@code maxLength}
	 */
	public static String truncate(String original, String ellipsis, int maxLength) {
		if (original.length() > maxLength) {
			if (ellipsis == null) {
				ellipsis = "...";
			}
			return original.substring(0, maxLength - ellipsis.length()) + ellipsis;
		}
		return original;
	}

	public static void writeToFile(File file, String text) throws IOException {
		FileWriter output = new FileWriter(file);
		try {
			output.write(text);
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}

	public static boolean deleteFiles(File file, FileFilter filter,
			int maxDepth, IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return false;
		}

		if (maxDepth < 0) {
			return true;
		}

		monitor.setTaskName(MessageFormat.format("Deleting {0}", file));

		if (file.isDirectory()) {
			boolean result = true;
			File[] filesToDelete = file.listFiles();
			for (int i = 0; i < filesToDelete.length; i++) {
				result &= deleteFiles(filesToDelete[i], filter, maxDepth - 1,
						monitor);
			}

			result &= file.delete();
			return result;
		} else {
			if (filter == null || filter.accept(file)) {
				return file.delete();
			} else {
				return true;
			}
		}
	}

	public static FileFilter getExtensionFilter(final String... ext) {
		FileFilter filter = new ExtensionFileFilter(ext);
		return filter;
	}

	public static String replaceExtension(String filename, String newExtension) {
		int where = filename.lastIndexOf('.');
		if (where != -1) {
			filename = filename.substring(0, where);
		}

		return filename + (isEmpty(newExtension) ? "" : ".") + newExtension;
	}

	public static void safeClose(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				// Ignore.
			}
		}
	}

	public static String readFile(String filename) throws IOException {
		return readFile(filename, null);
	}

	public static String readFile(String filename, String enc)
			throws IOException {
		if (filename == null) {
			return null;
		}

		File file = new File(filename);
		if (!file.exists()) {
			throw new IOException(MessageFormat.format(
					"File ''{0}'' does not exist", filename));
		}

		ByteArrayOutputStream result = new ByteArrayOutputStream();
		mergeFiles(new NullProgressMonitor(), new File[] { file }, result);
		return enc == null ? new String(result.toByteArray()) : new String(
				result.toByteArray(), enc);
	}

	public static int readInt(InputStream input) throws IOException {
		// LE or BE?
		byte[] intBuf = new byte[4];

		int totalRead = 0;
		for (int read = input.read(intBuf, totalRead, 4 - totalRead); read != 4
				&& totalRead < 4; read = input.read(intBuf, totalRead,
				4 - totalRead)) {
			if (read < 1) {
				throw new EOFException();
			}
			totalRead += read;
		}

		int result = (intBuf[3] & 0xff) << 24;
		result |= (intBuf[2] & 0xff) << 16;
		result |= (intBuf[1] & 0xff) << 8;
		result |= (intBuf[0] & 0xff);

		return result;
	}

	public static short readShort(InputStream input) throws IOException {
		byte[] shortBuf = new byte[2];

		int totalRead = 0;
		for (int read = input.read(shortBuf, totalRead, 2 - totalRead); read != 2
				&& totalRead < 2; read = input.read(shortBuf, totalRead,
				2 - totalRead)) {
			if (read < 1) {
				throw new EOFException();
			}
			totalRead += read;
		}

		short result = (short) ((shortBuf[1] & 0xff) << 8);
		result |= (shortBuf[0] & 0xff);

		return result;
	}

	public static File relativeTo(File peer, String filename) {
		File filenameFile = new File(filename);
		if (filenameFile.isAbsolute()) {
			return filenameFile;
		}

		File dir = peer.isDirectory() ? peer : peer.getParentFile();
		return new File(dir, filename).getAbsoluteFile();
	}

	public static boolean isParent(File potentialParent, File potentialChild) {
		if (potentialParent == null || potentialChild == null) {
			return false;
		}
		return potentialChild.getAbsolutePath().startsWith(potentialParent.getAbsolutePath());
	}

	public static boolean isEmpty(String text) {
		return text == null || text.isEmpty();
	}

	public static boolean isEmptyDirectory(File file) {
		return !file.isDirectory() || file.list().length == 0;
	}

	/**
	 * A utility method for handling <code>equals</code> of
	 * <code>null</code> objects.
	 * @param o1
	 * @param o2
	 */
	public static boolean equals(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}

		return o1.equals(o2);
	}

	/**
	 * A utility method for handling <code>compareTo</code> of
	 * <code>null</code> objects. <code>null</code> is always considered
	 * "less than" and will return <code>-1</code>
	 * @param o1
	 * @param o2
	 */
	public static int compare(Comparable c1, Comparable c2) {
		if (c1 == null) {
			return c2 == null ? 0 : -1;
		}
		if (c2 == null) {
			return +1;
		}

		return c1.compareTo(c2);
	}

	/**
	 * Replaces parameters tagged with <code>%</code>s and returns
	 * the resolved string.
	 * @param input
	 * @param map
	 * @return
	 */
    public static String replace(String input, Map<String, String> map) {
        try {
			return innerReplace(input, new DefaultParameterResolver(map), 0).toString();
		} catch (ParameterResolverException e) {
			// The default should never throw this exception
			throw new RuntimeException(e);
		}
    }

	/**
	 * Replaces parameters tagged with <code>%</code>s and returns
	 * the resolved string.
	 * @param input
	 * @param map
	 * @return
	 * @throws ParameterResolverException
	 */
    public static String replace(String input, ParameterResolver resolver) throws ParameterResolverException {
        return innerReplace(input, resolver, 0).toString();
    }

	public static String[] replace(String[] input, ParameterResolver resolver) throws ParameterResolverException {
		String[] output = new String[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = replace(input[i], resolver);
		}
		return output;
	}

    private static StringBuffer innerReplace(String input, ParameterResolver map, int depth) throws ParameterResolverException {
        if (depth > 12) {
            throw new IllegalArgumentException("Cyclic parameters"); //$NON-NLS-1$
        }

        StringBuffer result = new StringBuffer();
        char[] chars = input.toCharArray();
        boolean inParam = false;
        int paramStart = 0;

        for (int i = 0; i < chars.length; i++) {
            if ('%' == chars[i]) {
                if (!inParam) {
                    paramStart = i;
                } else {
                    String paramName = input.substring(paramStart + 1, i);
                    if (paramName.length() > 0 && paramName.charAt(0) == '#') {
                        // TODO.
                    } else {
                        if (paramName.length() == 0) { // Escape pattern %% => %
                            result.append("%"); //$NON-NLS-1$
                        } else {
                            String paramValue = map == null ? null : map.get(paramName);
                            if (paramValue != null) {
                                result.append(innerReplace(paramValue, map, depth + 1));
                            } else {
                                // Just leave as-is
                                result.append('%');
                                result.append(paramName);
                                result.append('%');
                            }
                        }
                    }
                }

                inParam = !inParam;
            } else if (!inParam) {
                result.append(chars[i]);
            }
        }

        return result;
    }

    /**
     * Creates a GET URL, given a baseURL and a map of parameters to send in the GET
     * @param service
     * @param params
     * @return A URL of the format <code>baseURL?param1=value1&param2=value2...</code>
     */
    public static String toGetUrl(String baseURL, Map<String, String> params) {
        StringBuffer paramsStr = new StringBuffer();
        if (baseURL == null) {
        	baseURL = "";
        }
        if (params != null && !params.isEmpty()) {
        	if (!Util.isEmpty(baseURL)) {
        		paramsStr.append("?"); //$NON-NLS-1$
        	}
            int paramCnt = 0;
            for (Map.Entry<String, String> param : params.entrySet()) {
                String key = param.getKey();
                String value = param.getValue();
                if (key != null && value != null) {
	            	paramCnt++;
	                if (paramCnt > 1) {
	                    paramsStr.append("&"); //$NON-NLS-1$
	                }

	                paramsStr.append(URLEncoder.encode(key) + "=" + URLEncoder.encode(value)); //$NON-NLS-1$
                }
            }
        }

        return baseURL + paramsStr;
    }

    public static <T> Comparator<T> reverseComparator(Comparator<T> original) {
        return new ReverseComparator<T>(original);
    }

    /**
     * Returns a 'parent' key of a key using path separators. So, if the input key is A/B/C, this
     * method returns A/B
     * @param key
     * @return <code>null</code> if <code>key</code> has no path separator
     */
    public static String getParentKey(String key) {
        Path permissionPath = new Path(key);
        return permissionPath.segmentCount() > 1 ? permissionPath.removeLastSegments(1).toPortableString() : null;
    }

	public static String convertSlashes(String str) {
		if (str == null) {
			return str;
		}
		return str.replace('\\', File.separatorChar).replace('/', File.separatorChar);
	}

	/**
	 * Given a string, returns a proper C identifier.
	 * @param name
	 * @return
	 */
	public static String toIdentifier(String str) {
		// C = Java identifiers :)
		StringBuffer buf = new StringBuffer();
		if (!str.isEmpty()) {
			char first = str.charAt(0);
			if (Character.isJavaIdentifierPart(first) && !Character.isJavaIdentifierStart(first)) {
				buf.append("_");
			}
			for (int i = 0; i < str.length(); i++) {
				char ch = str.charAt(i);
				if (Character.isJavaIdentifierPart(ch)) {
					buf.append(Character.toUpperCase(ch));
				} else {
					buf.append("_");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * Reverses a map into a {@link HashMap}.
	 * @param original The original map to reverse.
	 * @return A reverse map. If the original map contains several
	 * keys with the same value an {@link IllegalArgumentException}
	 * is thrown.
	 */
	public static <K, V> HashMap<V, K> reverseMap(Map<K, V> map) {
		HashMap<V, K> result = new HashMap<V, K>();
		for (Map.Entry<K, V> entry : map.entrySet()) {
			V key = entry.getValue();
			if (result.containsKey(key)) {
				throw new IllegalArgumentException("Key already present.");
			}
			result.put(key, entry.getKey());
		}
		return result;
	}

}
