package com.mobilesorcery.sdk.ui.targetphone.internal.bt;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.ui.targetphone.ITargetPhone;
import com.mobilesorcery.sdk.ui.targetphone.TargetPhonePlugin;

/**
 * Provides the UI with icons and a name of bluetooth devices.
 *
 * @author fmattias
 */
public class BluetoothLabelProvider extends OwnerDrawLabelProvider
{
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 10;

    /**
	 * Maps different device types to different icons.
	 */
	HashMap< BluetoothDevice.Type, Image> m_icons = new HashMap<BluetoothDevice.Type, Image>( );
    private final Display display;
    private final TableViewer viewer;

	/**
	 * Initializes the icons shown for each device type. Will fail if the icons
	 * cannot be found.
	 *
	 * @param display The display that handles the UI.
	 */
	public BluetoothLabelProvider(TableViewer viewer)
	{
	    this.viewer = viewer;
		this.display = viewer.getControl().getDisplay();
		/* Find icons and map them to different device types. */
		try {
			m_icons.put( BluetoothDevice.Type.COMPUTER, new Image( display, getClass( ).getResource( "/icons/desktop.png" ).openStream( ) ) );
			m_icons.put( BluetoothDevice.Type.LAPTOP, new Image( display, getClass( ).getResource( "/icons/laptop.png" ).openStream( ) ) );
			m_icons.put( BluetoothDevice.Type.PHONE, new Image( display, getClass( ).getResource( "/icons/phone.png" ).openStream( ) ) );
			m_icons.put( BluetoothDevice.Type.UNKNOWN, m_icons.get( BluetoothDevice.Type.COMPUTER ) );
		}
		catch( IOException e )
		{
			CoreMoSyncPlugin.getDefault().getLog().log(
					new Status(IStatus.ERROR, CoreMoSyncPlugin.PLUGIN_ID, "Could not load vital resources.") );
		}
	}

	/**
	 * Returns the image corresponding to the given device, or null
	 * if the element is not a BluetoothDevice.
	 *
	 * @param element A BluetoothDevice.
	 * @return The image corresponding to given element.
	 */
	public Image getImage(Object element)
	{
		BluetoothDevice device;
		if( ! (element instanceof BluetoothDevice) )
		{
			return null;
		}

		device = (BluetoothDevice) element;

		return m_icons.get( device.getType( ) );
	}

    @Override
	protected void measure(Event event, Object element) {
        Image image = getImage(element);
        Point te = computeTextExtent(event.gc, getLines(element));
        int height = Math.max(computeImageExtent(image).y, te.y) + 2 * PADDING_Y;
        int width = viewer.getTable().getColumn(event.index).getWidth();
        //int width = viewer.getTable().getColumn(event.index).getWidth();
        //int width = computeImageExtent(image).x + te.x + 2 * PADDING_X;
        event.setBounds(new Rectangle(event.x, event.y, width, height));
    }

    @Override
	protected void paint(Event event, Object element) {
        if (element instanceof BluetoothDevice) {
            String[] lines = getLines(element);

            Image image = getImage(element);
            Rectangle bounds = event.getBounds();
            GC gc = event.gc;
            if (image != null) {
                gc.drawImage(image, bounds.x, bounds.y + PADDING_Y);
            }

            Point imageExtent = computeImageExtent(image);
            Point te = computeTextExtent(gc, lines);
            int centered = image == null ? 0 : Math.max(0, (imageExtent.y - te.y) / 2);

            Color[] colors = new Color[] { display.getSystemColor(SWT.COLOR_BLACK), display.getSystemColor(SWT.COLOR_BLACK), display.getSystemColor(SWT.COLOR_GRAY) };
            drawLines(gc, bounds.x + imageExtent.x + PADDING_X, bounds.y + centered + PADDING_Y, lines, colors);
        }
    }

    private void drawLines(GC gc, int x, int y, String[] lines, Color[] colors) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null) {
                gc.setForeground(colors[i]);
                gc.drawText(lines[i], x, y, true);
                y += gc.textExtent(lines[i]).y;
            }
        }
    }

    private String[] getLines(Object element) {
        if (element instanceof BluetoothDevice) {
            BluetoothDevice btd = (BluetoothDevice) element;
            String name = btd.getProperty("name");
            String addr = btd.getProperty("address");
            ITargetPhone inHistory = BTTargetPhoneTransport.findInHistory(btd.getTargetPhone());
            int profileManagerType = TargetPhonePlugin.getDefault().getCurrentProfileManagerType();
            IProfile preferredProfile = inHistory == null ? null : inHistory.getPreferredProfile(profileManagerType);
            String preferredProfileName = preferredProfile == null ? null : preferredProfile.getVendor() + " - " + preferredProfile.getName();
            return new String[] { name, preferredProfileName, addr };
        }

        return new String[3];
    }


    private Point computeImageExtent(Image image) {
        return image == null ? new Point(0, 0) : new Point(image.getBounds().width, image.getBounds().height);
    }

    private Point computeTextExtent(GC gc, String[] lines) {
        int width = 0;
        int height = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] != null) {
                Point te = gc.textExtent(lines[i]);
                height += te.y;
                width = Math.max(width, te.x);
            }
        }

        return new Point(width, height);
    }

    @Override
	public void dispose() {
        super.dispose();
        for (Image icon : m_icons.values()) {
            icon.dispose();
        }
    }

}