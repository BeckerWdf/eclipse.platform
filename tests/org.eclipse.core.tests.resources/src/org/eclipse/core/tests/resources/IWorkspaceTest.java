package org.eclipse.core.tests.resources;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.EclipseWorkspaceTest;

public class IWorkspaceTest extends EclipseWorkspaceTest {
public IWorkspaceTest() {
}
public IWorkspaceTest(String name) {
	super(name);
}
public String[] defineHierarchy() {
	return new String[] {
		"/",
		"/Project/",
		"/Project/Folder/",
		"/Project/Folder/File",
	};
}
public static Test suite() {
	TestSuite suite = new TestSuite(IWorkspaceTest.class);
	return suite;
}
protected void tearDown() throws Exception {
	getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
	ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
}
/**
 * Tests handling of runnables that throw OperationCanceledException.
 */
public void testCancelRunnable() {
	boolean cancelled = false;
	try {
		getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) {
				throw new OperationCanceledException();
			}
		}, getMonitor());
	} catch (CoreException e) {
		fail("1.0", e);
	} catch (OperationCanceledException e) {
		cancelled = true;
	}
	assertTrue("2.0", cancelled);
}
/**
 * Performs black box testing of the following method:
 * 		IStatus copy([IResource, IPath, boolean, IProgressMonitor)
 * See also testMultiCopy()
 */
