package com.mobilesorcery.sdk.profiling.ui.views;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.profiling.CppFiltName;
import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.IProfilingSession;
import com.mobilesorcery.sdk.profiling.ui.ProfilingUiPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;

public class FunctionNameLabelProvider extends StyledCellLabelProvider {

    private final Font prefixFont;
    private final Font lastNameFont;
    private final Styler prefixStyler;
    private final Styler lastNameStyle;
    private final Styler disabledStyle;
    private IProfilingSession session;

    public FunctionNameLabelProvider() {
        prefixFont = UIUtils.modifyFont(null, SWT.ITALIC);
        lastNameFont = UIUtils.modifyFont(null, SWT.BOLD);
        Color disabledColor = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);

        prefixStyler = UIUtils.createStyler(prefixFont, null);
        lastNameStyle = UIUtils.createStyler(lastNameFont, null);
        disabledStyle = UIUtils.createStyler(null, disabledColor);
    }

    @Override
	public void dispose() {
        UIUtils.dispose(prefixFont, lastNameFont);
    }

    @Override
	public void update(ViewerCell cell) {
        Object obj = cell.getElement();
        IInvocation invocation = (IInvocation) obj;
        boolean enabled = session.getFilter().accept(invocation);
        StyledString styledFunctionName = styleFunctionName(enabled, invocation.getProfiledEntity().toString());
        cell.setStyleRanges(styledFunctionName.getStyleRanges());
        cell.setText(styledFunctionName.getString());
        cell.setImage(ProfilingUiPlugin.getDefault().getImageRegistry().get(ProfilingUiPlugin.METHOD_IMG));
    }

    private StyledString styleFunctionName(boolean enabled, String fn) {
        CppFiltName name = CppFiltName.parse(fn);
        StyledString result = new StyledString();

        result.append(name.getPrefix(), enabled ? prefixStyler : disabledStyle);
        result.append(name.getNamespace(), enabled ? null : disabledStyle);
        String shortName = name.getShortName();
        if (!Util.isEmpty(name.getNamespace())) {
            shortName = "::" + shortName;
        }
        result.append(shortName, enabled ? lastNameStyle : disabledStyle);
        result.append(name.getSignature(), disabledStyle);
        return result;
    }

    public void setSession(IProfilingSession session) {
        this.session = session;
    }
}
