/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.storage.hibernate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

//import javax.xml.XMLConstants;
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserConfigurationException;
//import javax.xml.transform.OutputKeys;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;
//import javax.xml.xpath.XPath;
//import javax.xml.xpath.XPathConstants;
//import javax.xml.xpath.XPathExpression;
//import javax.xml.xpath.XPathExpressionException;
//import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.util.core.MDMXMLUtils;
//import org.w3c.dom.Attr;
//import org.w3c.dom.Document;
//import org.w3c.dom.DocumentType;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.xml.sax.SAXException;

import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.datasource.RDBMSDataSource;
import com.amalto.core.storage.datasource.RDBMSDataSourceBuilder;

// Dynamically called! Do not remove!
public class DefaultStorageClassLoader extends StorageClassLoader {

    private static final Logger LOGGER = LogManager.getLogger(DefaultStorageClassLoader.class);

//    private static final XPath pathFactory = XPathFactory.newInstance().newXPath();

    public DefaultStorageClassLoader(ClassLoader parent,
                                     String storageName,
                                     StorageType type) {
        super(parent, storageName, type);
    }

    @Override
    public InputStream generateEhCacheConfig() {
        try {
//            DocumentBuilder documentBuilder = MDMXMLUtils.getDocumentBuilderWithNamespace().get();
//            Document document = documentBuilder.parse(this.getClass().getResourceAsStream(EHCACHE_XML_CONFIG));
//            // <diskStore path="java.io.tmpdir"/>
//            XPathExpression compile = pathFactory.compile("ehcache/diskStore"); //$NON-NLS-1$
//            Node node = (Node) compile.evaluate(document, XPathConstants.NODE);
//            node.getAttributes().getNamedItem("path").setNodeValue(dataSource.getCacheDirectory() + '/' + dataSource.getName()); //$NON-NLS-1$
//            return toInputStream(document);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream generateHibernateMapping() {
        if (resolver == null) {
            throw new IllegalStateException("Expected table resolver to be set before this method is called.");
        }
        return null;

//        try {
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            factory.setNamespaceAware(true);
//            factory.setExpandEntityReferences(false);
//            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
//            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
//            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
//            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
//            documentBuilder.setEntityResolver(HibernateStorage.ENTITY_RESOLVER);
//            Document document = documentBuilder.parse(this.getClass().getResourceAsStream(HIBERNATE_MAPPING_TEMPLATE));
//            if (dataSource.getDialectName() == RDBMSDataSource.DataSourceDialect.MYSQL) {
//                Attr propertyCatelog = document.createAttribute("catalog"); //$NON-NLS-1$
//                propertyCatelog.setValue(getCatalog());
//                document.getDocumentElement().getAttributes().setNamedItem(propertyCatelog);
//            }
//            MappingGenerator mappingGenerator = getMappingGenerator(document, resolver);
//            for (Map.Entry<String, Class<? extends Wrapper>> classNameToClass : registeredClasses.entrySet()) {
//                ComplexTypeMetadata typeMetadata = knownTypes.get(classNameToClass.getKey());
//                if (typeMetadata != null && typeMetadata.getSuperTypes().isEmpty()) {
//                    Element classElement = typeMetadata.accept(mappingGenerator);
//                    if (classElement != null) { // Class element might be null if mapping is not applicable for this type
//                        document.getDocumentElement().appendChild(classElement);
//                    }
//                }
//            }
//            return toInputStream(document);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }

    protected String getCatalog() {
        String catalog = StringUtils.EMPTY;
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(dataSource.getConnectionURL(), dataSource.getUserName(),
                    dataSource.getPassword());
            catalog = connection.getCatalog();
        } catch (Exception e) {
            LOGGER.error("Failed to get connection of " + dataSource.getDatabaseName(), e);  //$NON-NLS-1$
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Failed to close MySQL connection.", e); //$NON-NLS-1$
            }
        }
        return catalog;
    }



    @Override
    public InputStream generateHibernateConfig() {
        try {
//            Document document = generateHibernateConfiguration(dataSource);
//            return toInputStream(document);
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Could not generate Hibernate configuration", e);
        }
    }


    protected String getDialect(RDBMSDataSource.DataSourceDialect dialectType) {
        switch (dialectType) {
            case H2:
                // Default Hibernate configuration for Hibernate forgot some JDBC type mapping.
                return H2CustomDialect.class.getName();
            case MYSQL:
                return "org.hibernate.dialect.MySQL57Dialect"; //$NON-NLS-1$
            default:
                throw new IllegalArgumentException("Not supported database type '" + dialectType + "'");
        }
    }



}
