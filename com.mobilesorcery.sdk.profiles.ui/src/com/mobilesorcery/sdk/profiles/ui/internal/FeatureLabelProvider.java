/**
 * 
 */
package com.mobilesorcery.sdk.profiles.ui.internal;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.mobilesorcery.sdk.core.MoSyncTool;

final class FeatureLabelProvider extends LabelProvider implements ITableLabelProvider {
    public String getColumnText(Object element, int columnIndex) {
        return (MoSyncTool.getDefault().getFeatureDescription((String)element));
    }

    public Image getColumnImage(Object element, int columnIndex) {
        // TODO Auto-generated method stub
        return null;
    }
}