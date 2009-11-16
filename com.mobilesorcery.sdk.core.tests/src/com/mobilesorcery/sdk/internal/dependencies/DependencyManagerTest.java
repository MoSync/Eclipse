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
package com.mobilesorcery.sdk.internal.dependencies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class DependencyManagerTest {

	@Test
	public void addAndRemove() {
		DependencyManager dependencies = new DependencyManager();
		Object from1 = "from1";
		Object to1 = "to1";
		Object to2 = "to2";
		dependencies.addDependency(from1, to1);
		
		assertEquals(1, dependencies.getDependenciesOf(from1).size());
		
		dependencies.addDependency(from1, to2);
		assertEquals(2, dependencies.getDependenciesOf(from1).size());
		assertEquals(1, dependencies.getReverseDependenciesOf(to1).size());
		assertEquals(1, dependencies.getReverseDependenciesOf(to2).size());

		dependencies.clearDependencies(from1);
		assertEquals(0, dependencies.getDependenciesOf(from1).size());
		assertEquals(0, dependencies.getReverseDependenciesOf(to1).size());
		assertEquals(0, dependencies.getReverseDependenciesOf(to2).size());
	}
	
	@Test
	public void getRecursiveDependencies() {
		DependencyManager dependencies = new DependencyManager();
		Object level0 = "0";
		Object level1 = "1";
		Object level2 = "2";
		dependencies.addDependency(level0, level1);
		dependencies.addDependency(level1, level2);
		
		Set reverseDependencies = dependencies.getReverseDependenciesOf(level2, DependencyManager.DEPTH_INFINITE);
		assertTrue(reverseDependencies.contains(level1));
		assertTrue(reverseDependencies.contains(level0));	
	}

	@Test
	public void circularRecursiveDependencies() {
		DependencyManager dependencies = new DependencyManager();
		Object level0 = "0";
		Object level1 = "1";
		Object level2 = "2";
		dependencies.addDependency(level0, level1);
		dependencies.addDependency(level1, level2);
		dependencies.addDependency(level2, level0);
		
		Set reverseDependencies = dependencies.getReverseDependenciesOf(level2, DependencyManager.DEPTH_INFINITE);
		assertTrue(reverseDependencies.contains(level1));
		assertTrue(reverseDependencies.contains(level0));	
		assertTrue(reverseDependencies.contains(level2));	
	}
	
	@Test
	public void getRecursiveDependenciesOfMany() {
		DependencyManager dependencies = new DependencyManager();
		Object level0 = "0";
		Object level1 = "1";
		Object level2 = "2";
		Object level3 = "3";
		Object level4 = "4";
		dependencies.addDependency(level1, level0);
		dependencies.addDependency(level3, level0);
		dependencies.addDependency(level4, level3);
		
		Set reverseDependencies = dependencies.getReverseDependenciesOf(Arrays.asList(new Object[] { level0, level3 }), DependencyManager.DEPTH_INFINITE);
		assertEquals(new HashSet(Arrays.asList(new Object[] { level1, level3, level4 })), reverseDependencies);		
	}
			
}
