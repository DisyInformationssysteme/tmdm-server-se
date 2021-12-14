/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 *  %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.webapp.core.util;

import org.junit.Test;
import org.talend.mdm.commmon.util.core.MDMXMLUtils;

import static org.junit.Assert.*;

@SuppressWarnings("nls")
public class XmlUtilTest {

    @Test
    public void escapeXml() {
        assertEquals(null, MDMXMLUtils.escapeXml(null));
        assertEquals("fn:concat(&quot;a&b&quot;, &quot;s&quot;)", MDMXMLUtils.escapeXml("fn:concat(&quot;a&b&quot;, &quot;s&quot;)"));
        assertEquals("fn:concat(&quot;a&amp;b&quot;, &quot;s&quot;)",
                MDMXMLUtils.escapeXml("fn:concat(&quot;a&amp;b&quot;, &quot;s&quot;)"));
        assertEquals("fn:concat(&quot;a&amp;b&quot;, &quot;s&quot;)", MDMXMLUtils.escapeXml("fn:concat(\"a&b\", \"s\")"));
    }

    @Test
    public void unescapeXML() {
        assertEquals(null, MDMXMLUtils.unescapeXml(null));
        assertEquals("fn:concat(\"a&b\", \"s\")", MDMXMLUtils.unescapeXml("fn:concat(&quot;a&b&quot;, &quot;s&quot;)"));
        assertEquals("fn:concat(\"a&b\", \"s\")", MDMXMLUtils.unescapeXml("fn:concat(&quot;a&amp;b&quot;, &quot;s&quot;)"));
    }
}