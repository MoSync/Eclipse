package com.mobilesorcery.sdk.ui;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class UpdateListener implements Listener {

    public interface IUpdatableControl {
        public void updateUI();
    }
    
    private IUpdatableControl c;
    private boolean active = true;
    
    public UpdateListener(IUpdatableControl c) {
        this.c = c;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void handleEvent(Event event) {
        if (active) { 
            c.updateUI();
        }
    }

}
