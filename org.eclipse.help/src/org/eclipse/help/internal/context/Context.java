/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Phil Loats (IBM Corp.) - fix to use only foundation APIs
 *******************************************************************************/
package org.eclipse.help.internal.context;

import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Context extends UAElement implements IContext {

	public static final String NAME = "context"; //$NON-NLS-1$
	public static final String ELEMENT_DESCRIPTION = "description"; //$NON-NLS-1$
	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	public static final String ATTRIBUTE_PLUGIN_ID = "pluginId"; //$NON-NLS-1$
	
	public Context(IContext src, String id) {
		super(NAME);
		setText(src.getText());
		setId(id);
		IHelpResource[] topics = src.getRelatedTopics();
		for (int i=0;i<topics.length;++i) {
			if (topics[i] instanceof ITopic) {
				appendChild(new Topic((ITopic)topics[i]));
			}
			else {
				Topic topic = new Topic();
				topic.setHref(topics[i].getHref());
				topic.setLabel(topics[i].getLabel());
				appendChild(topic);
			}
		}
	}
	
	public Context(Element src) {
		super(src);
	}

	public IHelpResource[] getRelatedTopics() {
		return (IHelpResource[])getChildren(IHelpResource.class);
	}
	
	public String getId() {
		return getAttribute(ATTRIBUTE_ID);
	}
	
	public String getText() {
		Node node = element.getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (ELEMENT_DESCRIPTION.equals(node.getNodeName())) {
					node.normalize();
					Node text = node.getFirstChild();
					if (text.getNodeType() == Node.TEXT_NODE) {
						return text.getNodeValue();
					}
				}
				return new String();
			}
			node = node.getNextSibling();
		}
		return new String();
	}
		
	public void setId(String id) {
		setAttribute(ATTRIBUTE_ID, id);
	}

	public void setText(String text) {
		Node node = element.getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (ELEMENT_DESCRIPTION.equals(node.getNodeName())) {
					element.removeChild(node);
					break;
				}
			}
			node = node.getNextSibling();
		}
		Document document = element.getOwnerDocument();
		Node description = element.appendChild(document.createElement(ELEMENT_DESCRIPTION));
		description.appendChild(document.createTextNode(text));
	}
}
