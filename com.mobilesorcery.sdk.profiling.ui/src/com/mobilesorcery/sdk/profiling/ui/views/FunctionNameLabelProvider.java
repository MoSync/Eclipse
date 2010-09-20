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

import com.mobilesorcery.sdk.profiling.IInvocation;
import com.mobilesorcery.sdk.profiling.ui.ProfilingUiPlugin;
import com.mobilesorcery.sdk.ui.UIUtils;

public class FunctionNameLabelProvider extends StyledCellLabelProvider {

    private Font prefixFont;
    private Font lastNameFont;
    private Styler prefixStyler;
    private Styler lastNameStyle;
    private Styler disabledStyle;
    
    public FunctionNameLabelProvider() {
        prefixFont = UIUtils.modifyFont(null, SWT.ITALIC);
        lastNameFont = UIUtils.modifyFont(null, SWT.BOLD);
        Color disabledColor = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
        
        prefixStyler = createStyler(prefixFont, null);
        lastNameStyle = createStyler(lastNameFont, null);
        disabledStyle = createStyler(null, disabledColor);
    }
    
    public void dispose() {
        UIUtils.dispose(prefixFont, lastNameFont);
    }
    
    public void update(ViewerCell cell) {
        Object obj = cell.getElement();
        IInvocation invocation = (IInvocation) obj;
        StyledString styledFunctionName = styleFunctionName(invocation.getProfiledEntity().toString());
        cell.setStyleRanges(styledFunctionName.getStyleRanges());
        cell.setText(styledFunctionName.getString());
        cell.setImage(ProfilingUiPlugin.getDefault().getImageRegistry().get(ProfilingUiPlugin.METHOD_IMG));
    }
    
    private StyledString styleFunctionName(String fn) {
        StyledString result = new StyledString();
        // Parse stuff
        int firstParenIx = fn.indexOf('(');
        int lastSpaceIx = Math.max(0, (firstParenIx == -1 ? fn : fn.substring(0, firstParenIx)).lastIndexOf(' '));
        String prefixText = fn.substring(0, lastSpaceIx);
        
        String unmangledSymbol = fn.substring(lastSpaceIx);
        firstParenIx = unmangledSymbol.indexOf('('); 
        String unmangleSymbolNoSignature = firstParenIx == -1 ? unmangledSymbol : unmangledSymbol.substring(0, firstParenIx);
        String signatureString = firstParenIx == -1 ? "" : unmangledSymbol.substring(firstParenIx);

        int lastDoubleColonIx = unmangleSymbolNoSignature.lastIndexOf("::");
        String lastSegment = lastDoubleColonIx == -1 ? unmangleSymbolNoSignature : unmangleSymbolNoSignature.substring(lastDoubleColonIx);
        String otherSegments = unmangleSymbolNoSignature.substring(0, unmangleSymbolNoSignature.length() - lastSegment.length());
        result.append(prefixText, prefixStyler);
        result.append(otherSegments);
        result.append(lastSegment, lastNameStyle);
        result.append(signatureString, disabledStyle);
        return result;
    }

    private StyledString.Styler createStyler(final Font font, final Color fgColor) {
        Styler result = new StyledString.Styler() {
            public void applyStyles(TextStyle textstyle) {
                if (fgColor != null) {
                    textstyle.foreground = fgColor;
                }
                if (font != null) {
                    textstyle.font = font;
                }
            }          
         }; 
         
         return result;
    }
}
