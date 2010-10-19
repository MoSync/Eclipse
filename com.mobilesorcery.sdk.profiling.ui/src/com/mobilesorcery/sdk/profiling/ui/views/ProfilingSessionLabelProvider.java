package com.mobilesorcery.sdk.profiling.ui.views;

import java.text.DateFormat;
import java.util.Calendar;

import org.eclipse.jface.viewers.LabelProvider;

import com.mobilesorcery.sdk.profiling.IProfilingSession;

public class ProfilingSessionLabelProvider extends LabelProvider {

	public String getText(Object element) {
		if (element instanceof IProfilingSession) {
			IProfilingSession session = (IProfilingSession) element;
			Calendar timestamp = session.getStartTime();
			String timestampStr = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(timestamp.getTime());
			return session.getName() + " " + timestampStr;
		} else {
			return super.getText(element);
		}
	}
}
