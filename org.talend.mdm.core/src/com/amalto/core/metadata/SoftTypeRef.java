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

import java.util.Collection;
import java.util.List;

/**
*
*/
class SoftTypeRef implements TypeMetadata {

    private final MetadataRepository repository;

    private final String fieldTypeName;

    private TypeMetadata getType() {
        TypeMetadata type = repository.getType(fieldTypeName);
        if (type == null) {
            throw new IllegalArgumentException("Type '" + fieldTypeName + "' is not present in type repository.");
        }
        return type;
    }

    public SoftTypeRef(MetadataRepository repository, String fieldTypeName) {
        this.repository = repository;
        this.fieldTypeName = fieldTypeName;
    }

    public Collection<TypeMetadata> getSuperTypes() {
        return getType().getSuperTypes();
    }

    public String getName() {
        return fieldTypeName;
    }

    public String getNamespace() {
        return "";
    }

    public boolean isAbstract() {
        return getType().isAbstract();
    }

    public FieldMetadata getField(String fieldName) {
        return getType().getField(fieldName);
    }

    public List<FieldMetadata> getFields() {
        return getType().getFields();
    }

    public boolean isAssignableFrom(TypeMetadata type) {
        return getType().isAssignableFrom(type);
    }

    public void addSuperType(TypeMetadata superType) {
        getType().addSuperType(superType);
    }

    public <T> T accept(MetadataVisitor<T> visitor) {
        return getType().accept(visitor);
    }

    @Override
    public String toString() {
        return getType().toString();
    }
}
