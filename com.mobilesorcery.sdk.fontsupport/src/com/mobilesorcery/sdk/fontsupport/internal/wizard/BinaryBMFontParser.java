package com.mobilesorcery.sdk.fontsupport.internal.wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import com.mobilesorcery.sdk.core.Util;

public class BinaryBMFontParser {

	public final static int INFO_TYPE = 1;
	public final static int COMMON_TYPE = 2;
	public final static int PAGES_TYPE = 3;
	public final static int CHARS_TYPE = 4;
	public final static int KERNING_TYPE = 5;
	
	private static final int KERNING_PAIR_SIZE = 6;

	public BMFontInfo parse(File bmFontFile) throws IOException {
		FileInputStream input = new FileInputStream(bmFontFile);
		try {
			return parse(input);
		} finally {
			input.close();
		}
	}

	private BMFontInfo parse(InputStream input) throws IOException {
		BMFontInfo result = new BMFontInfo();
		
		byte[] magic = new byte[4];
		if (input.read(magic) != 4) {
			throw new IOException(Messages.BinaryBMFontParser_0 +
			Messages.BinaryBMFontParser_1);
		}
		
		if (magic[0] != 'B' || magic[1] != 'M' || magic[2] != 'F') {
			throw new IOException(Messages.BinaryBMFontParser_2 +
			Messages.BinaryBMFontParser_3);
		}
		
		int version = magic[3];
		result.setVersion(version);
	
		for (BMFontInfoBlock block = parseBlock(result, input); block != null; block = parseBlock(result, input)) {
			result.addBlock(block);
		}
		
		return result;
	}
	
	public final static void main(String[] args) throws Exception {
		BinaryBMFontParser parser = new BinaryBMFontParser();
		BMFontInfo info = parser.parse(new File(Messages.BinaryBMFontParser_4));
		System.err.println(info.getFirst(BinaryBMFontParser.PAGES_TYPE));
	}

	private BMFontInfoBlock parseBlock(BMFontInfo info, InputStream input) throws IOException {
		int type = input.read();
		
		if (type == -1) {
			return null;
		}
		
		int blockSize = Util.readInt(input);

		BMFontInfoBlock block = new BMFontInfoBlock(type);
		switch (type) {
		case INFO_TYPE:
		case COMMON_TYPE:
		case CHARS_TYPE:
			input.skip(blockSize - 4);
			break;
		case PAGES_TYPE:
			parsePagesBlock(block, input);
			break;
		case KERNING_TYPE:
			parseKerningBlock(info, block, blockSize, input);
			break;
		default:
			throw new IOException(MessageFormat.format(Messages.BinaryBMFontParser_5, type));	
		}
		
		return block;
	}

	private void parseKerningBlock(BMFontInfo info, BMFontInfoBlock block, int blockSize, InputStream input) throws IOException {		
		int kerningPairs = blockSize / KERNING_PAIR_SIZE;		
		for (int i = 0; i < kerningPairs; i++) {
			int first = Util.readShort(input) & 0xffff;
			int second = Util.readShort(input) & 0xffff;
			int amount = Util.readShort(input);
			info.setKerning(first, second, amount);
		}
	}

	private void parsePagesBlock(BMFontInfoBlock block, InputStream input) throws IOException {
		String pageName = readZS(input);
		block.setString(BMFontInfoBlock.PAGE_NAME, pageName);
	}

	private String readZS(InputStream input) throws IOException {
		StringBuffer pageName = new StringBuffer();
		for (int read = input.read(); read != 0; read = input.read()) {
			if (read == -1) {
				throw new IOException(Messages.BinaryBMFontParser_6);
			}
			
			pageName.append((char) read);
		}
		
		return pageName.toString();
	}

}
