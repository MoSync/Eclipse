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
package com.mobilesorcery.sdk.fontsupport;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.RGB;

import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.fontsupport.internal.wizard.BMFontInfo;
import com.mobilesorcery.sdk.fontsupport.internal.wizard.BMFontInfoBlock;
import com.mobilesorcery.sdk.fontsupport.internal.wizard.BinaryBMFontParser;

/**
 * A Java representation of the mof tool
 * @author Mattias Bybro
 *
 */
public class MOF {

	public static final String MOF_FILE_EXTENSION = "mof"; //$NON-NLS-1$
	public static final RGB DEFAULT_COLOR = new RGB(0xff, 0xff, 0xff);
	
	public static void generate(IProgressMonitor monitor, File fontFile, File outputFile, RGB outputColor) throws IOException {
		BinaryBMFontParser parser = new BinaryBMFontParser();
		BMFontInfo info = parser.parse(fontFile);
		BMFontInfoBlock pages = info.getFirst(BinaryBMFontParser.PAGES_TYPE);
		String pageName = null;
		if (pages != null) {
			pageName = pages.getString(BMFontInfoBlock.PAGE_NAME);
		}
		
		if (pageName == null) {
			throw new IOException(Messages.MOF_NoImageFile);
		}
		
		File pageNameFile = Util.relativeTo(fontFile, pageName);
		
		if (!pageNameFile.exists()) {
			throw new IOException(MessageFormat.format(Messages.MOF_FontImageMissing, pageName));
		}
		
		if (!isImageFormatSupported(Util.getExtension(pageNameFile))) {
			throw new IOException(Messages.MOF_InvalidImageFormat);
		}
		
		IPath mofExe = MoSyncTool.getDefault().getBinary("mof"); //$NON-NLS-1$
		CommandLineExecutor executor = new CommandLineExecutor("MOF"); //$NON-NLS-1$
		executor.addCommandLine(new String[] {
			mofExe.toFile().getAbsolutePath(),
			"-fontData", //$NON-NLS-1$
			fontFile.getAbsolutePath(),
			"-fontImage", //$NON-NLS-1$
			pageNameFile.getAbsolutePath(),
			"-outFile", //$NON-NLS-1$
			outputFile.getAbsolutePath(),
			"-fontColor", //$NON-NLS-1$
			rgbToHex(outputColor)
		});
		
		executor.execute();
	}

	public static boolean isImageFormatSupported(String fileExtension) {
		return "png".equalsIgnoreCase(fileExtension); //$NON-NLS-1$
	}
	
	private static String rgbToHex(RGB rgb) {
		char[] result = new char[6];
		
		if (rgb == null) {
			rgb = new RGB(0, 0, 0);
		}
		
		result[0] = Util.BASE16_CHARS[(rgb.red >> 4) & 0xF];
		result[1] = Util.BASE16_CHARS[rgb.red & 0xF];
		result[2] = Util.BASE16_CHARS[(rgb.green >> 4) & 0xF];
		result[3] = Util.BASE16_CHARS[rgb.green & 0xF];
		result[4] = Util.BASE16_CHARS[(rgb.blue >> 4) & 0xF];
		result[5] = Util.BASE16_CHARS[rgb.blue & 0xF];
		
		return new String(result);
	}
}
