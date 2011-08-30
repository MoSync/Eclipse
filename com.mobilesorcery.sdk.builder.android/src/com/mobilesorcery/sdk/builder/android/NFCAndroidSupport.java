package com.mobilesorcery.sdk.builder.android;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;

import com.mobilesorcery.sdk.core.DefaultPackager;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.apisupport.nfc.INFCEnablement;
import com.mobilesorcery.sdk.core.apisupport.nfc.NFCSupport;
import com.mobilesorcery.sdk.core.apisupport.nfc.TagTechNFCEnablement;

public class NFCAndroidSupport {

	private final static String NDEF_DISC_ACTION = "android.nfc.action.NDEF_DISCOVERED";
	private final static String TECH_DISC_ACTION = "android.nfc.action.TECH_DISCOVERED";
	private final static String TAG_DISC_ACTION = "android.nfc.action.TAG_DISCOVERED";
	private static final Object INTENT_FILTER_START_XML = "<intent-filter>";
	private static final Object INTENT_FILTER_END_XML = "</intent-filter>";
	private final NFCSupport nfcSupport;
	private final MoSyncProject project;

	public NFCAndroidSupport(NFCSupport nfc) {
		this.nfcSupport = nfc;
		this.project = nfc.getProject();
	}

	public String createIntentFilterXML() {
		StringBuffer result = new StringBuffer();

		for (INFCEnablement enablement : nfcSupport.getEnablements()) {
			if (enablement instanceof TagTechNFCEnablement) {
				result.append(INTENT_FILTER_START_XML);
				result.append(createActionXML(TECH_DISC_ACTION));
				result.append(INTENT_FILTER_END_XML);
				result.append("<meta-data android:name=\"" + TECH_DISC_ACTION +"\" " +
		                "android:resource=\"@xml/nfc\"/>");
			}
			result.append("\n");
		}

		return result.toString();
	}

	private String createActionXML(String action) {
		return "<action android:name=\"" + action + "\"/>";
	}

	public String createNFCFilterXML() throws CoreException {
		StringBuffer result = new StringBuffer();
		if (nfcSupport.getEnablements().size() > 0) {
			result.append("<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n");
			NFCSupport nfcSupport = NFCSupport.create(project);
			for (INFCEnablement enablement : nfcSupport.getEnablements()) {
				if (enablement instanceof TagTechNFCEnablement) {
					createNFCFilterXML(result, (TagTechNFCEnablement) enablement);
				}
				result.append("\n");
			}
		    result.append("</resources>");
		}
		return result.toString();
	}

	private void createNFCFilterXML(StringBuffer result, TagTechNFCEnablement enablement) {
		result.append("<tech-list>\n");
		for (String tech : enablement.getTechList()) {
	         result.append("<tech>");
	         result.append(toAndroidTech(tech));
	         result.append("</tech>");
		}
	    result.append("</tech-list>");
	}

	private String toAndroidTech(String tech) {
		// Simple for now :)
		return "android.nfc.tech." + tech;
	}

}