public void testCopy() throws CoreException {
	IResource[] resources = buildResources();
	IProject project = (IProject)resources[1];
	IFolder folder = (IFolder)resources[2];
	IFile file = (IFile)resources[3];
	IFile file2 = folder.getFile("File2");
	IFile file3 = folder.getFile("File3");
	IFolder folder2 = project.getFolder("Folder2");
	IFolder folderCopy = folder2.getFolder("Folder");
	IFile fileCopy = folder2.getFile("File");
	IFile file2Copy = folder2.getFile("File2");
	IFile file3Copy = folder2.getFile("File3");

	/********** FAILURE CASES ***********/

	//project not open
	try {
		getWorkspace().copy(new IResource[] {file}, folder.getFullPath(), false, getMonitor());
		fail("0.0");
	} catch (CoreException e) {
	}
	createHierarchy();

	//copy to bogus destination
	try {
		getWorkspace().copy(new IResource[] {file}, folder2.getFullPath().append("figment"), false, getMonitor());
		fail("1.0");
	} catch (CoreException e) {
	}

	//copy to non-existent destination
	try {
		getWorkspace().copy(new IResource[] {file}, folder2.getFullPath(), false, getMonitor());
		fail("1.1");
	} catch (CoreException e) {
	}

	//create the destination
	try {
		folder2.create(false, true, getMonitor());
	} catch (CoreException e) {
		fail("1.2", e);
	}

	//source file doesn't exist
	try {
		getWorkspace().copy(new IResource[] {file2}, folder2.getFullPath(), false, getMonitor());
		fail("1.3");
	} catch (CoreException e) {
	}

	//some source files don't exist
	try {
		getWorkspace().copy(new IResource[] {file, file2}, folder2.getFullPath(), false, getMonitor());
		fail("1.4");
	} catch (CoreException e) {
	}

	//make sure the first copy worked
	assertTrue("1.5", fileCopy.exists());
	try {
		fileCopy.delete(true, getMonitor());
	} catch (CoreException e) {
		fail("1.6", e);
	}

	// create the files
	IFile projectFile = project.getFile("ProjectPhile");
	try {
		file2.create(getRandomContents(), false, getMonitor());
		file3.create(getRandomContents(), false, getMonitor());
		projectFile.create(getRandomContents(), false, getMonitor());
	} catch (CoreException e) {
		fail("1.7", e);
	}

	//source files aren't siblings
	try {
		getWorkspace().copy(new IResource[] {file, projectFile}, folder2.getFullPath(), false, getMonitor());
		fail("1.8");
	} catch (CoreException e) {
	}

	//source files contains duplicates	
	try {
		getWorkspace().copy(new IResource[] {file, file2, file}, folder2.getFullPath(), false, getMonitor());
		fail("1.9");
	} catch (CoreException e) {
	}

	//source can't be prefix of destination
	try {
		IFolder folder3 = folder2.getFolder("Folder3");
		folder3.create(false, true, getMonitor());
		getWorkspace().copy(new IResource[] {folder2}, folder3.getFullPath(), false, getMonitor());
		fail("2.0");
	} catch (CoreException e) {
	}

	//target exists
	try {
		file2Copy.create(getRandomContents(), false, getMonitor());
		getWorkspace().copy(new IResource[] {file, file2}, folder2.getFullPath(), false, getMonitor());
		fail("2.1");
	} catch (CoreException e) {
	}
	ensureDoesNotExistInWorkspace(file2Copy);
	ensureDoesNotExistInFileSystem(file2Copy);

	//make sure the first copy worked
	fileCopy = folder2.getFile("File");
	assertTrue("2.2", fileCopy.exists());
	try {
		fileCopy.delete(true, getMonitor());
	} catch (CoreException e) {
		fail("2.3", e);
	}

	//resource out of sync with filesystem
	try {
		// Need to pause to make sure it gets a new timestamp.
		// Granularity of timestamps is not great
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		java.io.File osFile = Platform.getLocation().append(file.getFullPath()).toFile();
		java.io.RandomAccessFile raf = new java.io.RandomAccessFile(osFile, "rw");
		raf.seek(raf.length());
		raf.write(1);
		raf.write(2);
		raf.write(3);
		raf.close();
	} catch (java.io.IOException e) {
		fail("2.4", e);
	}
	try {
		getWorkspace().copy(new IResource[] {file}, folder2.getFullPath(), false, getMonitor());
		fail("2.5");
	} catch (CoreException e) {
	}

	// make sure "file" is in sync.
	file.refreshLocal(IResource.DEPTH_ZERO, null);
	/********** NON FAILURE CASES ***********/

	//empty resource list
	try {
		getWorkspace().copy(new IResource[] {}, folder2.getFullPath(), false, getMonitor());
	} catch (CoreException e) {
		fail("3.0", e);
	} catch (ArrayIndexOutOfBoundsException e) {
		fail("Fails because of 1FTXL69", e);
	}

	//copy single file
	try {
		getWorkspace().copy(new IResource[] {file}, folder2.getFullPath(), false, getMonitor());
	} catch (CoreException e) {
		fail("3.1", e);
	}
	assertTrue("3.2", fileCopy.exists());
	ensureDoesNotExistInWorkspace(fileCopy);
	ensureDoesNotExistInFileSystem(fileCopy);

	//copy two files
	try {
		getWorkspace().copy(new IResource[] {file, file2}, folder2.getFullPath(), false, getMonitor());
	} catch (CoreException e) {
		fail("3.3", e);
	}
	assertTrue("3.4", fileCopy.exists());
	assertTrue("3.5", file2Copy.exists());
	ensureDoesNotExistInWorkspace(fileCopy);
	ensureDoesNotExistInWorkspace(file2Copy);
	ensureDoesNotExistInFileSystem(fileCopy);
	ensureDoesNotExistInFileSystem(file2Copy);

	//copy a folder
	try {
		getWorkspace().copy(new IResource[] {folder}, folder2.getFullPath(), false, getMonitor());
	} catch (CoreException e) {
		fail("3.6", e);
	}
	assertTrue("3.7", folderCopy.exists());
	try {
		assertTrue("3.8", folderCopy.members().length > 0);
	} catch (CoreException e) {
		fail("3.9", e);
	}
	ensureDoesNotExistInWorkspace(folderCopy);
	ensureDoesNotExistInFileSystem(folderCopy);
}
/**
 * Performs black box testing of the following method:
 * 		IStatus delete([IResource, boolean, IProgressMonitor)
 */
