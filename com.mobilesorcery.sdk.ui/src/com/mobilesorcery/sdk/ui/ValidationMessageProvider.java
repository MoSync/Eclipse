package com.mobilesorcery.sdk.ui;

import java.util.LinkedHashMap;
import java.util.Set;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Control;

/**
 * A message provider that handles validation of several properties.
 * @author mattias.bybro@mosync.com
 *
 */
public class ValidationMessageProvider implements IMessageProvider {

	private final LinkedHashMap<Control, IMessageProvider> messages = new LinkedHashMap<Control, IMessageProvider>();

	public void setMessage(Control control, IMessageProvider message) {
		if (message == this) {
			throw new IllegalArgumentException();
		}
		if (DefaultMessageProvider.isEmpty(message)) {
			messages.remove(control);
		} else {
			messages.put(control,  message);
		}
	}

	@Override
	public String getMessage() {
		IMessageProvider message = getMaxSeverityMessage();
		return message == null ? null : message.getMessage();
	}

	@Override
	public int getMessageType() {
		IMessageProvider message = getMaxSeverityMessage();
		return message == null ? NONE : message.getMessageType();
	}

	public IMessageProvider getMessage(Control control) {
		return messages.get(control);
	}

	public IMessageProvider getMaxSeverityMessage() {
		int maxSeverity = IMessageProvider.NONE;
		IMessageProvider result = null;
		for (Control control : messages.keySet()) {
			IMessageProvider message = messages.get(control);
			if (message.getMessageType() > maxSeverity) {
				maxSeverity = message.getMessageType();
				result = message;
			}
		}
		return result;
	}

	public static int compare(IMessageProvider provider1, IMessageProvider provider2) {
		if (provider1 == null) {
			provider1 = DefaultMessageProvider.EMPTY;
		}
		if (provider2 == null) {
			provider2 = DefaultMessageProvider.EMPTY;
		}
		return provider1.getMessageType() - provider2.getMessageType();
	}

	public boolean isEmpty(Control control) {
		return DefaultMessageProvider.isEmpty(getMessage(control));
	}

	public Set<Control> getControls() {
		return this.messages.keySet();
	}

	@Override
	public String toString() {
		return messages.toString();
	}

}
