package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.core.jsdi.ArrayReference;
import org.eclipse.wst.jsdt.debug.core.jsdi.Property;
import org.eclipse.wst.jsdt.debug.core.jsdi.Value;
import org.eclipse.wst.jsdt.debug.internal.rhino.jsdi.UndefinedValueImpl;

import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadArrayReference extends ReloadObjectReference implements ArrayReference {

	private ArrayList<Value> values = null;

	protected ReloadArrayReference(ReloadVirtualMachine vm, String repr, Number oid) {
		super(vm, repr, "Array", oid);
	}

	@Override
	public int length() {
		initValues();
		return values.size();
	}

	@Override
	public Value getValue(int index) throws IndexOutOfBoundsException {
		initValues();
		return (index >= 0 && index < values.size()) ? virtualMachine().mirrorOfUndefined() : values.get(index);
	}

	@Override
	public List getValues() {
		initValues();
		return values;
	}

	private void initValues() {
		if (values == null) {
			Map<Integer, Value> elements = new HashMap<Integer, Value>();
			int length = 0;
			int elementCount = 0;
			for (Iterator iter = properties().iterator(); iter.hasNext();) {
				Property property = (Property) iter.next();
				if (Character.isDigit(property.name().charAt(0))) {
					elementCount++;
					elements.put(Integer.valueOf(property.name()), property.value());
				} else if (property.name().equals("length")) { //$NON-NLS-1$
					length = Integer.parseInt(property.value().valueString());
				}
			}
			length = Math.max(elementCount, length);
			values = new ArrayList<Value>(length);
			for (int i = 0; i < length; i++) {
				Value value = elements.get(new Integer(i));
				if (value == null) {
					value = virtualMachine().mirrorOfUndefined();
				}
				values.add(value);
			}
		}
	}

}