public void testDelete() throws CoreException {
	IResource[] resources = buildResources();
	IProject project = (IProject)resources[1];
	IFolder folder = (IFolder)resources[2];
	IFile file = (IFile)resources[3];

	//delete non-existent resources
	assertTrue(getWorkspace().delete(new IResource[] {project, folder, file}, false, getMonitor()).isOK());
	assertTrue(getWorkspace().delete(new IResource[] {file}, false, getMonitor()).isOK());
	assertTrue(getWorkspace().delete(new IResource[] {}, false, getMonitor()).isOK());
	createHierarchy();

	//delete existing resources
	resources = new IResource[] {file, project, folder};
	assertTrue(getWorkspace().delete(resources, false, getMonitor()).isOK());
	//	assertDoesNotExistInFileSystem(resources);
	assertDoesNotExistInWorkspace(resources);
	createHierarchy();
	resources = new IResource[] {file};
	assertTrue(getWorkspace().delete(resources, false, getMonitor()).isOK());
	assertDoesNotExistInFileSystem(resources);
	assertDoesNotExistInWorkspace(resources);
	file.create(getRandomContents(), false, getMonitor());
	resources = new IResource[] {};
	assertTrue(getWorkspace().delete(resources, false, getMonitor()).isOK());
	assertDoesNotExistInFileSystem(resources);
	assertDoesNotExistInWorkspace(resources);
	createHierarchy();

	//delete a combination of existing and non-existent resources
	IProject fakeProject = getWorkspace().getRoot().getProject("pigment");
	IFolder fakeFolder = fakeProject.getFolder("ligament");
	resources = new IResource[] {file, folder, fakeFolder, project, fakeProject};
	assertTrue(getWorkspace().delete(resources, false, getMonitor()).isOK());
	//	assertDoesNotExistInFileSystem(resources);
	assertDoesNotExistInWorkspace(resources);
	createHierarchy();
	resources = new IResource[] {fakeProject, file};
	assertTrue(getWorkspace().delete(resources, false, getMonitor()).isOK());
	assertDoesNotExistInFileSystem(resources);
	assertDoesNotExistInWorkspace(resources);
	file.create(getRandomContents(), false, getMonitor());
	resources = new IResource[] {fakeProject};
	assertTrue(getWorkspace().delete(resources, false, getMonitor()).isOK());
	//	assertDoesNotExistInFileSystem(resources);
	assertDoesNotExistInWorkspace(resources);
	createHierarchy();
}
/**
 * Performs black box testing of the following method:
 *     IPath getPluginStateLocation(IPluginDescriptor)
 */
public void testGetPluginStateLocation() throws CoreException {
	IPluginDescriptor coreDescriptor = Platform.getPluginRegistry().getPluginDescriptor("org.eclipse.core.resources");
	IPluginDescriptor builderDescriptor = Platform.getPluginRegistry().getPluginDescriptor("org.eclipse.core.tests.resources");
	assertTrue("0.9", builderDescriptor != null);

	IPath coreLocation = coreDescriptor.getPlugin().getStateLocation();
	assertTrue("1.0", coreLocation.toFile().exists());

	IPath builderLocation = builderDescriptor.getPlugin().getStateLocation();
	assertTrue("1.1", builderLocation.toFile().exists());
}
/**
 * Performs black box testing of the following method:
 *     IStatus move([IResource, IPath, boolean, IProgressMonitor)
 */
