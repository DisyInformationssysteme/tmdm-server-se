/*
 * Copyright (C) 2006-2011 Talend Inc. - www.talend.com
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

/**
 *
 */
public class DefaultMetadataVisitor<T> implements MetadataVisitor<T> {

    public T visit(MetadataRepository repository) {
        Collection<TypeMetadata> types = repository.getTypes();
        for (TypeMetadata type : types) {
            type.accept(this);
        }

        return null;
    }

    public T visit(SimpleTypeMetadata simpleType) {
        return null;
    }

    public T visit(ComplexTypeMetadata complexType) {
        Collection<FieldMetadata> fields = complexType.getFields();
        for (FieldMetadata field : fields) {
            field.accept(this);
        }

        return null;
    }

    public T visit(ContainedComplexTypeMetadata containedType) {
        Collection<FieldMetadata> fields = containedType.getFields();
        for (FieldMetadata field : fields) {
            field.accept(this);
        }

        return null;
    }

    public T visit(ReferenceFieldMetadata referenceField) {
        return null;
    }

    public T visit(ContainedTypeFieldMetadata containedField) {
        return containedField.getContainedType().accept(this);
    }

    public T visit(FieldMetadata field) {
        return null;
    }

    public T visit(SimpleTypeFieldMetadata simpleField) {
        return null;
    }

    public T visit(EnumerationFieldMetadata enumField) {
        return null;
    }

}