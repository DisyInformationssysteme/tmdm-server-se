// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package com.amalto.webapp.core.util;

import java.io.StringReader;
import java.util.Properties;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;


/**
 * DOC HSHU  class global comment. Detailled comment
 */
public class XmlUtil {
    
    /**
     * DOC HSHU Comment method "parseDocument".
     * @param doc
     * @return
     * @throws Exception
     */
    public static Document parseDocument(org.w3c.dom.Document doc) throws Exception {
        if (doc == null) {
            return (null);
        }
        org.dom4j.io.DOMReader xmlReader = new org.dom4j.io.DOMReader();
        return (xmlReader.read(doc));
    }
    
    public static Document styleDocument(org.w3c.dom.Document document, String stylesheet) throws Exception  {
        
        Document parsedDocument=parseDocument(document);
        return styleDocument(parsedDocument,stylesheet);
        
    }
    
    /**
     * DOC HSHU Comment method "styleDocument".
     * @param document
     * @param stylesheet
     * @return
     * @throws Exception
     */
    public static Document styleDocument(Document document, String stylesheet) throws Exception {

        // load the transformer using JAXP
        // Set the TransformerFactory system property.
        // Note: For more flexibility, load properties from a properties file.
        String key = "javax.xml.transform.TransformerFactory"; //$NON-NLS-1$
        String value = "net.sf.saxon.TransformerFactoryImpl"; //$NON-NLS-1$
        Properties props = System.getProperties();
        props.put(key, value);
        System.setProperties(props);
        
        TransformerFactory factory = TransformerFactory.newInstance();

        Transformer transformer = factory.newTransformer(new StreamSource(new StringReader(stylesheet)));

        // now lets style the given document
        DocumentSource source = new DocumentSource(document);
        DocumentResult result = new DocumentResult();
        transformer.transform(source, result);

        // return the transformed document
        Document transformedDoc = result.getDocument();

        return transformedDoc;
    }
    
    /**
     * DOC HSHU Comment method "normalizeXpath".
     * @param xpath
     */
    public static String normalizeXpath(String xpath) {
        if (xpath.startsWith("/"))
            xpath = xpath.substring(1);
        return xpath;
    }
    
    public static String toXml(Document document) {

        String text = document.asXML();

        return text;
    }
    
    public static void print(Document document) {

        String text = toXml(document);

        System.out.println(text);
    }
    
    public static String escapeXml(String value) {
        if (value == null)
            return null;
        boolean isEscaped=false;
        if(value.indexOf("&quot;")!=-1||
           value.indexOf("&amp;")!=-1||
           value.indexOf("&lt;")!=-1||
           value.indexOf("&gt;")!=-1) isEscaped =true;
        
        if(!isEscaped)value=StringEscapeUtils.escapeXml(value);
        return value;
    }

}
