package org.eclipse.core.tests.resources.usecase;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
/**
 * This session only performs a full save. The workspace should stay
 * the same.
 */
public class Snapshot3Test extends SnapshotTest {
public Snapshot3Test() {
}
public Snapshot3Test(String name) {
	super(name);
}
protected static String[] defineHierarchy1() {
	return Snapshot2Test.defineHierarchy1();
}
protected static String[] defineHierarchy2() {
	return Snapshot2Test.defineHierarchy2();
}
public static Test suite() {
	// we do not add the whole class because the order is important
	TestSuite suite = new TestSuite();
	suite.addTest(new Snapshot3Test("testVerifyPreviousSession"));
	suite.addTest(new Snapshot3Test("testSaveWorkspace"));
	return suite;
}
public void testSaveWorkspace() {
	try {
		getWorkspace().save(true, null);
	} catch (CoreException e) {
		fail("2.0", e);
	}
}
public void testVerifyPreviousSession() {
	// MyProject
	IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
	assertTrue("0.0", project.exists());
	assertTrue("0.1", project.isOpen());

	// verify existence of children
	IResource[] resources = buildResources(project, Snapshot2Test.defineHierarchy1());
	assertExistsInFileSystem("2.1", resources);
	assertExistsInWorkspace("2.2", resources);

	// Project2
	project = getWorkspace().getRoot().getProject(PROJECT_2);
	assertTrue("3.0", project.exists());
	assertTrue("3.1", project.isOpen());

	try {
		assertTrue("4.0", project.members().length == 3);
	} catch (CoreException e) {
		fail("4.1", e);
	}

	// verify existence of children
	resources = buildResources(project, Snapshot2Test.defineHierarchy2());
	assertExistsInFileSystem("5.1", resources);
	assertExistsInWorkspace("5.2", resources);
}
}
