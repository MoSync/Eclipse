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
package com.mobilesorcery.sdk.internal.debug;

import org.eclipse.cdt.debug.mi.core.output.MIVar;

import com.mobilesorcery.sdk.core.Util;

/**
 * We need this one in case no "exp" is returned from MDB.
 * The standard answer would something like this:
 * <blockquote><code>-var-list-children var2
 *  ^done,numchild="6",children={child={name="var2.0",exp="0",numchild="0",type="char"},child={name="var2.1",exp="1",numchild="0",type="char"},child={name="var2.2",exp="2",numchild="0",type="char"},child={name="var2.3",exp="3",numchild="0",type="char"},child={name="var2.4",exp="4",numchild="0",type="char"},child={name="var2.5",exp="5",numchild="0",type="char"}}
 * </code></blockquote>
 * But early versions of MDB skipped the exp attribute, which
 * caused all kinds of weird behaviour.
 * @author Mattias Bybro
 *
 */
public class HackMIVar extends MIVar {

	private MIVar miVar;

	public HackMIVar(MIVar miVar) {
		super(null, 0, null);
		this.miVar = miVar;
	}
	
	public String getVarName() {
		return miVar.getVarName();
	}

	public String getType() {
		return miVar.getType();
	}

	public int getNumChild() {
		return miVar.getNumChild();
	}

	public String getExp() {
		if (Util.isEmpty(miVar.getExp())) {
			String name = miVar.getVarName();
			int lix = name.lastIndexOf('.');
			if (lix == -1) {
				return name;
			} else {
				String result = name.substring(lix + 1);
				if (result.startsWith("__")) {
					result = '_' + result;
				}
				return result;
			}
		}
		
		return miVar.getExp();
	}


}