public void testMove() throws CoreException {
	/* create folders and files */
	IProject project = getWorkspace().getRoot().getProject("project");
	project.create(null);
	project.open(null);
	IFolder folder = project.getFolder("folder");
	ensureExistsInWorkspace(folder, true);
	ensureExistsInFileSystem(folder);
	IFile file = project.getFile("file.txt");
	ensureExistsInWorkspace(file, true);
	ensureExistsInFileSystem(file);
	IFile anotherFile = project.getFile("anotherFile.txt");
	ensureExistsInWorkspace(anotherFile, true);
	ensureExistsInFileSystem(anotherFile);
	IFile oneMoreFile = project.getFile("oneMoreFile.txt");
	ensureExistsInWorkspace(oneMoreFile, true);
	ensureExistsInFileSystem(oneMoreFile);

	/* normal case */
	IResource[] resources = {file, anotherFile, oneMoreFile};
	getWorkspace().move(resources, folder.getFullPath(), true, getMonitor());
	assertTrue("1.1", !file.exists());
	assertTrue("1.2", !anotherFile.exists());
	assertTrue("1.3", !oneMoreFile.exists());
	assertTrue("1.4", folder.getFile(file.getName()).exists());
	assertTrue("1.5", folder.getFile(anotherFile.getName()).exists());
	assertTrue("1.6", folder.getFile(oneMoreFile.getName()).exists());

	/* test duplicates */
	resources = new IResource[] {folder.getFile(file.getName()), folder.getFile(anotherFile.getName()), folder.getFile(oneMoreFile.getName()), folder.getFile(oneMoreFile.getName())};
	IStatus status = getWorkspace().move(resources, project.getFullPath(), true, getMonitor());
	assertTrue("2.1", status.isOK());
	assertTrue("2.3", file.exists());
	assertTrue("2.4", anotherFile.exists());
	assertTrue("2.5", oneMoreFile.exists());
	assertTrue("2.6", !folder.getFile(file.getName()).exists());
	assertTrue("2.7", !folder.getFile(anotherFile.getName()).exists());
	assertTrue("2.8", !folder.getFile(oneMoreFile.getName()).exists());

	/* test no simblings */
	resources = new IResource[] {file, anotherFile, oneMoreFile, project};
	boolean ok = false;
	try {
		getWorkspace().move(resources, folder.getFullPath(), true, getMonitor());
	} catch (CoreException e) {
		ok = true;
		status = e.getStatus();
	}
	assertTrue("3.0", ok);
	assertTrue("3.1", !status.isOK());
	assertTrue("3.2", status.getChildren().length == 1);
	assertTrue("3.3", !file.exists());
	assertTrue("3.4", !anotherFile.exists());
	assertTrue("3.5", !oneMoreFile.exists());
	assertTrue("3.6", folder.getFile(file.getName()).exists());
	assertTrue("3.7", folder.getFile(anotherFile.getName()).exists());
	assertTrue("3.8", folder.getFile(oneMoreFile.getName()).exists());

	/* inexisting resource */
	resources = new IResource[] {folder.getFile(file.getName()), folder.getFile(anotherFile.getName()), folder.getFile("inexisting"), folder.getFile(oneMoreFile.getName())};
	ok = false;
	try {
		getWorkspace().move(resources, project.getFullPath(), true, getMonitor());
	} catch (CoreException e) {
		ok = true;
		status = e.getStatus();
	}
	assertTrue("4.0", ok);
	assertTrue("4.1", !status.isOK());
	assertTrue("4.3", file.exists());
	assertTrue("4.4", anotherFile.exists());
	assertTrue("4.5", oneMoreFile.exists());
	assertTrue("4.6", !folder.getFile(file.getName()).exists());
	assertTrue("4.7", !folder.getFile(anotherFile.getName()).exists());
	assertTrue("4.8", !folder.getFile(oneMoreFile.getName()).exists());
}
/**
 * Another test method for IWorkspace.copy().  See also testCopy
 */
