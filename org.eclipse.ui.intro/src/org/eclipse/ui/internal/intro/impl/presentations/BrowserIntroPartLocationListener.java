/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.intro.impl.presentations;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;

/**
 * A Location Listener that knows how to intercept OOBE action URLs. It also
 * knows how to update UI navigation hisutory.
 */
public class BrowserIntroPartLocationListener implements LocationListener {

    private BrowserIntroPartImplementation implementation;

    /**
     * Takes the implementation as an input.
     */
    public BrowserIntroPartLocationListener(
            BrowserIntroPartImplementation implementation) {
        this.implementation = implementation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.browser.LocationListener#changed(org.eclipse.swt.browser.LocationEvent)
     */
    public void changed(LocationEvent event) {
        // no-op
    }

    /**
     * Intercept any LocationEvents on the browser. If the event location is a
     * valid IntroURL, cancel the event and execute the intro action that is
     * embedded in the URL
     */
    public void changing(LocationEvent event) {
        String url = event.location;
        if (url == null)
            return;

        IntroModelRoot model = implementation.getModel();
        IntroURLParser parser = new IntroURLParser(url);
        if (parser.hasIntroUrl()) {
            // stop URL first.
            event.doit = false;
            // execute the action embedded in the IntroURL
            IntroURL introURL = parser.getIntroURL();
            introURL.execute();

            // In the case of dynamic Intro, guard against extra Frame
            // navigations. This can happen in the case of intro injected
            // IFrames or Frames included through intro xml content.
            // INTRO: user defined iframes in Intro pages are not properly
            // supported here, only Help system injected iframes.
            if (model.isDynamic()) {
                if ((introURL.getParameter(IntroURL.KEY_EMBED_TARGET) != null)
                        || introURL.getAction().equals(IntroURL.SHOW_PAGE))
                    implementation.flagStartOfNavigation();
            }
            return;
        }

        if (!parser.hasProtocol() || parser.getHost() == null)
            // This will filter out two navigation events fired by the browser
            // on a setText. (about:blank and
            // res://C:\WINDOWS\System32\shdoclc.dll/navcancl.htm on windows,
            // and file:/// on Linux)
            return;

        if (model.isDynamic()) {
            // Update the history even with real URLs. Note that if we have
            // multiple embedded URL navigations due to frames, the
            // frameNavigation flag filters them. This flag is set here, and is
            // cleared by a progress listener, when all the frame navigation is
            // completed. We need to update history only in dynamic case. In
            // static case, the browser keeps the history.
            Browser browser = (Browser) event.getSource();
            if (browser.getData("frameNavigation") == null && event.top == true) {
                // we are navigating to a regular fully qualified URL. Event.top
                // is true.
                browser.setData("frameNavigation", "true");
                implementation.updateHistory(url);
            }

            if (browser.getData("frameNavigation") == null
                    && event.top == false) {
                // a new url navigation that is not in a top frame, assume that
                // it is an embedded Help System topic navigation. we are
                // navigating to an injected iframe since event.top is false.
                // Set the iframe url of the current iframe page, and add it to
                // history.
                AbstractIntroPage currentPage = model.getCurrentPage();
                if (currentPage.isIFramePage()) {
                    currentPage.setIFrameURL(url);
                    implementation.updateHistory(currentPage);
                }
            }
        }
        return;
    }



}
