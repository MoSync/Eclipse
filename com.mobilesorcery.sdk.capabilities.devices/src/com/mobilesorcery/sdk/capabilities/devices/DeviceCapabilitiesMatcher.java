package com.mobilesorcery.sdk.capabilities.devices;

import com.mobilesorcery.sdk.capabilities.core.AddDeviceFilterChangeRequest;
import com.mobilesorcery.sdk.capabilities.core.CompoundChangeRequest;
import com.mobilesorcery.sdk.capabilities.core.ICapabilitiesMatcher;
import com.mobilesorcery.sdk.capabilities.core.IChangeRequest;
import com.mobilesorcery.sdk.core.CapabilityState;
import com.mobilesorcery.sdk.core.ICapabilities;
import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.profiles.filter.DeviceCapabilitiesFilter;

public class DeviceCapabilitiesMatcher implements ICapabilitiesMatcher {

	public DeviceCapabilitiesMatcher() {
	}

	@Override
	public IChangeRequest match(MoSyncProject project,
			ICapabilities requestedCapabilites) {
		CompoundChangeRequest result = new CompoundChangeRequest("Remove devices that lacks required capabilities", project);
		/*for (String requestedCapability : requestedCapabilites.listCapabilities()) {
			AddDeviceFilterChangeRequest changeRequest;
			for (CapabilityState disallowedCapabilityState : CapabilityState.values()) {
				DeviceCapabilitiesFilter filter = new DeviceCapabilitiesFilter(requestedCapability, disallowedCapabilityState);
				changeRequest = new AddDeviceFilterChangeRequest(project, filter);
				changeRequest.setMessage(createChangeRequestMessage(requestedCapability, disallowedCapabilityState));
				if (disallowedCapabilityState != CapabilityState.SUPPORTED && changeRequest.isApplicable()) {
					result.addChangeRequest(changeRequest);
					result.setShouldApply(changeRequest, disallowedCapabilityState == CapabilityState.NOT_IMPLEMENTED || disallowedCapabilityState == CapabilityState.UNSUPPORTED);
				}
			}
		}*/
		//  ABOVE TEMPORARILY REMOVED!

		return result.getChangeRequests().length > 0 ? result : null;
	}

	private String createChangeRequestMessage(String requestedCapability,
			CapabilityState disallowedCapabilityState) {
		switch (disallowedCapabilityState) {
		case NOT_IMPLEMENTED:
			return requestedCapability + ": Remove {0} devices for which this capability is not yet implemented";
		case UNSUPPORTED:
			return requestedCapability + ": Remove {0} devices that do not support this capability";
		case REQUIRES_PERMISSION:
			return requestedCapability + ": Remove {0} devices that requires permissions to be set for this capability";
		case REQUIRES_PRIVILEGED_PERMISSION:
			return requestedCapability + ": Remove {0} devices that requires privileged permissions (eg operator certs) to be set for this capability";
		default:
			return "?";
		}
	//	 " + requestedCapability);
	}

	/*public IChangeRequest match(MoSyncProject project,
			ICapabilities requestedCapabilites) {
		IApplicationPermissions permissions = project.getPermissions();

		IProfile[] profiles = project.getFilteredProfiles();
		HashMap<String, Map<CapabilityState, List<IProfile>>> capabilityStates = new HashMap<String, Map<CapabilityState,List<IProfile>>>();
		for (IProfile profile : profiles) {
			ICapabilities profileCapabilities = DeviceCapabilitesPlugin.getDefault().getCapabilitiesForProfile(profile);

			for (String requestedCapability : requestedCapabilites.listCapabilities()) {
				CapabilityState capabilityState = (CapabilityState) profileCapabilities.getCapabilityValue(requestedCapability);
				if (capabilityState == null) {
					capabilityState = CapabilityState.UNSUPPORTED;
				}

				if (capabilityState != CapabilityState.SUPPORTED && !permissions.isPermissionRequested(requestedCapability)) {
					Map<CapabilityState, List<IProfile>> missingCapabilities = capabilityStates.get(requestedCapability);
					if (missingCapabilities == null) {
						missingCapabilities = new HashMap<CapabilityState, List<IProfile>>();
						capabilityStates.put(requestedCapability, missingCapabilities);
					}

					List<IProfile> profileList = missingCapabilities.get(capabilityState);
					if (profileList == null) {
						profileList = new ArrayList<IProfile>();
						missingCapabilities.put(capabilityState, profileList);
					}

					profileList.add(profile);
				}
			}
		}

		if (!capabilityStates.isEmpty()) {
			return createChangeRequest(project, capabilityStates);
		}

		return null;
	}

	private IChangeRequest createChangeRequest(MoSyncProject project,
			HashMap<String, Map<CapabilityState, List<IProfile>>> capabilityStates) {
		CompoundChangeRequest result = new CompoundChangeRequest("Remove devices that lacks required capabilities", project);

		for (String requestedCapability : capabilityStates.keySet()) {
			Map<CapabilityState, List<IProfile>> profilesWithLackingCapabilities = capabilityStates.get(requestedCapability);
			if (!profilesWithLackingCapabilities.isEmpty()) {
				CompoundChangeRequest requestedCapabilityChangeRequest = new CompoundChangeRequest(MessageFormat.format("Missing required capability: {0}", requestedCapability), project);
				requestedCapabilityChangeRequest.setParent(result);
				result.addChangeRequest(requestedCapabilityChangeRequest);
				for (CapabilityState state : profilesWithLackingCapabilities.keySet()) {
					ProfileFilter filter = new ProfileFilter();
					filter.setProfiles(profilesWithLackingCapabilities.get(state).toArray(new IProfile[0]), true);
					AddDeviceFilterChangeRequest changeRequest = new AddDeviceFilterChangeRequest(project, filter);
					requestedCapabilityChangeRequest.addChangeRequest(changeRequest);
					if (state == CapabilityState.NOT_IMPLEMENTED || state == CapabilityState.UNSUPPORTED) {
						changeRequest.setMessage("Remove {0} devices that do not support the capabilities needed for this project");
					} else if (state == CapabilityState.REQUIRES_PERMISSION) {
						changeRequest.setMessage("Remove {0} devices that requires permisson for the capabilities needed for this project");
					} else if (state == CapabilityState.REQUIRES_PRIVILEGED_PERMISSION) {
						changeRequest.setMessage("Remove {0} devices that requires special privileges (such as operator certificates) for the capabilities needed for this project");
						requestedCapabilityChangeRequest.setShouldApply(changeRequest, false);
					}
				}
			}
		}

		return result;
	}*/

}
