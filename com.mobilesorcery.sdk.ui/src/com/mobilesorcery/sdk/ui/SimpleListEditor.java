package com.mobilesorcery.sdk.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

/**
 * <p>A simple class for creating an editor for editing lists;
 * with add, remove, edit as well as [optional] up and down buttons.</p>
 * <p>
 * Example:
 * <blockquote><code>
 * SimpleListEditor editor = new SimpleListEditor(parent, SimpleListEditor.REARRANGEABLE);
 * editor.setInput(myList);
 * [...]
 * List edited = editor.getEditedInput();
 * </code></blockquote>
 * </p>
 * @author Mattias Bybro
 *
 */
public class SimpleListEditor<T> extends Composite {

	private ListViewer list;
    private Button add;
	private Button edit;
	private Button remove;
	private Button up;
	private Button down;
	private ArrayList<T> input;
	private boolean editAfterAdd = true;
	private Listener buttonListener;

	/**
	 * A style constant for a rearrangeable list (ie up/down buttons
	 * are added if {@link #createButtons(Composite)} is not overridden).
	 */
	public final static int REARRANGEABLE = 0xffff;
	
	public SimpleListEditor(Composite parent, int style) {
		super(parent, style);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		list = new ListViewer(this);
		int numButtons = createButtons(this);
		list.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, numButtons));
		list.setContentProvider(new ListContentProvider());
		list.setLabelProvider(new LabelProvider());
		list.getControl().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateButtons(getSelection());
			}			
		});
		
		updateButtons(getSelection());
	}
	
	public void setEditAfterAdd(boolean editAfterAdd) {
		this.editAfterAdd = editAfterAdd;
	}

	/**
	 * Sets the initial list of objects.
	 * @param input
	 */
	public void setInput(List<T> input) {
		this.input = new ArrayList<T>(input);
		list.setInput(input);
	}
	
	public List<T> getEditedInput() {
		return input;
	}
	
	/**
	 * Creates the buttons located to the right of the 
	 * list viewer. Clients may override.
	 * @return The number of create buttons (used by layout manager)
	 */
	protected int createButtons(Composite main) {
		int numButtons = 3;
		buttonListener = new Listener() {
			public void handleEvent(Event event) {
				buttonPressed(event.widget);
			}			
		};
		
		add = createButton(main, "&Add");
		edit = createButton(main, "&Edit");
		remove = createButton(main, "&Remove");
		if ((getStyle() & REARRANGEABLE) != 0) {
			up = createButton(main, "&Up");
			down = createButton(main, "&Down");
			numButtons = 5;
		}
		
		return numButtons;
	}
	
	private Button createButton(Composite main, String caption) {
		Button button = new Button(main, SWT.PUSH);
		button.setText(caption);
		button.setLayoutData(new GridData(GridData.FILL, SWT.DEFAULT, true, false, 1, 1));
		button.addListener(SWT.Selection, buttonListener);		
		return button;
	}

	protected void buttonPressed(Widget widget) {
		if (widget == add) {
			add();
		} else if (widget == edit) {
			edit(getSelection().getFirstElement(), false);
		} else if (widget == remove) {
			remove(getSelection());
		} else if (widget == up) {
			up(getSelection());
		} else if (widget == down) {
			down(getSelection());
		}
		list.refresh();
	}
	
	private IStructuredSelection getSelection() {
		return (IStructuredSelection) list.getSelection();
	}
	
	protected void updateButtons(IStructuredSelection selection) {
		boolean enabled = !selection.isEmpty();
		enable(edit, enabled);
		enable(remove, enabled);	
		enable(up, enabled);
		enable(down, enabled);
	}

	private void enable(Control control, boolean enabled) {
		if (control != null) {
			control.setEnabled(enabled);
		}
	}

	/**
	 * Moves the current selection up. Clients may override,
	 * but in general should not need to. 
	 * @param selection
	 */
	protected void up(IStructuredSelection selection) {
		throw new UnsupportedOperationException("TBD");
	}
	
	/**
	 * Moves the current selection up. Clients may override,
	 * but in general should not need to.
	 * @param selection
	 */
	protected void down(IStructuredSelection selection) {
		throw new UnsupportedOperationException("TBD");
	}

	/**
	 * Removes the current selection. Clients may override,
	 * but in general should not need to.
	 * @param selection
	 */
	protected void remove(IStructuredSelection selection) {
		for (Object element : selection.toArray()) {
			input.remove(element);
		}
		list.refresh();
	}

	/**
	 * Edits the current selection. Clients should override.
	 * @param selection
	 * @param add Whether this method was called after an "add" request
	 * @return <code>true</code> if it should be added (only applicable for "add" requests);
	 */
	protected boolean edit(Object selection, boolean add) {
		return true;
	}

	/**
	 * Adds an element to the list. Clients may override,
	 * but in general should not need to. Instead, override
	 * the {@link #createObject()} method.
	 */
	protected void add() {
		T newObject = createObject();
		boolean doAdd = newObject != null;
		if (editAfterAdd) {
			doAdd = edit(newObject, true);
		}
		if (doAdd) {
			input.add(newObject);
		}
		list.setInput(input);
	}
	
	protected T createObject() {
		return null;
	}

}
