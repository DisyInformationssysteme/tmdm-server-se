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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.SQLCriterion;
import org.hibernate.type.Type;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.DefaultMetadataVisitor;
import org.talend.mdm.commmon.metadata.EnumerationFieldMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.MetadataUtils;
import org.talend.mdm.commmon.metadata.ReferenceFieldMetadata;
import org.talend.mdm.commmon.metadata.SimpleTypeFieldMetadata;
import org.talend.mdm.commmon.metadata.Types;

import com.amalto.core.query.user.Predicate;
import com.amalto.core.storage.datasource.RDBMSDataSource;

class ManyFieldCriterion extends SQLCriterion {

    private static final long serialVersionUID = -1416041576110629822L;

    private static final Logger LOGGER = LogManager.getLogger(ManyFieldCriterion.class);

    private final RDBMSDataSource dataSource;

    private final Criteria typeCriteria;

    private final FieldMetadata field;

    private final Object value;

    private final int position;

    private final TableResolver resolver;

    private final Predicate predicate;

    private final String fieldTypeName;

    ManyFieldCriterion(RDBMSDataSource dataSource, Criteria typeSelectionCriteria, TableResolver resolver, FieldMetadata field,
            Object value, Predicate predicate, String fieldTypeName) {
        this(dataSource, typeSelectionCriteria, resolver, field, value, predicate, fieldTypeName, -1);
    }

    ManyFieldCriterion(RDBMSDataSource dataSource, Criteria typeSelectionCriteria, TableResolver resolver, FieldMetadata field,
            Object value, Predicate predicate, String fieldTypeName, int position) {
        super(StringUtils.EMPTY, new Object[0], new Type[0]);
        this.dataSource = dataSource;
        this.typeCriteria = typeSelectionCriteria;
        this.resolver = resolver;
        this.field = field;
        this.value = value;
        this.position = position;
        this.predicate = predicate;
        this.fieldTypeName = fieldTypeName;
    }

