package com.mobilesorcery.sdk.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * Adds validation info to a certain control.
 * @author mattias
 *
 * TODO: Should we start using Eclipse's databinding instead?
 */
public class FieldValidationHelper {

	private final HashMap<Control, ControlDecoration> decorations = new HashMap<Control, ControlDecoration>();
	private Set<Control> previousControls = new HashSet<Control>();
	private final HashMap<TabItem, IMessageProvider> messagesPerTab = new HashMap<TabItem, IMessageProvider>();

	public FieldValidationHelper() {

	}

	public void setMessage(ValidationMessageProvider provider) {
		for (TabItem tab : messagesPerTab.keySet()) {
			messagesPerTab.put(tab, DefaultMessageProvider.EMPTY);
		}

		HashSet<Control> resetControls = new HashSet<Control>(previousControls);

		for (Control control : provider.getControls()) {
			IMessageProvider message = provider.getMessage(control);
			if (message != null) {
				setMessage(control, message);
				resetControls.remove(control);
			}
		}

		for (Control control : resetControls) {
			setMessage(control, DefaultMessageProvider.EMPTY);
		}

		for (TabItem tab : messagesPerTab.keySet()) {
			IMessageProvider tabMessage = messagesPerTab.get(tab);
			tab.setImage(getImage(tabMessage == null ? IMessageProvider.NONE : tabMessage.getMessageType(), true));
		}

		previousControls = provider.getControls();
	}

	private void setMessage(Control control, IMessageProvider message) {
		TabItem tab = guessTabAncestor(control);
		if (tab != null) {
			IMessageProvider tabMessage = messagesPerTab.get(tab);
			if (ValidationMessageProvider.compare(message, tabMessage) > 0) {
				messagesPerTab.put(tab, message);
			}
		}

		ControlDecoration decoration = initDecoration(control);
		if (decoration != null) {
			decoration.setDescriptionText(message.getMessage());
			Image image = getImage(message.getMessageType(), false);
			if (image == null) {
				decoration.hide();
			} else {
				decoration.show();
				decoration.setImage(image);
			}
		}
	}

	private Label guessLabel(Control control) {
		Control[] siblings = control.getParent().getChildren();
		Label currentLabel = null;
		for (int i = 0; i < siblings.length; i++) {
			Control sibling = siblings[i];
			if (sibling instanceof Label) {
				currentLabel = (Label) sibling;
			}
			if (sibling == control) {
				break;
			}
		}
		return currentLabel;
	}

	private TabItem guessTabAncestor(Control control) {
		TabFolder folder = null;
		boolean found = false;
		Composite current = control.getParent();
		Control tabControl = control;
		while (!found) {
			tabControl = current;
			current = current.getParent();
			if (current == null || current instanceof TabFolder) {
				folder = (TabFolder) current;
				found = true;
			}
		}

		if (folder != null) {
			TabItem[] tabItems = folder.getItems();
			for (TabItem tabItem : tabItems) {
				if (tabControl == tabItem.getControl()) {
					return tabItem;
				}
			}
		}

		return null;
	}

	private Image getImage(int messageType, boolean onlyNoninformative) {
		String id = null;
		switch (messageType) {
		case IMessageProvider.ERROR:
			id = FieldDecorationRegistry.DEC_ERROR;
			break;
		case IMessageProvider.WARNING:
			id = FieldDecorationRegistry.DEC_WARNING;
			break;
		case IMessageProvider.INFORMATION:
			id = onlyNoninformative ? null : FieldDecorationRegistry.DEC_INFORMATION;
			break;
		}
		return id == null ? null : FieldDecorationRegistry.getDefault()
				.getFieldDecoration(id).getImage();
	}

	private ControlDecoration initDecoration(Control validatedControl) {
		ControlDecoration decoration = decorations.get(validatedControl);
        if (decoration == null) {
        	decoration = new ControlDecoration(validatedControl, SWT.LEFT | SWT.CENTER);
        	decorations.put(validatedControl, decoration);
        }
        return decoration;
	}

}