public void testMultiCopy() throws CoreException {
	/* create common objects */
	IResource[] resources = buildResources();
	IProject project = (IProject)resources[1];
	IFolder folder = (IFolder)resources[2];

	/* create folder and file */
	ensureExistsInWorkspace(folder, true);
	ensureExistsInFileSystem(folder);
	IFile file1 = project.getFile("file.txt");
	ensureExistsInWorkspace(file1, true);
	ensureExistsInFileSystem(file1);
	IFile anotherFile = project.getFile("anotherFile.txt");
	ensureExistsInWorkspace(anotherFile, true);
	ensureExistsInFileSystem(anotherFile);
	IFile oneMoreFile = project.getFile("oneMoreFile.txt");
	ensureExistsInWorkspace(oneMoreFile, true);
	ensureExistsInFileSystem(oneMoreFile);
	
	/* normal case */
	resources = new IResource[] {file1, anotherFile, oneMoreFile};
	getWorkspace().copy(resources, folder.getFullPath(), true, getMonitor());
	assertTrue("1.1", file1.exists());
	assertTrue("1.2", anotherFile.exists());
	assertTrue("1.3", oneMoreFile.exists());
	assertTrue("1.4", folder.getFile(file1.getName()).exists());
	assertTrue("1.5", folder.getFile(anotherFile.getName()).exists());
	assertTrue("1.6", folder.getFile(oneMoreFile.getName()).exists());
	ensureDoesNotExistInWorkspace(folder.getFile(file1.getName()));
	ensureDoesNotExistInWorkspace(folder.getFile(anotherFile.getName()));
	ensureDoesNotExistInWorkspace(folder.getFile(oneMoreFile.getName()));
	ensureDoesNotExistInFileSystem(folder.getFile(file1.getName()));
	ensureDoesNotExistInFileSystem(folder.getFile(anotherFile.getName()));
	ensureDoesNotExistInFileSystem(folder.getFile(oneMoreFile.getName()));

	/* test duplicates */
	resources = new IResource[] {file1, anotherFile, oneMoreFile, file1};
	getWorkspace().copy(resources, folder.getFullPath(), true, getMonitor());
	assertTrue("2.2", file1.exists());
	assertTrue("2.3", anotherFile.exists());
	assertTrue("2.4", oneMoreFile.exists());
	assertTrue("2.5", folder.getFile(file1.getName()).exists());
	assertTrue("2.6", folder.getFile(anotherFile.getName()).exists());
	assertTrue("2.7", folder.getFile(oneMoreFile.getName()).exists());
	ensureDoesNotExistInWorkspace(folder.getFile(file1.getName()));
	ensureDoesNotExistInWorkspace(folder.getFile(anotherFile.getName()));
	ensureDoesNotExistInWorkspace(folder.getFile(oneMoreFile.getName()));
	ensureDoesNotExistInFileSystem(folder.getFile(file1.getName()));
	ensureDoesNotExistInFileSystem(folder.getFile(anotherFile.getName()));
	ensureDoesNotExistInFileSystem(folder.getFile(oneMoreFile.getName()));

	/* test no siblings */
	resources = new IResource[] {file1, anotherFile, oneMoreFile, project};
	IStatus status = null;
	boolean ok = false;
	try {
		getWorkspace().copy(resources, folder.getFullPath(), true, getMonitor());
	} catch (CoreException e) {
		ok = true;
		status = e.getStatus();
	}
	assertTrue("3.0", ok);
	assertTrue("3.1", !status.isOK());
	assertTrue("3.2", status.getChildren().length == 1);
	assertTrue("3.3", file1.exists());
	assertTrue("3.4", anotherFile.exists());
	assertTrue("3.5", oneMoreFile.exists());
	assertTrue("3.6", folder.getFile(file1.getName()).exists());
	assertTrue("3.7", folder.getFile(anotherFile.getName()).exists());
	assertTrue("3.8", folder.getFile(oneMoreFile.getName()).exists());
	ensureDoesNotExistInWorkspace(folder.getFile(file1.getName()));
	ensureDoesNotExistInWorkspace(folder.getFile(anotherFile.getName()));
	ensureDoesNotExistInWorkspace(folder.getFile(oneMoreFile.getName()));
	ensureDoesNotExistInFileSystem(folder.getFile(file1.getName()));
	ensureDoesNotExistInFileSystem(folder.getFile(anotherFile.getName()));
	ensureDoesNotExistInFileSystem(folder.getFile(oneMoreFile.getName()));

	/* inexisting resource */
	resources = new IResource[] {file1, anotherFile, project.getFile("inexisting"), oneMoreFile};
	ok = false;
	try {
		getWorkspace().copy(resources, folder.getFullPath(), true, getMonitor());
	} catch (CoreException e) {
		ok = true;
		status = e.getStatus();
	}
	assertTrue("4.0", ok);
	assertTrue("4.1", !status.isOK());
	assertTrue("4.2", file1.exists());
	assertTrue("4.3", anotherFile.exists());
	assertTrue("4.4", oneMoreFile.exists());
	assertTrue("4.5", folder.getFile(file1.getName()).exists());
	assertTrue("4.6", folder.getFile(anotherFile.getName()).exists());
	assertTrue("4.7 Fails because of 1FVFOOQ", folder.getFile(oneMoreFile.getName()).exists());

	/* copy projects should not be allowed */
	IResource destination = getWorkspace().getRoot().getProject("destination");
	ok = false;
	try {
		getWorkspace().copy(new IResource[] {project}, destination.getFullPath(), true, getMonitor());
	} catch (CoreException e) {
		ok = true;
		status = e.getStatus();
	}
	assertTrue("5.0", ok);
	assertTrue("5.1", !status.isOK());
	assertTrue("5.2", status.getChildren().length == 1);
}
public void testMultiCreation() throws Throwable {
	final IProject project = getWorkspace().getRoot().getProject("bar");
	final IResource[] resources = buildResources(project, new String[] {"a/", "a/b"});
	IWorkspaceRunnable body = new IWorkspaceRunnable() {
		public void run(IProgressMonitor monitor) throws CoreException {
			project.create(null);
			project.open(null);
			// define an operation which will create a bunch of resources including a project.
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				switch (resource.getType()) {
					case IResource.FILE :
					 	((IFile) resource).create(null, false, getMonitor());
						break;
					case IResource.FOLDER :
					 	((IFolder) resource).create(false, true, getMonitor());
						break;
					case IResource.PROJECT :
					 	((IProject) resource).create(getMonitor());
						break;
				}
			}
		}
	};
	getWorkspace().run(body, getMonitor());
	assertExistsInWorkspace(project);
	assertExistsInWorkspace(resources);
}
public void testMultiDeletion() throws Throwable {
	IProject project = getWorkspace().getRoot().getProject("testProject");
	IResource[] before = buildResources(project, new String[] {"c/", "c/b/", "c/x", "c/b/y", "c/b/z"});
	ensureExistsInWorkspace(before, true);
	//
	assertExistsInWorkspace(before);
	getWorkspace().delete(before, true, getMonitor());
	assertDoesNotExistInWorkspace(before);
}
/**
 * Performs black box testing of the following method:
 *     IStatus validateName(String, int)
 */