    @Override
    public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
        if (field instanceof ReferenceFieldMetadata && position < 0) {
            if (value instanceof List) {
                throw new UnsupportedOperationException("Do not support collection search criteria with multiple values."); //$NON-NLS-1$
            }
            String whereCondition = field.accept(new ForeignKeySQLGenerator(criteriaQuery, value));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The final whereCondition is : " + whereCondition);
            }
            return whereCondition;
        } else {
            if (value instanceof Object[]) {
                throw new UnsupportedOperationException("Do not support collection search criteria with multiple values."); //$NON-NLS-1$
            }
            ComplexTypeMetadata type = field.getContainingType();
            if (type.getKeyFields().size() > 1) {
                throw new UnsupportedOperationException("Do not support collection search if main type has a composite key."); //$NON-NLS-1$
            }
            String containingTypeAlias = criteriaQuery.getSQLAlias(typeCriteria);
            String containingType = resolver.get(type);
            String containingTypeKey = resolver.get(type.getKeyFields().iterator().next());
            StringBuilder builder = new StringBuilder();
            String joinedTableName = resolver.getCollectionTable(field);
            String columnName;
            if (field instanceof ReferenceFieldMetadata) {
                // Naming convention in MDM for repeatable FKs: "x_stores_store" (database field name) + "_" + "x_id"
                // (referenced field name).
                columnName = field.getName() + '_' + ((ReferenceFieldMetadata) field).getReferencedField().getName();
            } else {
                if (dataSource.getDialectName() == RDBMSDataSource.DataSourceDialect.H2) {
                    columnName = "\"value\""; //$NON-NLS-1$
                } else {
                    columnName = "value"; //$NON-NLS-1$
                }
            }
            builder.append("(SELECT COUNT(1) FROM ") //$NON-NLS-1$
                    .append(containingType).append(" INNER JOIN ") //$NON-NLS-1$
                    .append(joinedTableName).append(" ON ") //$NON-NLS-1$
                    .append(containingType).append(".") //$NON-NLS-1$
                    .append(containingTypeKey).append(" = ") //$NON-NLS-1$
                    .append(joinedTableName).append(".") //$NON-NLS-1$
                    .append(containingTypeKey).append(" WHERE ") //$NON-NLS-1$
                    .append(joinedTableName).append("." + columnName); //$NON-NLS-1$ //$NON-NLS-2$
            if (predicate == Predicate.IN) {
                if (!(value instanceof List)) {
                    throw new UnsupportedOperationException("The value should be invalidea for in operation"); //$NON-NLS-1$
                }
                builder.append(" in("); //$NON-NLS-1$
                List valuelsit = (List) value;
                for (int i = 0; i < valuelsit.size(); i++) {
                    Object obj = valuelsit.get(i);
                    builder.append("'"); //$NON-NLS-1$
                    builder.append(obj);
                    builder.append("'"); //$NON-NLS-1$
                    if (i < valuelsit.size() - 1) {
                        builder.append(","); //$NON-NLS-1$
                    }
                }
                builder.append(") "); //$NON-NLS-1$
            } else {
                builder.append(" = '").append(value).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            if (position >= 0) {
                builder.append(" AND ").append(joinedTableName).append(".pos = ").append(position); //$NON-NLS-1$ //$NON-NLS-2$
            }
            builder.append(" AND ") //$NON-NLS-1$
                    .append(containingType).append(".") //$NON-NLS-1$
                    .append(containingTypeKey).append(" = ") //$NON-NLS-1$
                    .append(containingTypeAlias).append(".") //$NON-NLS-1$
                    .append(containingTypeKey).append(") > 0"); //$NON-NLS-1$
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The final whereCondition is : " + builder.toString());
            }
            return builder.toString();
        }
    }

    private class ForeignKeySQLGenerator extends DefaultMetadataVisitor<String> {

        private final CriteriaQuery criteriaQuery;

        private final StringBuilder query = new StringBuilder();

        private final Object value;

        private int currentIndex = 0;

        public ForeignKeySQLGenerator(CriteriaQuery criteriaQuery, Object value) {
            this.criteriaQuery = criteriaQuery;
            this.value = value;
        }

        private Object getValue() {
            Object o = value;
            if (value instanceof Object[]) {
                o = ((Object[]) value)[currentIndex++];
            }
            if (value instanceof String) {
                o = ((String) value).replace("'", "\\'"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            // DB2 does not like receiving a Boolean for boolean comparison (Hibernate uses a TINYINT)
            if (dataSource.getDialectName() == RDBMSDataSource.DataSourceDialect.DB2 && o instanceof Boolean) {
                Boolean booleanValue = (Boolean) o;
                if (booleanValue) {
                    o = 1;
                } else {
                    o = 0;
                }
            }
            return o;
        }

        @Override
        public String visit(ReferenceFieldMetadata referenceField) {
            referenceField.getReferencedField().accept(this);
            return query.toString();
        }

        @Override
        public String visit(SimpleTypeFieldMetadata simpleField) {
            return handleField(simpleField);
        }

        @Override
        public String visit(EnumerationFieldMetadata enumField) {
            return handleField(enumField);
        }

        private String handleField(FieldMetadata simpleField) {
            if (query.length() > 0) {
                query.append(" AND "); //$NON-NLS-1$
            }
            query.append("(") //$NON-NLS-1$
                    .append(criteriaQuery.getSQLAlias(typeCriteria)).append(".") //$NON-NLS-1$
                    .append(resolver.get(simpleField));

            String queryValue = String.valueOf(getValue());
            if (Predicate.STARTS_WITH == predicate) {
                query.append(" like "); //$NON-NLS-1$
                queryValue = queryValue + "%"; //$NON-NLS-1$
            } else {
                query.append(" = "); //$NON-NLS-1$
            }

            String name = MetadataUtils.getSuperConcreteType(simpleField.getType()).getName();
            boolean isStringType = Types.STRING.equals(name);
            // TMDM-7226: Consider date / time types as strings (for quotes).
            if (!isStringType) {
                for (String dateType : Types.DATES) {
                    isStringType = dateType.equals(name);
                    if (isStringType) {
                        break;
                    }
                }
            }
            if (dataSource.getDialectName() == RDBMSDataSource.DataSourceDialect.ORACLE_10G && ArrayUtils.contains(Types.DATES, name)) {
                query.append("timestamp "); //$NON-NLS-1$
            }
            if (isStringType) {
                query.append("'"); //$NON-NLS-1$
            }
            query.append(queryValue);
            if (isStringType) {
                query.append("'"); //$NON-NLS-1$
            }
            query.append(")"); //$NON-NLS-1$
            return query.toString();
        }
    }
}
