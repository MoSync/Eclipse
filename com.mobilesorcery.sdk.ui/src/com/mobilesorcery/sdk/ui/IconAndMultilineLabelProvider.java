package com.mobilesorcery.sdk.ui;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.profiles.IProfile;

/**
 * Simple label provider for having an icon + multiple lines
 */
public abstract class IconAndMultilineLabelProvider extends OwnerDrawLabelProvider
{
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 10;

    protected final Display display;
    protected final TableViewer viewer;

	/**
	 * Initializes the icons shown for each device type. Will fail if the icons
	 * cannot be found.
	 *
	 * @param display The display that handles the UI.
	 */
	public IconAndMultilineLabelProvider(TableViewer viewer) {
	    this.viewer = viewer;
		this.display = viewer.getControl().getDisplay();
	}

	/**
	 * Returns the image corresponding a given element.
	 *
	 * @param element An object to render an image for
	 * @return The image corresponding to given element.
	 */
	public abstract Image getImage(Object element);

	/**
	 * Returns the lines to render for a given element.
	 * @param element
	 * @return
	 */
    public abstract String[] getLines(Object element);

    /**
     * Returns the fonts to use for each line of a given
     * element. A font being {@code null} means "use default font".
     * @param element
     * @return May be {@code null} or an array of any length (the
     * length need not correspond to the number of lines for the element)
     */
    public Font[] getFonts(Object element) {
    	return null;
    }

    /**
     * Returns the colors to use for each line of a given
     * element. A color being {@code null} means "use default font".
     * The default implementation returns "BLACK, BLACK, GRAY".
     * @param element
     * @return May be {@code null} or an array of any length (the
     * length need not correspond to the number of lines for the element)
     */
    public Color[] getColors(Object element) {
    	Color[] colors = new Color[] { display.getSystemColor(SWT.COLOR_BLACK), display.getSystemColor(SWT.COLOR_BLACK), display.getSystemColor(SWT.COLOR_GRAY) };
    	return colors;
    }

    @Override
	protected void measure(Event event, Object element) {
        Image image = getImage(element);
        Point te = computeTextExtent(event.gc, element);
        int height = Math.max(computeImageExtent(image).y, te.y) + 2 * PADDING_Y;
        int width = viewer.getTable().getColumn(event.index).getWidth();
        //int width = viewer.getTable().getColumn(event.index).getWidth();
        //int width = computeImageExtent(image).x + te.x + 2 * PADDING_X;
        event.setBounds(new Rectangle(event.x, event.y, width, height));
    }

    @Override
	protected void paint(Event event, Object element) {
        String[] lines = getLines(element);

        Image image = getImage(element);
        Rectangle bounds = event.getBounds();
        GC gc = event.gc;
        if (image != null) {
            gc.drawImage(image, bounds.x, bounds.y + PADDING_Y);
        }

        Point imageExtent = computeImageExtent(image);
        Point te = computeTextExtent(gc, element);
        int centered = image == null ? 0 : Math.max(0, (imageExtent.y - te.y) / 2);

        drawLines(gc, element, bounds.x + imageExtent.x + PADDING_X, bounds.y + centered + PADDING_Y);
    }

    private void drawLines(GC gc, Object element, int x, int y) {
        String[] lines = getLines(element);
        Color[] colors = getColors(element);
    	Font[] fonts = getFonts(element);
    	for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null) {
            	Color color = colors != null && colors.length > i ? colors[i] : display.getSystemColor(SWT.COLOR_BLACK);
                gc.setForeground(colors[i]);
            	Font font = fonts != null && fonts.length > i ? fonts[i] : null;
                gc.setFont(font);
                gc.drawText(lines[i], x, y, true);
                y += gc.textExtent(lines[i]).y;
            }
        }
    }

    private Point computeImageExtent(Image image) {
        return image == null ? new Point(0, 0) : new Point(image.getBounds().width, image.getBounds().height);
    }

    private Point computeTextExtent(GC gc, Object element) {
    	Font[] fonts = getFonts(element);
    	String[] lines = getLines(element);
    	int width = 0;
        int height = 0;
        for (int i = 0; lines != null && i < lines.length; i++) {
            if (lines[i] != null) {
            	Font font = fonts != null && fonts.length > i ? fonts[i] : null;
                gc.setFont(font);
            	Point te = gc.textExtent(lines[i]);
                height += te.y;
                width = Math.max(width, te.x);
            }
        }

        return new Point(width, height);
    }
}
