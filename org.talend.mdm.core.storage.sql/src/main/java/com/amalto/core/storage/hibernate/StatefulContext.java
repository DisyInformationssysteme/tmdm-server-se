/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.storage.hibernate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.ContainedComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.ContainedTypeFieldMetadata;
import org.talend.mdm.commmon.metadata.DefaultMetadataVisitor;
import org.talend.mdm.commmon.metadata.EnumerationFieldMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataUtils;
import org.talend.mdm.commmon.metadata.ReferenceFieldMetadata;
import org.talend.mdm.commmon.metadata.SimpleTypeFieldMetadata;

import com.amalto.core.storage.datasource.RDBMSDataSource;

public class StatefulContext implements MappingCreatorContext {

    private static final Logger LOGGER = LogManager.getLogger(StatefulContext.class);

    private final Map<String, String> enforcedUniqueNames = new HashMap<String, String>();

    private final AtomicInteger uniqueInheritanceCounter = new AtomicInteger();

    private final RDBMSDataSource.DataSourceDialect dialect;

    public StatefulContext(RDBMSDataSource.DataSourceDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public String getFieldColumn(FieldMetadata field) {
        if (!field.getContainingType().getSuperTypes().isEmpty() && !field.getContainingType().isInstantiable()) {
            boolean isUnique = isUniqueWithinTypeHierarchy(field.getContainingType(), field.getName());
            boolean isExistEntity = isExistInSameEntity(field);
            if (field.getDeclaringType().equals(field.getContainingType()) && !isUnique && !isExistEntity) {
                // Non instantiable types are mapped using a "table per hierarchy" strategy, if field name isn't unique
                // make sure name becomes unique to avoid conflict (Hibernate doesn't issue warning/errors in case of overlap).
                synchronized (enforcedUniqueNames) {
                    loadValueFromFile();
                    String fullPathKey = getQualifiedKey(field);
                    String enforcedUniqueName = enforcedUniqueNames.get(fullPathKey);
                    if (enforcedUniqueName == null) {
                        enforcedUniqueName = getFieldColumn(field.getName()) + uniqueInheritanceCounter.incrementAndGet();
                        writeValueToFile(fullPathKey, enforcedUniqueName);
                        enforcedUniqueNames.put(getQualifiedKey(field), enforcedUniqueName);
                    }
                    return enforcedUniqueName;
                }
            } else {
                return getFieldColumn(field.getName());
            }
        } else {
            return getFieldColumn(field.getName());
        }
    }

    /**
     * return the specific format key, that composed of namespace and name of subType, plus namespace and name of top
     * level base type. like <code>sub_namespace_student/sub_name_student/base_namespace_person/base_name_person</code>
     * the new key only is using reusable type.
     *
     * @param field : A field's Metadata

     * @return: the actual key info
     */
    private static String getQualifiedKey(FieldMetadata field) {
        String key = field.toString();
        ComplexTypeMetadata type = field.getContainingType();
        if (type == null) {
            return key;
        }
        ComplexTypeMetadata topLevelType = (ComplexTypeMetadata) MetadataUtils.getSuperConcreteType(type);
        boolean isDupFKName = false;

        Collection<ComplexTypeMetadata> subTypeSet = topLevelType.getSubTypes().stream()
                .filter(item -> !item.getName().equals(field.getDeclaringType().getName())).collect(Collectors.toList());
        outloop: for (ComplexTypeMetadata subType : subTypeSet) {
            for (FieldMetadata fieldMetadata : subType.getFields()) {
                if (fieldMetadata instanceof ReferenceFieldMetadata && field instanceof ReferenceFieldMetadata
                        && ((ReferenceFieldMetadata) fieldMetadata).isFKIntegrity()
                        && fieldMetadata.getName().equals(field.getName())) {
                    isDupFKName = true;
                    break outloop;
                }
            }
        }

        if (type instanceof ContainedComplexTypeMetadata && isDupFKName) {
            ComplexTypeMetadata containedType = ((ContainedComplexTypeMetadata) type).getContainedType();
            key = String.join("/", containedType.getNamespace(), containedType.getName(), topLevelType.getNamespace(),
                    topLevelType.getName(), field.getName());
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The current qualified key is " + key);
        }
        return key;
    }

    private static boolean isExistInSameEntity(FieldMetadata field) {
        ComplexTypeMetadata type = field.getContainingType();

        if (type == null) {
            return false;
        }
        ComplexTypeMetadata topType = (ComplexTypeMetadata) MetadataUtils.getSuperConcreteType(type);
        int expectedLength = 18;
        if (type.getContainer() != null && type.getContainer().getContainingType() != null) {
            String curName = type.getContainer().getContainingType().getName();

            if (StringUtils.isEmpty(curName)) {
                return false;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The current containType name of field is " + curName);
            }
            String rawTypeName = type.getContainer().getType().getName();
            //If the current field's type is the same with it's super type name, return true.
            if (topType.getName().equals(rawTypeName) && curName.length() == expectedLength) {
                return true;
            }
            Boolean isSameName = false;
            boolean isExist = topType.getSubTypes().stream()
                    .anyMatch(action -> action.getUsages().stream().anyMatch(item -> {
                        if (item.getContainer() != null && item.getContainer().getContainingType() != null) {
                            return curName.equals(item.getContainer().getContainingType().getName());
                        } else {
                            return false;
                        }
                    }));
            if (isExist) {
                //This logic code only use in RTE DM
                Collection<ComplexTypeMetadata> subTypeList = topType.getSubTypes();
                outLoop: for (ComplexTypeMetadata subTribe : subTypeList) {
                    for (FieldMetadata subField : subTribe.getFields()) {
                        if (!subField.equals(field) && !subField.getContainingType().equals(field.getContainingType())
                                && subField.getName().equals(field.getName())) {
                            isSameName = true;
                            break outLoop;
                        }
                    }
                }
            }
            return isExist && !isSameName;
        }
        return false;
    }

    /**
     * Controls whether a field name is unique within type hierarchy accessible from <code>type</code> (i.e. go to the
     * top level type and recursively checks for field with name <code>name</code>).
     * 
     * @param type A type part of an inheritance hierarchy
     * @param name A field name.
     * @return <code>true</code> if there's no other field named <code>name</code> in the type hierarchy accessible from
     * <code>type</code>.
     */
    private static boolean isUniqueWithinTypeHierarchy(ComplexTypeMetadata type, final String name) {
        ComplexTypeMetadata topLevelType = (ComplexTypeMetadata) MetadataUtils.getSuperConcreteType(type);
        int occurrenceCount = topLevelType.accept(new DefaultMetadataVisitor<Integer>() {

            int count = 0;

            private void handleField(FieldMetadata simpleField) {
                if (name.equals(simpleField.getName())) {
                    count++;
                }
            }

            @Override
            public Integer visit(FieldMetadata fieldMetadata) {
                handleField(fieldMetadata);
                return count;
            }

            @Override
            public Integer visit(ContainedTypeFieldMetadata containedField) {
                handleField(containedField);
                return count;
            }

            @Override
            public Integer visit(ReferenceFieldMetadata referenceField) {
                handleField(referenceField);
                return count;
            }

            @Override
            public Integer visit(SimpleTypeFieldMetadata simpleField) {
                handleField(simpleField);
                return count;
            }

            @Override
            public Integer visit(EnumerationFieldMetadata enumField) {
                handleField(enumField);
                return count;
            }

            @Override
            public Integer visit(ComplexTypeMetadata complexType) {
                if (complexType.getSubTypes().size() < 1) {
                    super.visit(complexType);
                }
                for (ComplexTypeMetadata subType : complexType.getSubTypes()) {
                    subType.accept(this);
                }
                return count;
            }
        });
        return occurrenceCount <= 1;
    }

    private File loadColumnNameIndexes() {
        File file = null;
        String str = System.getProperty("java.io.tmpdir");
        try {
            file = Paths.get(str, "columnNameIndexFile.data").toFile();
            if (file.createNewFile()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("New temporary File is created!");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create file.", e);
        }
        return file;
    }

    private void writeValueToFile(String key, String value) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(loadColumnNameIndexes().getPath(), true))) {
            if (!enforcedUniqueNames.containsKey(key)) {
                // write text to file
                bw.write(key + "$" + value);
                bw.newLine();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write content to file.", e);
        }
    }

    private void loadValueFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(loadColumnNameIndexes().getPath()))) {
            // read until end of file
            String line;
            while ((line = br.readLine()) != null) {
                enforcedUniqueNames.put(line.substring(0, line.indexOf("$")), line.substring(line.indexOf("$") + 1));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load content from file.", e);
        }
    }

    @Override
    public String getFieldColumn(String name) {
        return "x_" + name.replace('-', '_').toLowerCase(); //$NON-NLS-1$
    }

    @Override
    public int getTextLimit() {
        return dialect.getTextLimit();
    }
}