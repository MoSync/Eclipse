/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.mobilesorcery.sdk.core.MoSyncProject;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.ui.internal.console.PathLink;

public class UIUtils {

	public static void setDefaultShellBounds(Shell shell) {
        Rectangle displayBounds = shell.getDisplay().getBounds();
        shell.setSize(320, 480);

        int x = (displayBounds.width - shell.getBounds().width) / 2;
        int y = (displayBounds.height - shell.getBounds().height) / 2;
        shell.setLocation(x, y);
    }

    public static int getDefaultFieldSize() {
        return 180;
    }

    public static int getDefaultListHeight() {
        return 320;
    }

	public static int getRowHeight(int rows) {
		Display display = Display.getCurrent();
		GC gc = new GC(display);
		try {
			Point wh = gc.textExtent("\n");
			return rows * wh.y;
		} finally {
			gc.dispose();
		}
	}

    /**
     * Opens the editor in the workbench for a specific file.
     * @param workbench
     * @param file
     */
    public static void openResource(final IWorkbenchWindow window, final IFile file) {
        if (window != null) {
            final IWorkbenchPage activePage =
                window.getActivePage();

            if (activePage != null) {
                final Display display = window.getShell().getDisplay();

                if (display != null) {
                    display.asyncExec(new Runnable() {
                        @Override
						public void run() {
                            try {
                                IDE.openEditor(activePage, file, true);
                            } catch (PartInitException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }
    }

    public static void openResource(IWorkbench workbench, IFile file) {
        openResource(workbench.getActiveWorkbenchWindow(), file);
    }

    public static void openResource(IPath path, int lineNumber) {
    	// Minor hack :)
        IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(path);
    	if (files.length > 0) {
            new PathLink(files[0], null, -1, -1, lineNumber).linkActivated();
    	} else {
    		new PathLink(path, null, -1, -1, lineNumber).linkActivated();
    	}
    }

    public static void setTransparency(ImageData data, int alpha) {
    	for (int x = 0; x < data.width; x++) {
    		for (int y = 0; y < data.height; y++) {
    			data.setAlpha(x, y, alpha);
    		}
    	}
    }

    public static ImageData mix(ImageData data, RGB mixRGB, int alpha) {
    	Image image = new Image(Display.getCurrent(), data);
    	GC gc = new GC(image);
    	Color mixColor = new Color(Display.getCurrent(), mixRGB);
    	try {
    		gc.setAlpha(alpha);
    		gc.setBackground(mixColor);
    		gc.fillRectangle(image.getBounds());
    		return image.getImageData();
    	} finally {
    		image.dispose();
    		gc.dispose();
    		mixColor.dispose();
    	}
    }

    public static void safeDispose(GC gc) {
        if (gc != null && !gc.isDisposed()) {
            gc.dispose();
        }
    }

    public static void safeDispose(Image image) {
        if (image != null && !image.isDisposed()) {
            image.dispose();
        }
    }

    public static void safeDispose(Shell shell) {
        if (shell != null && !shell.isDisposed()) {
            shell.dispose();
        }
    }

    /**
     * Returns a password property; if no such property is defined (or empty),
     * a password dialog is popped up.
     * @param project
     * @param propertyKey
     * @return
     */
    public static String getPassword(final MoSyncProject project, final String propertyKey) {
        String pwd = project.getProperty(propertyKey);
        if (Util.isEmpty(pwd)) {
            final String[] result = new String[1];
            Display display = PlatformUI.getWorkbench().getDisplay();
            display.syncExec(new Runnable() {
                @Override
				public void run() {
                    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                    PasswordDialog dialog = new PasswordDialog(shell);
                    if (dialog.open() == PasswordDialog.OK) {
                        result[0] = dialog.getPassword();
                        if (dialog.shouldRememberPassword()) {
                            project.setProperty(propertyKey, result[0]);
                        }
                    }
                }
            });
            return result[0];
        } else {
            return pwd;
        }
    }

    /**
     * <p>Creates a new {@link Font} based on a prototype font.</p>
     * <p>Clients must dispose of the new font.</p>
     * @param original
     * @param style The style(ie <code>SWT.BOLD</code>) to apply to
     * the new font, or <code>SWT.DEFAULT</code> if the current style should be kept
     * @return
     */
    public static Font modifyFont(Font original, int style)  {
    	return modifyFont(original, style, 0);
    }

    /**
     * <p>Creates a new {@link Font} based on a prototype font.</p>
     * <p>Clients must dispose of the new font.</p>
     * @param original
     * @param style The style(ie <code>SWT.BOLD</code>) to apply to
     * the new font, or <code>SWT.DEFAULT</code> if the current style should be kept
     * @param relativeHeight
     * @return
     */
    public static Font modifyFont(Font original, int style, int relativeHeight) {
        if (original == null) {
            original = JFaceResources.getDefaultFont();
        }

        FontData[] fd = original.getFontData();
        fd = modifyFont(fd, style, relativeHeight);
        return new Font(original.getDevice(), fd);
    }

    /**
     * <p>Modifies an array of {@link FontData} based on a new style and relative height.</p>
     * @param original
     * @param style The style(ie <code>SWT.BOLD</code>) to apply to
     * the new font, or <code>SWT.DEFAULT</code> if the current style should be kept
     * @param relativeHeight
     * @return The modified array, always the same as the input value unless the input value is <code>null</code>
     * in which case a new array is created.
     */
    public static FontData[] modifyFont(FontData[] fd, int style, int relativeHeight) {
        if (fd == null) {
            fd = JFaceResources.getDefaultFont().getFontData();
        }

        for (int i = 0; i < fd.length; i++) {
        	if (style != SWT.DEFAULT) {
        		fd[i].setStyle(style);
        	}
            fd[i].setHeight(fd[i].getHeight() + relativeHeight);
        }

        return fd;
    }

    public static void main(String[] args) {
    	Image i = new Image(Display.getCurrent(), "C:\\development\\projects\\mobilesorcery-4\\com.mobilesorcery.sdk.ui\\icons\\mosyncproject.png");
    	ImageData d = i.getImageData();
    	ImageData d2 = mix(d, new RGB(0xff, 0xff, 0xff), 0x7f);
    	ImageLoader il = new ImageLoader();
    	il.data = new ImageData[] { d2 };
    	il.save("C:\\development\\projects\\mobilesorcery-4\\com.mobilesorcery.sdk.ui\\icons\\deprecated-mosyncproject.png", SWT.IMAGE_PNG);
    	i.dispose();
    }

    public static void dispose(Object... resources) {
        for (int i = 0; i < resources.length; i++) {
            Object resource = resources[i];
            if (resource instanceof Color) {
                ((Color) resource).dispose();
            } else if (resource instanceof Font) {
                ((Font) resource).dispose();
            } else if (resource instanceof Widget) {
                ((Widget) resource).dispose();
            }
        }
    }

    /**
     * Returns a default layout for preference/property pages
     * (no margins)
     * @param i
     * @return
     */
	public static GridLayout newPrefsLayout(int columns) {
		GridLayout result = new GridLayout(columns, false);
		result.marginWidth = 0;
		result.marginHeight = 0;
		return result;
	}

	/**
	 * Runs a runnable on the current UI thread, or if the
	 * thread this method is called from is NOT a UI thread,
	 * it will be posted to the UI thread.
	 * @param display
	 * @param r
	 */
	public static void onUiThread(Display display, Runnable r, boolean fork) {
		if (display == Display.getCurrent()) {
			r.run();
		} else {
			if (fork) {
				display.asyncExec(r);
			} else {
				display.syncExec(r);
			}
		}
	}

	public static void centerShell(Shell newShell) {
		Rectangle screenSize = Display.getCurrent().getPrimaryMonitor().getBounds();
		int x = (screenSize.width - newShell.getSize().x) / 2;
		int y = (screenSize.height - newShell.getSize().y) / 2;
		newShell.setLocation(x, y);
	}

}
