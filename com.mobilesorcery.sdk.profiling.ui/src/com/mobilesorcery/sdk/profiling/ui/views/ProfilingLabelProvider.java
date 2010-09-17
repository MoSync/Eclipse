/**
 * 
 */
package com.mobilesorcery.sdk.profiling.ui.views;

import java.util.HashMap;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.mobilesorcery.sdk.profiling.IInvocation;

class ProfilingLabelProvider extends LabelProvider implements ITableLabelProvider {
    
    public static final String FUNCTION_COL = "fn";
    public static final String CALLS_COL = "c";
    public static final String SELF_TIME_COL = "st";
    public static final String SELF_TIME_PERC_COL = "stp";
    public static final String AGG_TIME_COL = "at";
    
    private HashMap<Integer, Object> columns = new HashMap<Integer, Object>();
    
    public ProfilingLabelProvider(TreeColumn[] columns) {
        for (int i = 0; i < columns.length; i++) {
            this.columns.put(i, columns[i].getData());
        }
    }
    
	public String getColumnText(Object obj, int index) {
	    IInvocation invocation = (IInvocation) obj;
	    Object column = columns.get(index);
	    if (FUNCTION_COL == column) {
	        return invocation.getProfiledEntity().toString();
	    } else if (SELF_TIME_COL == column) {
	        return invocation.getSelfTime() + " ms";
	    } else if (CALLS_COL == column) {
	        return "" + invocation.getCount();
	    } else if (AGG_TIME_COL == column) {
	        return invocation.getAggregateTime() + " ms";
	    }
		return getText(obj);
	}
	
	public Image getColumnImage(Object obj, int index) {
		return getImage(obj);
	}
	public Image getImage(Object obj) {
		return PlatformUI.getWorkbench().
				getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
	}
}