/*
 * Copyright (C) 2006-2012 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.storage.record;

import com.amalto.core.metadata.ComplexTypeMetadata;
import com.amalto.core.metadata.FieldMetadata;
import com.amalto.core.metadata.MetadataUtils;
import com.amalto.core.metadata.TypeMetadata;
import com.amalto.core.storage.record.metadata.DataRecordMetadata;
import com.amalto.core.storage.record.metadata.DataRecordMetadataImpl;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.util.Stack;

public class XmlStringDataRecordReader implements DataRecordReader<String> {

    private static final XMLInputFactory xmlInputFactory;

    static {
        /*
         * FIXME The newInstance() is deprecated and the newFactory() method should be used instead. However since no
         * changes in behavior are defined by this replacement method, keep deprecated method to ensure there's no
         * classloading issues for now (see TMDM-3604).
         */
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    }

    public DataRecord read(String dataClusterName, long revisionId, ComplexTypeMetadata type, String input) {
        if (type == null) {
            throw new IllegalArgumentException("Type can not be null");
        }
        if (input == null) {
            throw new IllegalArgumentException("Input can not be null");
        }

        try {
            XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new StringReader(input));
            FieldMetadata field = null;
            Stack<TypeMetadata> currentType = new Stack<TypeMetadata>();
            currentType.push(type);

            long lastModificationTime = 0;
            String taskId = null;

            // TODO To refactor (not really extensible)
            int level = 0;
            boolean hasMetUserElement = false;
            boolean isReadingTimestamp = false;
            boolean isReadingTaskId = false;
            while (!hasMetUserElement && xmlEventReader.hasNext()) {
                XMLEvent event = xmlEventReader.nextEvent();
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    if (!hasMetUserElement) {
                        if ("t".equals(startElement.getName().getLocalPart())) {
                            isReadingTimestamp = true;
                        } else if ("taskId".equals(startElement.getName().getLocalPart())) {
                            isReadingTaskId = true;
                        }
                    }

                    if (startElement.getName().getLocalPart().equals(type.getName())) {
                        hasMetUserElement = true;
                    }
                    level++;
                } else if (event.isEndElement()) {
                    level--;
                } else if (event.isCharacters()) {
                    Characters characters = event.asCharacters();
                    if (isReadingTimestamp) {
                        try {
                            lastModificationTime = Long.parseLong(characters.getData().trim());
                        } catch (NumberFormatException e) {
                            throw new RuntimeException(e);
                        }
                        isReadingTimestamp = false;
                    } else if (isReadingTaskId) {
                        String value = characters.getData().trim();
                        taskId = value.isEmpty() ? null : value;
                        isReadingTaskId = false;
                    }
                }
            }

            DataRecordMetadata metadata = new DataRecordMetadataImpl(lastModificationTime, taskId);
            DataRecord dataRecord = new DataRecord(type, metadata);
            dataRecord.setRevisionId(revisionId);

            int userXmlPayloadLevel = level;
            while (xmlEventReader.hasNext()) {
                XMLEvent xmlEvent = xmlEventReader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    if (level >= userXmlPayloadLevel) {
                        StartElement startElement = xmlEvent.asStartElement();
                        TypeMetadata typeMetadata = currentType.peek();
                        if (!(typeMetadata instanceof ComplexTypeMetadata)) {
                            throw new IllegalStateException("Expected a complex type but got a " + typeMetadata.getClass().getName());
                        }
                        field = ((ComplexTypeMetadata) typeMetadata).getField(startElement.getName().getLocalPart());
                        currentType.push(field.getType());
                    }
                    level++;
                } else if (xmlEvent.isCharacters()) {
                    if (level >= userXmlPayloadLevel && field != null) {
                        Object value = MetadataUtils.convert(xmlEvent.asCharacters().getData(), field);
                        if (value != null) {
                            dataRecord.set(field, value);
                        }
                    }
                } else if (xmlEvent.isEndElement()) {
                    EndElement endElement = xmlEvent.asEndElement();
                    if (level == userXmlPayloadLevel && endElement.getName().getLocalPart().equals(type.getName())) {
                        break;
                    }
                    field = null;
                    currentType.pop();
                    level--;
                }
            }
            return dataRecord;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
