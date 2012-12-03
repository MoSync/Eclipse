package com.mobilesorcery.sdk.html5.ui;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.IProcessConsole;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.html5.Html5Plugin;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.live.JSODDServer;
import com.mobilesorcery.sdk.ui.MosyncUIPlugin;

/**
 * An evaluation handler.
 * (JSDT only supports evaluation at breakpoints, we try to support more than that)
 * @author Mattias Bybro
 *
 */
public class EvaluateHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		IEditorInput input = editor.getEditorInput();
		IProject project = null;
		IResource resource = (IResource) input.getAdapter(IResource.class);
		if (resource != null) {
			project = resource.getProject();
		}

		ISelection selection = editor.getSite().getSelectionProvider().getSelection();
		if (project != null && selection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection) selection;
			final String expression = textSelection.getText();
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					// NOT THE BUILDER CONSOLE!?
					IProcessConsole console = CoreMoSyncPlugin.getDefault().createConsole(MoSyncBuilder.CONSOLE_ID);
					JSODDServer server = Html5Plugin.getDefault().getReloadServer();
					Set<Integer> sessionIds = server.getSessions();
					for (Integer sessionId : sessionIds) {
						try {
							// JSODDSTREAMSPROXY!?
							Object result = server.evaluate(sessionId, expression, null);
							String sessionIdStr = "";
							if (sessionIds.size() > 1) {
								ReloadVirtualMachine vm = server.getVM(sessionId);
								sessionIdStr = vm.getRemoteAddr();
							}
							console.addMessage(sessionIdStr + ">" + result);
						} catch (Exception e) {
							// IGNORE!
						}
					}
				}

			});
			t.start();

		}
		return null;
	}

}
