/**
 * 
 */
package com.mobilesorcery.sdk.profiling.ui.views;

import java.awt.image.IndexColorModel;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.text.StyledEditorKit.ItalicAction;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TreeColumn;

import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.ui.ProfilingUiPlugin;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;

class ProfilingLabelProvider extends LabelProvider implements ITableLabelProvider {
    
    public static final String FUNCTION_COL = "fn";
    public static final String CALLS_COL = "c";
    public static final String SELF_TIME_COL = "st";
    public static final String SELF_TIME_PERC_COL = "stp";
    public static final String AGG_TIME_COL = "at";
    public static final String PERCENTAGE_TIME_COL = "pt";
    
    //private final static NumberFormat NUMBER_FORMAT_MS = new DecimalFormat("###,###,##0.000", DecimalFormatSymbols.getInstance(Locale.US));
    private final static NumberFormat NUMBER_FORMAT_US = new DecimalFormat("###,###,##0", DecimalFormatSymbols.getInstance(Locale.US));
    private final static NumberFormat PERCENTAGE = new DecimalFormat("##0.00%", DecimalFormatSymbols.getInstance(Locale.US));
    
    private HashMap<Integer, Object> columns = new HashMap<Integer, Object>();
   
    private IProfilingSession session;
    private boolean aggregatedPercentage;
    
    public ProfilingLabelProvider(boolean aggregatedPercentage) {
        this.aggregatedPercentage = aggregatedPercentage;
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
	    } else if (PERCENTAGE_TIME_COL == column) {
	        return getPercentage(aggregatedPercentage ? invocation.getAggregateTime() : invocation.getSelfTime());
	    }
	    
	    return getText(obj);
	}

    public Image getColumnImage(Object element, int ix) {
        return null;
    }

    private String getPercentage(float timeInMs) {
	    float totalTime = getTotalTime(session);
        float ratio = timeInMs / totalTime;
        
        return PERCENTAGE.format(ratio);
    }

    private float getTotalTime(IProfilingSession session) {
        // TODO: Refactor
        float totalTime = 0;
        IInvocation rootInvocation = session.getInvocation();
        for (IInvocation child : rootInvocation.flatten(null)) {
            if (child.getInvocations().isEmpty()) {
                totalTime += child.getAggregateTime();
            } else {
                totalTime += child.getSelfTime();
            }
        }
        return totalTime;
    }

    private String formatTime(float timeInMs) {
        return NUMBER_FORMAT_US.format(1000 * timeInMs) + " \u00B5s";
    }

    public void setSession(IProfilingSession session) {
        this.session = session;
    }

    public void setColumns(TreeColumn[] columns) {
        for (int i = 0; i < columns.length; i++) {
            this.columns.put(i, columns[i].getData());
        }
    }
}