package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.StackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.Value;
import org.eclipse.wst.jsdt.debug.core.jsdi.Variable;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.LocalVariableScope;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;

public class ReloadStackFrame implements StackFrame {

	private final static JSONParser PARSER = new JSONParser();

	private final ReloadVirtualMachine vm;
	private Variable thisVar;
	private SimpleLocation location;
	private Number catchLine;

	private boolean inited;

	private ArrayList<ReloadVariable> localVars;

	private final int stackDepth;

	private boolean isTop;

	public ReloadStackFrame(ReloadVirtualMachine vm, JSONObject suspendCommand,
			int ix) {
		this.vm = vm;
		this.stackDepth = ix;

		init(suspendCommand, ix);
	}

	public int getStackDepth() {
		return stackDepth;
	}
	
	private void init(JSONObject suspended, int ix) {
		int line = ((Long) suspended.get("line")).intValue();
		JSONArray stack = (JSONArray) suspended.get("stack");
		JSONArray frame = ix >= 0 ? (JSONArray) stack.get(ix) : null;
		String functionName = frame == null ? "<unknown>" : (String) frame.get(0);
		String file = frame == null ? (String) suspended.get("file") : (String) frame.get(1);
		// For the non-top stack frame, the line has been stored elsewhere.
		isTop = ix == stack.size() - 1;
		if (!isTop) {
			JSONArray nextFrame = (JSONArray) stack.get(ix + 1);
			line = ((Long) nextFrame.get(3)).intValue();
		}
		IFile resource = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(file));
		location = new SimpleLocation(vm, resource, line);
		location.setFunctionName(functionName);
		// For exceptions; where was it caught!?
		this.catchLine = (Number) suspended.get("catchLine");
	}

	@Override
	public VirtualMachine virtualMachine() {
		return vm;
	}

	@Override
	public Variable thisObject() {
		initVars();
		return thisVar;
	}

	private void initVars() {
		if (!inited) {
			inited = true;
			thisVar = new ReloadVariable(vm, this, "this");
			localVars = new ArrayList<ReloadVariable>();
			int scopeLine = catchLine == null ? location.lineNumber() : catchLine.intValue();
			LocalVariableScope scope = vm.getLocalVariableScope(location.scriptReference(), scopeLine);
			if (scope != null) {
				for (String localVar : scope.getLocalVariables()) {
					localVars.add(new ReloadVariable(vm, this, localVar));
				}
			}
		}
	}

	@Override
	public List variables() {
		initVars();
		ArrayList result = new ArrayList();
		result.addAll(localVars);
		return result;
	}

	@Override
	public Location location() {
		return location;
	}

	@Override
	public Value evaluate(String expression) {
		return parse(internalEvaluate(expression, stackDepth));
	}

	private String internalEvaluate(String expression, int stackDepth) {
		Integer stackDepthToSend = isTop ? null : stackDepth + 1;
		String valueStr;
		try {
			valueStr = "" + vm.evaluate(expression, stackDepthToSend);
		} catch (Exception e) {
			valueStr = null;
		}
		return valueStr;
	}

	private Value parse(String valueStr) {
		return vm.mirrorOf(valueStr);
	}
	
	public static String createDropToFrameExpression(int frameToDropTo, String expression) {
		if (Util.isEmpty(expression)) {
			// Noop.
			expression = "{}";
		}
		return MessageFormat.format("MoSyncDebugProtocol.doDropToFrame({0}, {1});", Integer.toString(frameToDropTo), expression);
	}

	public Value getValue(ReloadProperty property) {
		String symbolToEvaluate = property.getSymbolToEvaluate();
		String name = property.name();
		String metaFn = "this".equals(symbolToEvaluate) ? "evalThis" : "evalVar";
		String metaExpr = String.format("MoSyncDebugProtocol.%s(%s);", metaFn, symbolToEvaluate);

		String metaEvaluation = internalEvaluate(metaExpr, stackDepth);
		try {
			if (metaEvaluation != null) {
				JSONObject metaObject = (JSONObject) PARSER.parse(metaEvaluation);
				String type = (String) metaObject.get("type");
				String repr = "" + metaObject.get("repr");

				if ("object".equals(type) || "function".equals(type)) {
					Number oid = (Number) metaObject.get("oid");
					String className = (String) metaObject.get("clazz");
					JSONArray properties = (JSONArray) metaObject.get("properties");
					
					if (properties != null) {
						properties.addAll(property.getIntrinsicProperties());
					}
					
					ArrayList<ReloadProperty> generatedProperties = new ArrayList<ReloadProperty>();
					boolean hasArrayProperty = false;
					boolean hasLengthProperty = false;

					HashSet<String> unDuplicateSet = new HashSet<String>();
					for (int i = 0; properties != null && i < properties.size(); i++) {
						String propertyName = (String) properties.get(i);
						hasLengthProperty |= "length".equals(propertyName);
						boolean isArrayProperty = Character.isDigit(propertyName.charAt(0));
						hasArrayProperty |= isArrayProperty;
						boolean isInternalProperty = "____oid".equals(propertyName);
						if (!isInternalProperty && !unDuplicateSet.contains(propertyName)) {
							unDuplicateSet.add(propertyName);
							generatedProperties.add(isArrayProperty ?
									new ReloadArrayProperty(vm, this, property, propertyName) : 
								    new ReloadProperty(vm, this, property, propertyName));
						}
					}
					
					boolean isArray = hasArrayProperty; //|| hasLengthProperty;
					ReloadObjectReference ref = isArray ? new ReloadArrayReference(vm, repr, oid) : new ReloadObjectReference(vm, repr, className, oid);
					for (ReloadProperty generatedProperty : generatedProperties) {
						ref.addProperty(generatedProperty);
					}
					return ref;
				} else if ("null".equals(type)) {
					return vm.mirrorOfNull();
				} else if ("string".equals(type)) {
					return vm.mirrorOf(repr);
				} else if ("number".equals(type)) {
					Number reprNum = Double.parseDouble(repr);
					return vm.mirrorOf(reprNum);
				} else if ("boolean".equals(type)) {
					Boolean reprBool = Boolean.parseBoolean(repr);
					return vm.mirrorOf(reprBool);
				} else {
					return vm.mirrorOfUndefined();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// IGNORE.
		}
		return vm.mirrorOfUndefined();
	}

}
