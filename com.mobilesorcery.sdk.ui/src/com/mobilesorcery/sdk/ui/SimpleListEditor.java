package com.mobilesorcery.sdk.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
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

	private TableViewer list;
	protected Button add;
	protected Button edit;
	protected Button remove;
	protected Button up;
	protected Button down;
	private ArrayList<T> input;
	private boolean editAfterAdd = true;
	private Listener buttonListener;

	/**
	 * A style constant for a rearrangeable list (ie up/down buttons
	 * are added if {@link #createButtons(Composite)} is not overridden).
	 */
	public final static int REARRANGEABLE = SWT.UP | SWT.DOWN;
	
	public SimpleListEditor(Composite parent, int style) {
		super(parent, style);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		list = new TableViewer(this);
		buttonListener = new Listener() {
			public void handleEvent(Event event) {
				buttonPressed(event.widget);
			}			
		};
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
	
	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		this.list.setLabelProvider(labelProvider);
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
	
	protected Button createButton(Composite main, String caption) {
		Button button = new Button(main, SWT.PUSH);
		button.setText(caption);
		button.setLayoutData(new GridData(GridData.FILL, SWT.DEFAULT, true, false, 1, 1));
		button.addListener(SWT.Selection, buttonListener);		
		return button;
	}

	protected void buttonPressed(Widget widget) {
		ISelection selection = getSelection();
		if (widget == add) {
			T added = add(getSelection().getFirstElement());
			if (added != null) {
				selection = new StructuredSelection(added);
			}
		} else if (widget == edit) {
			edit(getSelection().getFirstElement(), false);
		} else if (widget == remove) {
			remove(getSelection());
		} else if (widget == up) {
			up(getSelection());
		} else if (widget == down) {
			down(getSelection());
		}
		list.setInput(input);
		list.setSelection(selection);
		updateButtons(getSelection());
	}
	
	private IStructuredSelection getSelection() {
		return (IStructuredSelection) list.getSelection();
	}
	
	protected void updateButtons(IStructuredSelection selection) {
		boolean enabled = selection.size() == 1;
		Object object = selection.getFirstElement();
		enable(edit, enabled && canEdit(object));
		enable(remove, enabled && canRemove(object));	
		enable(up, enabled && !isFirst(object) && canMoveUp(object));
		enable(down, enabled && !isLast(object) && canMoveDown(object));
		enable(add, canAdd(object));
	}

	protected boolean canAdd(Object object) {
		return true;
	}

	protected boolean canMoveDown(Object object) {
		return true;
	}

	protected boolean canMoveUp(Object object) {
		return true;
	}

	protected boolean canRemove(Object object) {
		return true;
	}

	protected boolean canEdit(Object object) {
		return true;
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
		Object first = selection.getFirstElement();
		int ix = input.indexOf(first);
		if (ix != -1 && !isFirst(first)) {
			input.remove(ix);
			input.add(ix - 1, (T) first);
		}
	}
	
	/**
	 * Moves the current selection down. Clients may override,
	 * but in general should not need to.
	 * @param selection
	 */
	protected void down(IStructuredSelection selection) {
		Object first = selection.getFirstElement();
		int ix = input.indexOf(first);
		if (ix != -1 && !isLast(first)) {
			input.remove(ix);
			input.add(ix + 1, (T) first);
		}
	}
	
	private boolean isLast(Object element) {
		return !input.isEmpty() && input.get(input.size() - 1) == element;
	}
	
	private boolean isFirst(Object element) {
		return !input.isEmpty() && input.get(0) == element;		
	}

	/**
	 * Removes the current selection. Clients may override,
	 * but in general should not need to.
	 * @param selection
	 */
	protected void remove(IStructuredSelection selection) {
		Object[] toBeRemoved = selection.toArray();
		if (remove(toBeRemoved)) {
			for (Object element : selection.toArray()) {
				input.remove(element);
			}
		}
	}
	
	/**
	 * Removes the current selection. Clients may override.
	 * @param selection The objects to remove
	 * @return <code>true</code> if they should be removed
	 */
	protected boolean remove(Object[] selection) {
		return true;
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
	 * @param nextObject The object this object should be inserted before,
	 * or <code>null</code> if it should be added to the end of the list
	 */
	protected T add(Object nextObject) {
		T newObject = createObject();
		boolean doAdd = newObject != null;
		if (editAfterAdd) {
			doAdd = edit(newObject, true);
		}
		if (doAdd) {
			int ix = nextObject == null ? -1 : input.indexOf(nextObject);
			if (ix == -1) {
				input.add(newObject);
			} else {
				input.add(ix, newObject);
			}
			return newObject;
		}
		return null;
	}
	
	protected T createObject() {
		return null;
	}
	
	protected TableViewer getList() {
		return list;
	}

}
