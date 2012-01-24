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

package com.amalto.core.metadata;

/**
 *
 */
public class EnumerationFieldMetadata implements FieldMetadata {

    private final boolean key;

    private final String elementName;

    private final String fieldTypeName;

    private final TypeMetadata declaringType;

    private TypeMetadata containingType;

    public EnumerationFieldMetadata(TypeMetadata containingType, boolean isKey, String elementName, String fieldTypeName) {
        this.containingType = containingType;
        this.declaringType = containingType;
        key = isKey;
        this.elementName = elementName;
        this.fieldTypeName = fieldTypeName;
    }

    public String getName() {
        return elementName;
    }

    public boolean isKey() {
        return key;
    }

    public String getType() {
        return fieldTypeName;
    }

    public boolean hasForeignKeyInfo() {
        return false;
    }

    public String getForeignKeyInfoField() {
        throw new IllegalStateException("This type of field can't be a foreign key");
    }

    public TypeMetadata getContainingType() {
        return containingType;
    }

    public void setContainingType(TypeMetadata typeMetadata) {
        this.containingType = typeMetadata;
    }

    public TypeMetadata getDeclaringType() {
        return declaringType;
    }

    public boolean isFKIntegrity() {
        return false;
    }

    public boolean allowFKIntegrityOverride() {
        return false;
    }

    public void adopt(ComplexTypeMetadata metadata) {
        FieldMetadata copy = copy();
        copy.setContainingType(metadata);
        metadata.addField(copy);
    }

    public FieldMetadata copy() {
        return new EnumerationFieldMetadata(containingType, isKey(), elementName, fieldTypeName);
    }

    public <T> T accept(MetadataVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "EnumerationFieldMetadata{" +
                "declaringType=" + declaringType +
                ", containingType=" + containingType +
                ", is key=" + key +
                ", name ='" + elementName + '\'' +
                ", type name ='" + fieldTypeName + '\'' +
                '}';
    }
}
