package com.mobilesorcery.sdk.molint.rules;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.mobilesorcery.sdk.core.IBuildVariant;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.apisupport.nfc.NFCSupport;
import com.mobilesorcery.sdk.core.security.ICommonPermissions;
import com.mobilesorcery.sdk.molint.AbstractMolintRule;

public class NFCRule extends AbstractMolintRule {

	private static final String ID = "nfc";

	public NFCRule() {
		super(ID, "NFC");
	}

	@Override
	public List<IMarker> analyze(IProgressMonitor monitor,
			MoSyncProject project, IBuildVariant variant) throws CoreException {
		// TODO: API Analysis
		ArrayList<IMarker> result = new ArrayList<IMarker>();
		if (project.getPermissions().isPermissionRequested(ICommonPermissions.NFC)) {
			NFCSupport nfcSupport = NFCSupport.create(project);
			if (!nfcSupport.getNFCDescription().exists()) {
				IMarker marker = project.getWrappedProject().createMarker(ICModelMarker.C_MODEL_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, 
						MessageFormat.format(
								"NFC permission set; requires an NFC enablement file at {0}.",
								nfcSupport.getNFCDescription().getAbsolutePath()));
				marker.setAttribute(IMarker.SEVERITY, getSeverity(IMarker.SEVERITY_ERROR));
				result.add(marker);
			}
		}
		return result;
	}

}
