package com.mobilesorcery.sdk.ui.internal.navigationext;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import com.mobilesorcery.sdk.core.IsReleasePackageTester;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

public class MoSyncNavigatorLabelProvider implements IStyledLabelProvider, ILabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof ReleasePackage) {
			String base = getText(element);
			StyledString str = new StyledString(base);
			String res = MessageFormat.format(" [{0}]", ((ReleasePackage) element).getUnderlyingResource().getName());
			str.append(res);
			str.setStyle(base.length(), res.length(), StyledString.COUNTER_STYLER);
			return str;
		}
		return null;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof ReleasePackage) {
			return MosyncUIPlugin.getDefault().getImageRegistry().get(MosyncUIPlugin.IMG_BINARY);
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		return element.toString();
	}

}