public void testValidateName() {
	/* normal name */
	assertTrue("1.1", getWorkspace().validateName("abcdef", IResource.FILE).isOK());
	/* invalid characters (windows only) */
	if (BootLoader.getOS().equals(BootLoader.OS_WIN32)) {
		assertTrue("2.1", !(getWorkspace().validateName("dsa:sf", IResource.FILE).isOK()));
		assertTrue("2.2", !(getWorkspace().validateName("*dsasf", IResource.FILE).isOK()));
		assertTrue("2.3", !(getWorkspace().validateName("?dsasf", IResource.FILE).isOK()));
		assertTrue("2.4", !(getWorkspace().validateName("\"dsasf", IResource.FILE).isOK()));
		assertTrue("2.5", !(getWorkspace().validateName("<dsasf", IResource.FILE).isOK()));
		assertTrue("2.6", !(getWorkspace().validateName(">dsasf", IResource.FILE).isOK()));
		assertTrue("2.7", !(getWorkspace().validateName("|dsasf", IResource.FILE).isOK()));
		assertTrue("2.8", !(getWorkspace().validateName("\"dsasf", IResource.FILE).isOK()));
	}
	/* invalid characters on all platforms */
	assertTrue("2.9", !(getWorkspace().validateName("/dsasf", IResource.FILE).isOK()));
	assertTrue("2.10", !(getWorkspace().validateName("\\dsasf", IResource.FILE).isOK()));

	/* dots */
	assertTrue("3.1", !(getWorkspace().validateName(".", IResource.FILE).isOK()));
	assertTrue("3.2", !(getWorkspace().validateName("..", IResource.FILE).isOK()));
	assertTrue("3.3", !(getWorkspace().validateName("...", IResource.FILE).isOK()));
	assertTrue("3.4", !(getWorkspace().validateName("....", IResource.FILE).isOK()));
	assertTrue("3.5", getWorkspace().validateName("....abc", IResource.FILE).isOK());
	assertTrue("3.6", getWorkspace().validateName("abc....def", IResource.FILE).isOK());
	assertTrue("3.7", !(getWorkspace().validateName("abc....", IResource.FILE).isOK()));
}
/**
 * Performs black box testing of the following method:
 *     IStatus validatePath(String, int)
 */
