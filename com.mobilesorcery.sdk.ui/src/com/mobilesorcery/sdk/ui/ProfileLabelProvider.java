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
/**
 *
 */
package com.mobilesorcery.sdk.ui;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.ITargetProfileProvider;
import com.mobilesorcery.sdk.profiles.IVendor;

public class ProfileLabelProvider extends LabelProvider {

	private ITargetProfileProvider targetProfileProvider;
	private final int style;
	private Point imageSize = new Point(16, 16);

	public static final int NO_IMAGES = 1;

	public ProfileLabelProvider(int style) {
		this.style = style;
	}

	public void setImageSize(Point size) {
		this.imageSize = size;
	}

	public void setTargetProfileProvider(
			ITargetProfileProvider targetProfileProvider) {
		this.targetProfileProvider = targetProfileProvider;
	}

	private boolean isFlat() {
		return (style & SWT.FLAT) != 0;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IProfile) {
			return ((IProfile) element).getName();
		} else if (element instanceof IVendor) {
			return ((IVendor) element).getName();
		} else {
			return "?";
		}
	}

	@Override
	public Image getImage(Object element) {
		if (style == NO_IMAGES) {
			return null;
		}

		if (element instanceof IProfile && !isFlat()) {
			if (targetProfileProvider != null
					&& targetProfileProvider.getTargetProfile() != null
					&& targetProfileProvider.getTargetProfile().equals(element)) {
				return MosyncUIPlugin.getDefault().getImageRegistry()
						.get(MosyncUIPlugin.TARGET_PHONE_IMAGE);
			} else {
				return MosyncUIPlugin.getDefault().getImageRegistry()
						.get(MosyncUIPlugin.PHONE_IMAGE);
			}
		} else if (element instanceof IVendor) {
			return MosyncUIPlugin.getDefault().getPlatformImage(
					(IVendor) element, imageSize);
		} else {
			return null;
		}
	}

}