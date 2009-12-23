package com.mobilesorcery.sdk.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import com.mobilesorcery.sdk.core.IFilter;
import com.mobilesorcery.sdk.core.MoSyncProject;

public class IsExcludableFromBuildTester extends MoSyncNatureTester {

	public static String EXCLUDABLE = "excludable";

	public static String INCLUDABLE = "includable";

	public IsExcludableFromBuildTester() {
		// TODO Auto-generated constructor stub
	}

	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		MoSyncProject project = extractProject(receiver, property, args,
				expectedValue);
		if (project != null) {
			List<IResource> resources = extractApplicableResources(receiver,
					property, args, expectedValue);
			System.err.println(expectedValue + "; " + resources);
			return resources.size() > 0;
		}
		

		return false;
	}

	private List<IResource> extractApplicableResources(Object receiver,
			String property, Object[] args, Object expectedValue) {
		ArrayList result = new ArrayList();

		if (receiver instanceof List) {
			List list = (List) receiver;
			for (int i = 0; i < list.size(); i++) {
				IResource resource = extractApplicableResource(list.get(i),
						property, args, expectedValue);
				if (resource != null) {
					result.add(resource);
				}
			}
		} else {
			IResource resource = extractApplicableResource(receiver, property,
					args, expectedValue);
			if (resource != null) {
				result.add(resource);
			}
		}

		return result;
	}

	private IResource extractApplicableResource(Object receiver,
			String property, Object[] args, Object expectedValue) {
		if (receiver instanceof IResource) {
			IResource resource = (IResource) receiver;
			if (test(resource, expectedValue)) {
				return resource;
			}
		} else if (receiver instanceof IAdaptable) {
			return extractApplicableResource(((IAdaptable) receiver)
					.getAdapter(IResource.class), property, args, expectedValue);
		}

		return null;
	}

	private boolean test(IResource resource, Object expectedValue) {
		boolean isIncludable = INCLUDABLE.equals(expectedValue);
		boolean isExcludable = EXCLUDABLE.equals(expectedValue);

		if (isExcludable || isIncludable) {
			if (resource.getType() == IResource.FILE
					|| resource.getType() == IResource.FOLDER) {
				IFilter<IResource> filter = MoSyncProject
						.getExclusionFilter(MoSyncProject.create(resource
								.getProject()));
				boolean isIncluded = filter.accept(resource);
				return isExcludable ? isIncluded : !isIncluded;
			}
		}

		return false;
	}

}
