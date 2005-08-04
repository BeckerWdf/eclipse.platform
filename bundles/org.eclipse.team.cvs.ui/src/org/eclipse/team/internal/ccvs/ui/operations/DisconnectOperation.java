/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Disconnect the given projects from CVS control
 */
public class DisconnectOperation extends RepositoryProviderOperation {

	private boolean unmanage;

	public DisconnectOperation(IWorkbenchPart part, IProject[] projects, boolean unmanage) {
		super(part, projects);
		this.unmanage = unmanage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#execute(org.eclipse.team.internal.ccvs.core.CVSTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(
		CVSTeamProvider provider,
		IResource[] resources,
		boolean recurse, IProgressMonitor monitor)
		throws CVSException, InterruptedException {
		
		// This method will be invoked for each provider being disconnected
        monitor.beginTask(null, IProgressMonitor.UNKNOWN);
		IProject project = provider.getProject();
		try {
			RepositoryProvider.unmap(project);
		} catch (TeamException e) {
			throw CVSException.wrapException(e);
		}
		if (unmanage) {
			ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(project);
			cvsFolder.unmanage(monitor);
			EclipseSynchronizer.getInstance().deconfigure(project, Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
		}
        monitor.done();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
	 */
	protected String getTaskName() {
		return CVSUIMessages.DisconnectOperation_0; 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.DisconnectOperation_1, new String[] { provider.getProject().getName() }); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#canRunAsJob()
	 */
	public boolean canRunAsJob() {
		// Do not run in the background
		return false;
	}

}
