package com.mobilesorcery.sdk.core.apisupport.nfc;

import java.io.File;
import java.io.FileReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;

/**
 * A class for managing a {@link MoSyncProject}'s NFC specific information.
 * TODO: Should this be in the core plugin?
 * @author mattias
 *
 */
public class NFCSupport {

	public final static String NFC_A_TECH = "NfcA";
	public final static String NFC_B_TECH = "NfcB";
	public final static String NFC_F_TECH = "NfcF";
	public final static String NFC_V_TECH = "NfcV";
	public final static String NDEF_TECH = "Ndef";
	public final static String ISO_DEP_TECH = "IsoDep";
	public final static String MIFARE_CLASSIC_TECH = "MifareClassic";
	public final static String MIFARE_ULTRALIGHT_TECH = "MifareUltralight";

	private final static HashSet<String> AVAILABLE_TECHS = new HashSet<String>(Arrays.asList(new String[] {
		NFC_A_TECH, NFC_B_TECH, NFC_F_TECH, NFC_V_TECH, NDEF_TECH, ISO_DEP_TECH,
		MIFARE_CLASSIC_TECH, MIFARE_ULTRALIGHT_TECH
	}));

	private final List<INFCEnablement> enablements = new ArrayList<INFCEnablement>();
	private MoSyncProject project;

	public static NFCSupport create(MoSyncProject project) throws CoreException {
		NFCSupport result = new NFCSupport();
		result.project = project;
		result.parseEnablements();
		return result;
	}

	private void parseEnablements() throws CoreException {
		File nfcInfoLoc = getNFCDescription();
		if (nfcInfoLoc.exists()) {
			parseEnablements(nfcInfoLoc);
		}
	}

	public File getNFCDescription() {
		return project.getWrappedProject().getLocation().append("nfc.xml").toFile();
	}

	private void parseEnablements(File nfcInfoLoc) throws CoreException {
		FileReader nfcReader = null;
		try {
			nfcReader = new FileReader(nfcInfoLoc);
			XMLMemento root = XMLMemento.createReadRoot(nfcReader);
			IMemento[] techListMementoes = root.getChildren("tech-list");
			for (IMemento techListMemento : techListMementoes) {
				ArrayList<String> currentTechs = new ArrayList<String>();
				IMemento[] techMementoes = techListMemento.getChildren("tech");
				for (IMemento techMemento : techMementoes) {
					String techName = techMemento.getString("name");
					if (!AVAILABLE_TECHS.contains(techName)) {
						throw new IllegalArgumentException(
								MessageFormat.format(
										"<tech> tags must have a name attribute with one of these tecnologies: {0}", AVAILABLE_TECHS));
					}
					currentTechs.add(techName);
				}

				if (currentTechs.size() > 0) {
					enablements.add(new TagTechNFCEnablement(currentTechs));
				}
			}

		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not parse NFC info (nfc.xml)", e));
		} finally {
			Util.safeClose(nfcReader);
		}
	}

	public List<INFCEnablement> getEnablements() {
		return enablements;
	}

	public MoSyncProject getProject() {
		return project;
	}
}
