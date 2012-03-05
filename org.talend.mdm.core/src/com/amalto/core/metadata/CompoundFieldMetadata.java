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

import java.util.Arrays;
import java.util.List;

public class CompoundFieldMetadata implements FieldMetadata {

    private final FieldMetadata[] fields;

    public CompoundFieldMetadata(FieldMetadata... fields) {
        this.fields = fields;
    }

    public FieldMetadata[] getFields() {
        return fields;
    }

    public String getName() {
        throw new UnsupportedOperationException();
    }

    public boolean isKey() {
        boolean isKey = true;
        for (FieldMetadata field : fields) {
            isKey &= field.isKey();
        }
        return isKey;
    }

    public TypeMetadata getType() {
        /*
         * Compound / Composite keys are always represented as strings the [id0][id1] format.
         * So this method can return "string" as type even if fields are not all string.
         */
        return new SimpleTypeMetadata(MetadataRepository.XSD_NAMESPACE, "string"); //$NON-NLS-1$ // TODO Constant
    }

    public boolean hasForeignKeyInfo() {
        throw new UnsupportedOperationException();
    }

    public FieldMetadata getForeignKeyInfoField() {
        throw new UnsupportedOperationException();
    }

    public ComplexTypeMetadata getContainingType() {
        return fields[0].getContainingType();
    }

    public void setContainingType(ComplexTypeMetadata typeMetadata) {
        throw new UnsupportedOperationException();
    }

    public TypeMetadata getDeclaringType() {
        return fields[0].getDeclaringType();
    }

    public boolean isFKIntegrity() {
        throw new UnsupportedOperationException();
    }

    public boolean allowFKIntegrityOverride() {
        throw new UnsupportedOperationException();
    }

    public void adopt(ComplexTypeMetadata metadata, MetadataRepository repository) {
        throw new UnsupportedOperationException();
    }

    public FieldMetadata copy(MetadataRepository repository) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "Compound {" + "# of fields=" + fields.length + '}'; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public List<String> getHideUsers() {
        throw new UnsupportedOperationException();
    }

    public List<String> getWriteUsers() {
        throw new UnsupportedOperationException();
    }

    public boolean isMany() {
        throw new UnsupportedOperationException();
    }

    public boolean isMandatory() {
        throw new UnsupportedOperationException();
    }

    public void setName(String fieldName) {
        throw new UnsupportedOperationException();
    }

    public <T> T accept(MetadataVisitor<T> visitor) {
        T returnedValue = null;
        for (FieldMetadata field : fields) {
            returnedValue = field.accept(visitor);
        }
        return returnedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompoundFieldMetadata)) {
            return false;
        }

        CompoundFieldMetadata that = (CompoundFieldMetadata) o;

        return Arrays.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return fields != null ? Arrays.hashCode(fields) : 0;
    }
}