public void testValidatePath() {
	/* normal path */
	assertTrue("1.1", getWorkspace().validatePath("/one/two/three/four/", IResource.FILE | IResource.FOLDER).isOK());

	/* invalid characters (windows only) */
	if (BootLoader.getOS().equals(BootLoader.OS_WIN32)) {
		assertTrue("2.1", !(getWorkspace().validatePath("\\dsa:sf", IResource.FILE).isOK()));
		assertTrue("2.2", !(getWorkspace().validatePath("/abc/*dsasf", IResource.FILE).isOK()));
		assertTrue("2.3", !(getWorkspace().validatePath("/abc/?dsasf", IResource.FILE).isOK()));
		assertTrue("2.4", !(getWorkspace().validatePath("/abc/\"dsasf", IResource.FILE).isOK()));
		assertTrue("2.5", !(getWorkspace().validatePath("/abc/<dsasf", IResource.FILE).isOK()));
		assertTrue("2.6", !(getWorkspace().validatePath("/abc/>dsasf", IResource.FILE).isOK()));
		assertTrue("2.7", !(getWorkspace().validatePath("/abc/|dsasf", IResource.FILE).isOK()));
		assertTrue("2.8", !(getWorkspace().validatePath("/abc/\"dsasf", IResource.FILE).isOK()));
	}

	/* dots */
	assertTrue("3.1", !(getWorkspace().validatePath("/abc/.../defghi", IResource.FILE).isOK()));
	assertTrue("3.2", !(getWorkspace().validatePath("/abc/..../defghi", IResource.FILE).isOK()));
	assertTrue("3.3", !(getWorkspace().validatePath("/abc/def..../ghi", IResource.FILE).isOK()));
	assertTrue("3.4", getWorkspace().validatePath("/abc/....def/ghi", IResource.FILE).isOK());
	assertTrue("3.5", getWorkspace().validatePath("/abc/def....ghi/jkl", IResource.FILE).isOK());

	/* test hiding incorrect characters using .. and device separator : */
	assertTrue("4.1", getWorkspace().validatePath("/abc/.?./../def/as", IResource.FILE).isOK());
	assertTrue("4.2", getWorkspace().validatePath("/abc/;*?\"'/../def/safd", IResource.FILE).isOK());
	assertTrue("4.3", !(getWorkspace().validatePath("/abc;*?\"':/def/asdf/sadf", IResource.FILE).isOK()));

	/* other invalid paths */
	assertTrue("5.1", !(getWorkspace().validatePath("/", IResource.FILE).isOK()));
	assertTrue("5.2", !(getWorkspace().validatePath("\\", IResource.FILE).isOK()));
	assertTrue("5.3", !(getWorkspace().validatePath("", IResource.FILE).isOK()));
	assertTrue("5.4", !(getWorkspace().validatePath("device:/abc/123", IResource.FILE).isOK()));

	/* test types / segments */
	assertTrue("6.6", getWorkspace().validatePath("/asf", IResource.PROJECT).isOK());
	assertTrue("6.7", !(getWorkspace().validatePath("/asf", IResource.FILE).isOK()));
	// FIXME: Should this be valid?
	assertTrue("6.8", getWorkspace().validatePath("/asf", IResource.PROJECT | IResource.FILE).isOK());
	assertTrue("6.10", getWorkspace().validatePath("/project/.metadata", IResource.FILE).isOK());
	// FIXME: Should this be valid?
	assertTrue("6.11", getWorkspace().validatePath("/.metadata/project", IResource.FILE).isOK());
}
}
