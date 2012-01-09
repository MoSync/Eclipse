package com.mobilesorcery.sdk.core.stats;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Variables implements IVariable {

	private static final String TYPE = "vars";

	private final HashMap<String, IVariable> variables = new HashMap<String, IVariable>();

	public <T extends IVariable> T get(Class<T> clazz, String variableKey) {
		IVariable variable = variables.get(variableKey);
		if (variable == null || !clazz.isAssignableFrom(variable.getClass())) {
			try {
				variable = clazz.newInstance();
				variables.put(variableKey, variable);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return (T) variable;
	}

	public void write(Writer output) throws IOException {
		JSONObject outputObject = new JSONObject();
		write(outputObject);
		outputObject.writeJSONString(output);
	}

	@Override
	public void write(JSONObject output) {
		for (Map.Entry<String, IVariable> variableEntry : variables.entrySet()) {
			String key = variableEntry.getKey();
			IVariable v = variableEntry.getValue();
			JSONObject o = new JSONObject();
			v.write(o);
			output.put(key, o);
		}
	}

	public void read(Reader input) throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		JSONObject inputObject = (JSONObject) parser.parse(input);
		read(inputObject);
	}

	@Override
	public void read(JSONObject input) {
		for (Object key : input.keySet()) {
			String keyStr = (String) key;
			JSONObject value = (JSONObject) input.get(keyStr);
			IVariable var = getVariable(value);
			if (var != null) {
				variables.put(keyStr, var);
			}
		}
	}

	private IVariable getVariable(JSONObject o) {
		String type = (String) o.get("type");
		IVariable result = null;
		// Ok, if this starts to get more complex, use xstream or smth.
		if (CounterVariable.TYPE.equals(type)) {
			result = new CounterVariable();
		} else if (DecimalVariable.TYPE.equals(type)) {
			result = new DecimalVariable();
		} else if (TimeStamp.TYPE.equals(type)) {
			result = new TimeStamp();
		} else if (StringVariable.TYPE.equals(type)) {
			result = new StringVariable();
		}

		if (result != null) {
			result.read(o);
		}

		return result;
	}

	@Override
	public String getType() {
		return TYPE;
	}
}
