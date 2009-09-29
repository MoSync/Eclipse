//package com.mobilesorcery.sdk.internal.debug;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//import org.eclipse.cdt.debug.core.cdi.CDIException;
//import org.eclipse.cdt.debug.core.cdi.ICDICondition;
//import org.eclipse.cdt.debug.core.cdi.ICDILocator;
//import org.eclipse.cdt.debug.mi.core.MIException;
//import org.eclipse.cdt.debug.mi.core.MISession;
//import org.eclipse.cdt.debug.mi.core.cdi.BreakpointManager;
//import org.eclipse.cdt.debug.mi.core.cdi.CdiResources;
//import org.eclipse.cdt.debug.mi.core.cdi.EventManager;
//import org.eclipse.cdt.debug.mi.core.cdi.MI2CDIException;
//import org.eclipse.cdt.debug.mi.core.cdi.Session;
//import org.eclipse.cdt.debug.mi.core.cdi.model.AddressBreakpoint;
//import org.eclipse.cdt.debug.mi.core.cdi.model.FunctionBreakpoint;
//import org.eclipse.cdt.debug.mi.core.cdi.model.LineBreakpoint;
//import org.eclipse.cdt.debug.mi.core.cdi.model.LocationBreakpoint;
//import org.eclipse.cdt.debug.mi.core.cdi.model.Target;
//import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
//import org.eclipse.cdt.debug.mi.core.command.MIBreakDisable;
//import org.eclipse.cdt.debug.mi.core.command.MIBreakInsert;
//import org.eclipse.cdt.debug.mi.core.output.MIBreakInsertInfo;
//import org.eclipse.cdt.debug.mi.core.output.MIBreakpoint;
//import org.eclipse.cdt.debug.mi.core.output.MIInfo;
//
//// Overrides one method in the standard bpm to allow for
//// asymmetrical mdb calls: -break-insert a.c:7 -> line 8 does not work
//// in standard bpm
//// 
//public class MoSyncBreakpointManager extends BreakpointManager {
//
//	public MoSyncBreakpointManager(Session session) {
//		super(session);
//	}
//
//	// Cut 'n' paste from super class.
//	public void setLocationBreakpoint (LocationBreakpoint bkpt) throws CDIException {
//		Target target = (Target)bkpt.getTarget();
//		MISession miSession = target.getMISession();
//		MIBreakInsert[] breakInserts = createMIBreakInsert(bkpt, miSession.isBreakpointsWithFullName());
//		List pointList = new ArrayList();
//		boolean restart = false;
//		try {
//			restart = suspendInferior(target);
//			CommandFactory factory = miSession.getCommandFactory();
//			boolean enable = bkpt.isEnabled();
//			for (int i = 0; i < breakInserts.length; i++) {
//				miSession.postCommand(breakInserts[i]);
//				MIBreakInsertInfo info = breakInserts[i].getMIBreakInsertInfo();
//				if (info == null) {
//					throw new CDIException(CdiResources.getString("cdi.Common.No_answer")); //$NON-NLS-1$
//				}
//				MIBreakpoint[] points = info.getMIBreakpoints();
//				if (points == null || points.length == 0) {
//					throw new CDIException(CdiResources.getString("cdi.BreakpointManager.Parsing_Error")); //$NON-NLS-1$
//				}
//				// Set
//				if (bkpt.getFile() != null && bkpt.getFile().length() > 0)
//				{
//					for (int j = 0; j < points.length; j++) {
//						points[j].setFile(bkpt.getFile());
//					}					
//				}
//				// Make sure that if the breakpoint was disable we create them disable.
//				if (!enable) {
//					int[] numbers = new int[points.length];
//					for (int j = 0; j < points.length; j++) {
//						numbers[j] = points[j].getNumber();
//					}
//					MIBreakDisable breakDisable = factory.createMIBreakDisable(numbers);
//					try {
//						miSession.postCommand(breakDisable);
//						MIInfo disableInfo = breakDisable.getMIInfo();
//						if (disableInfo == null) {
//							throw new CDIException(CdiResources.getString("cdi.Common.No_answer")); //$NON-NLS-1$
//						}
//					} catch (MIException e) {
//						throw new MI2CDIException(e);
//					}
//				}
//
//				pointList.addAll(Arrays.asList(points));
//			}
//		} catch (MIException e) {
//			try {
//				// Things did not go well remove all the breakpoints we've set before.
//				MIBreakpoint[] allPoints = (MIBreakpoint[]) pointList.toArray(new MIBreakpoint[pointList.size()]);
//				if (allPoints != null && allPoints.length > 0) {
//					deleteMIBreakpoints(target, allPoints);
//				}
//			} catch (CDIException cdie) {
//				// ignore this one;
//			}
//			throw new MI2CDIException(e);
//		} finally {
//			resumeInferior(target, restart);
//		}
//		MIBreakpoint[] allPoints = (MIBreakpoint[]) pointList.toArray(new MIBreakpoint[pointList.size()]);
//		bkpt.setMIBreakpoints(allPoints);
//	}
//
//	/**********************************
//	 * 
//	 * ALL METHODS BELOW THIS LINE ARE
//	 * COPIED VERBATIM FROM SUPER CLASS
//	 * MUST DO THAT, UNFORTUNATELY
//	 * 
//	 **********************************/
//	MIBreakInsert[] createMIBreakInsert(LocationBreakpoint bkpt, boolean fullPath) throws CDIException {
//		boolean hardware = bkpt.isHardware();
//		boolean temporary = bkpt.isTemporary();
//		String exprCond = null;
//		int ignoreCount = 0;
//		String[] threadIds = null;
//		StringBuffer line = new StringBuffer();
//
//		if (bkpt.getCondition() != null) {
//			ICDICondition condition = bkpt.getCondition();
//			exprCond = condition.getExpression();
//			ignoreCount = condition.getIgnoreCount();
//			threadIds = condition.getThreadIds();
//		}
//
//		if (bkpt.getLocator() != null) {
//			ICDILocator locator = bkpt.getLocator();
//			String file = locator.getFile();
//			if (file != null) {
//				if (fullPath==false) {
//					file = new File(file).getName();
//				}
//			}
//			String function = locator.getFunction();
//			int no = locator.getLineNumber();
//			if (bkpt instanceof LineBreakpoint) {
//				if (file != null && file.length() > 0) {
//					line.append(file).append(':');
//				}
//				line.append(no);				
//			} else if (bkpt instanceof FunctionBreakpoint) {
//				if (function != null && function.length() > 0) {
//					// if the function contains :: assume the user
//					// knows the exact funciton
//					int colon = function.indexOf("::"); //$NON-NLS-1$
//					if (colon != -1) {
//						line.append(function);
//					} else {
//						if (file != null && file.length() > 0) {
//							line.append(file).append(':');
//						}
//						// GDB does not seem to accept function arguments when
//						// we use file name:
//						// (gdb) break file.c:Test(int)
//						// Will fail, altought it can accept this
//						// (gdb) break file.c:main
//						// so fall back to the line number or
//						// just the name of the function if lineno is invalid.
//						int paren = function.indexOf('(');
//						if (paren != -1) {
//							if (no <= 0) {
//								String func = function.substring(0, paren);
//								line.append(func);
//							} else {
//								line.append(no);
//							}
//						} else {
//							line.append(function);
//						}
//					}
//				} else {
//					// ???
//					if (file != null && file.length() > 0) {
//						line.append(file).append(':');
//					}
//					if (no > 0) {
//						line.append(no);
//					}
//				}
//			} else if (bkpt instanceof AddressBreakpoint) {
//				line.append('*').append(locator.getAddress());				
//			} else {
//				// ???
//				if (file != null && file.length() > 0) {
//					line.append(file).append(':');
//				}
//				line.append(no);
//			}
//		}
//
//		MIBreakInsert[] miBreakInserts;
//		MISession miSession = ((Target)bkpt.getTarget()).getMISession();
//		CommandFactory factory = miSession.getCommandFactory();
//		if (threadIds == null || threadIds.length == 0) {
//			MIBreakInsert bi = factory.createMIBreakInsert(temporary, hardware, exprCond, ignoreCount, line.toString(), 0);
//			miBreakInserts = new MIBreakInsert[] { bi } ;
//		} else {
//			List list = new ArrayList(threadIds.length);
//			for (int i = 0; i < threadIds.length; i++) {
//				String threadId = threadIds[i];
//				int tid = 0;
//				if (threadId != null && threadId.length() > 0) {
//					try {
//						tid = Integer.parseInt(threadId);
//						list.add(factory.createMIBreakInsert(temporary, hardware, exprCond, ignoreCount, line.toString(), tid));
//					} catch (NumberFormatException e) {
//					}
//				}
//			}
//			miBreakInserts = (MIBreakInsert[]) list.toArray(new MIBreakInsert[list.size()]);
//		}
//		return miBreakInserts;
//	}
//
//}
