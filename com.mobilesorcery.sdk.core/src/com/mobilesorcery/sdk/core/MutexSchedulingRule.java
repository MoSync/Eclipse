/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.core;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class MutexSchedulingRule implements ISchedulingRule {
    public boolean isConflicting(ISchedulingRule rule) {
        return rule == this;
    }

    public boolean contains(ISchedulingRule rule) {
        return rule == this;
    }
}