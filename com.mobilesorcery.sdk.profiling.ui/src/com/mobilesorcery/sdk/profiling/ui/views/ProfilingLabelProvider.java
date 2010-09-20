/**
 * 
 */
package com.mobilesorcery.sdk.profiling.ui.views;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeColumn;

import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingSession;

class ProfilingLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {
    
    public static final String FUNCTION_COL = "fn";
    public static final String CALLS_COL = "c";
    public static final String SELF_TIME_COL = "st";
    public static final String SELF_TIME_PERC_COL = "stp";
    public static final String AGG_TIME_COL = "at";
    public static final String PERCENTAGE_TIME_COL = "pt";
    
    //private final static NumberFormat NUMBER_FORMAT_MS = new DecimalFormat("###,###,##0.000", DecimalFormatSymbols.getInstance(Locale.US));
    private final static NumberFormat NUMBER_FORMAT_US = new DecimalFormat("###,###,##0", DecimalFormatSymbols.getInstance(Locale.US));
    
    private HashMap<Integer, Object> columns = new HashMap<Integer, Object>();
    private IProfilingSession session;
    
    public ProfilingLabelProvider() {
        
    }
    
    public String getColumnText(Object obj, int index) {
	    IInvocation invocation = (IInvocation) obj;
	    Object column = columns.get(index);

	    if (SELF_TIME_COL == column) {
	        return formatTime(invocation.getSelfTime());
	    } else if (CALLS_COL == column) {
	        return "" + invocation.getCount();
	    } else if (AGG_TIME_COL == column) {
	        return formatTime(invocation.getAggregateTime());
	    }
	    
	    return getText(obj);
	}

    public Image getColumnImage(Object element, int ix) {
        return null;
    }

    private String formatTime(float timeInMs) {
        return NUMBER_FORMAT_US.format(1000 * timeInMs) + " \u00B5s";
    }

    public void setColumns(TreeColumn[] columns) {
        for (int i = 0; i < columns.length; i++) {
            this.columns.put(i, columns[i].getData());
        }
    }

    public Color getBackground(Object obj, int ix) {
        return null;
    }

    public Color getForeground(Object obj, int ix) {
        return session.getFilter().accept((IInvocation) obj) ? null : Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
    }
    
    public void setSession(IProfilingSession session) {
        this.session = session;
    }
}