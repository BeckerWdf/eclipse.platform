/**********************************************************************
 * Copyright (c) 2000,2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.internal.watson;

import org.eclipse.core.runtime.IPath;

/**
 * An interface for objects which can visit an element of
 * an element tree and access that element's node info.
 * @see ElementTreeIterator
 */
public interface IElementContentVisitor {
	/**
	 * Callback interface so visitors can request the path of the object they
	 * are visiting. This avoids creating paths when they are not needed.
	 */
	interface IPathRequestor {
		public IPath requestPath();
	}
/** Visits a node (element).
 * <p> Note that <code>elementContents</code> is equal to<code>tree.
 * getElement(elementPath)</code> but takes no time.
 * @param tree the element tree being visited
 * @param elementContents the object at the node being visited on this call
 * @param requestor callback object for requesting the path of the object being
 * visited.
 */
public void visitElement(ElementTree tree, IPathRequestor requestor, Object elementContents);
}
