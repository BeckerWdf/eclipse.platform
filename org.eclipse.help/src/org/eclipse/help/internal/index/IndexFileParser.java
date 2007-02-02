/*******************************************************************************
 * Copyright (c) 2005, 2007 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation - 122967 [Help] Remote help system
 *******************************************************************************/
package org.eclipse.help.internal.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.internal.dynamic.DocumentReader;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class IndexFileParser extends DefaultHandler {

	private DocumentReader reader;
	
    public IndexContribution parse(IndexFile indexFile) throws IOException, SAXException, ParserConfigurationException {
		if (reader == null) {
			reader = new DocumentReader();
		}
		InputStream in = indexFile.getInputStream();
		if (in != null) {
			Index index = (Index)reader.read(in);
			IndexContribution contrib = new IndexContribution();
	    	contrib.setId('/' + indexFile.getPluginId() + '/' + indexFile.getFile());
			contrib.setIndex(index);
			contrib.setLocale(indexFile.getLocale());
			return contrib;
		}
    	else {
    		throw new FileNotFoundException();
    	}
    }
}
