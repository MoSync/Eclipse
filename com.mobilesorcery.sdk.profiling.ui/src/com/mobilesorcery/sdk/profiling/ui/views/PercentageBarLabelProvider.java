package com.mobilesorcery.sdk.profiling.ui.views;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;

import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingSession;

public class PercentageBarLabelProvider extends OwnerDrawLabelProvider {

    private final static NumberFormat PERCENTAGE = new DecimalFormat("##0.00%", DecimalFormatSymbols.getInstance(Locale.US));
    
    private IProfilingSession session;
    private boolean aggregatedPercentage;
    
    public PercentageBarLabelProvider(boolean aggregatedPercentage) {
        this.aggregatedPercentage = aggregatedPercentage;
    }
    
    public void setSession(IProfilingSession session) {
        this.session = session;
    }
    
    protected void measure(Event event, Object obj) {
        Rectangle bounds = ((TreeItem)event.item).getBounds(event.index);
        event.setBounds(new Rectangle(event.x, event.y, bounds.width, bounds.height));
    }

    protected void paint(Event event, Object obj) {
        measure(event, obj);
        IInvocation invocation = (IInvocation) obj;
        float timeInMs = aggregatedPercentage ? invocation.getAggregateTime() : invocation.getSelfTime();
        String percentage = getPercentage(timeInMs);
        float ratio = getRatio(timeInMs);
        
        GC gc = event.gc;
        Rectangle bounds = event.getBounds();
        
        Point actualTextExtent = gc.textExtent(percentage);
        gc.drawText(percentage, bounds.x + event.width - actualTextExtent.x, bounds.y);
        
        gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
        int barWidth = Math.round(((float)bounds.width - maxTextExtent(event).x) * ratio);
        gc.fillRectangle(bounds.x, bounds.y + 1, barWidth, bounds.height - 2);
    }

    private Point maxTextExtent(Event event) {
        return event.gc.textExtent(">100.00%");
    }
    private float getRatio(float timeInMs) {
        float totalTime = getTotalTime(session);
        float ratio = timeInMs / totalTime;
        return ratio;   
    }
    
    private String getPercentage(float timeInMs) {
        return PERCENTAGE.format(getRatio(timeInMs));
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
}
