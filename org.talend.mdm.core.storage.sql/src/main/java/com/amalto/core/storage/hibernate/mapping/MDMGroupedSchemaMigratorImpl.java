// ============================================================================
//
// Copyright (C) 2006-2020 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package com.amalto.core.storage.hibernate.mapping;

import java.util.Iterator;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.GroupedSchemaMigratorImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.jboss.logging.Logger;

import com.amalto.core.storage.hibernate.H2CustomDialect;
import com.amalto.core.storage.hibernate.MDMHibernateSchemaManagementTool;

/**
 * As an adapter class during MDM and Hibernate 5, it derived from standard Hibernate implementation for performing
 * schema management class {@link GroupedSchemaMigratorImpl}.This implementation executes a single
 * {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} call to retrieve all the database table
 * in order to determine if all the {@link javax.persistence.Entity} have a mapped database tables. If some DDL
 * statement of {@link javax.persistence.Entity} has the special grammar, {@link MDMTable} will derived from
 * {@link Table} to gennerate normal one against kinds of DB.
 * <p>
 * Created by hwzhu on Aug 19, 2020
 */
public class MDMGroupedSchemaMigratorImpl extends GroupedSchemaMigratorImpl {

    private static final CoreMessageLogger LOGGER = Logger.getMessageLogger(CoreMessageLogger.class, MDMGroupedSchemaMigratorImpl.class.getName());

    public MDMGroupedSchemaMigratorImpl(MDMHibernateSchemaManagementTool tool, SchemaFilter schemaFilter) {
        super(tool, schemaFilter);
    }

    @Override
    protected NameSpaceTablesInformation performTablesMigration(
            Metadata metadata,
            DatabaseInformation existingDatabase,
            ExecutionOptions options,
            Dialect dialect,
            Formatter formatter,
            Set<String> exportIdentifiers,
            boolean tryToCreateCatalogs,
            boolean tryToCreateSchemas,
            Set<Identifier> exportedCatalogs,
            Namespace namespace, GenerationTarget[] targets) {
        final NameSpaceTablesInformation tablesInformation =
                new NameSpaceTablesInformation( metadata.getDatabase().getJdbcEnvironment().getIdentifierHelper());

        if (schemaFilter.includeNamespace(namespace)) {
            createSchemaAndCatalog(
                    existingDatabase,
                    options,
                    dialect,
                    formatter,
                    tryToCreateCatalogs,
                    tryToCreateSchemas,
                    exportedCatalogs,
                    namespace,
                    targets
            );
            final NameSpaceTablesInformation tables = existingDatabase.getTablesInformation(namespace);

            for (Table table : namespace.getTables()) {
                if (schemaFilter.includeTable(table) && table.isPhysicalTable()) {
                    checkExportIdentifier(table, exportIdentifiers);
                    final TableInformation tableInformation = tables.getTableInformation(table);
                    if (tableInformation == null) {
                        LOGGER.tableNotFound(table.getName());
                        createTable(table, dialect, metadata, formatter, options, targets);
                    } else if (tableInformation.isPhysicalTable()) {
                        tablesInformation.addTableInformation(tableInformation);
                        MDMTable mdmTable = new MDMTable(namespace, table.getNameIdentifier(), table.getSubselect(), table.isAbstract());
                        for (Iterator iterator = table.getColumnIterator(); iterator.hasNext();) {
                            mdmTable.addColumn((Column) iterator.next());
                        }
                        migrateTable(mdmTable, tableInformation, dialect, metadata, formatter, options, targets);
                    }
                }
            }

            for (Table table : namespace.getTables()) {
                if (schemaFilter.includeTable(table) && table.isPhysicalTable()) {
                    final TableInformation tableInformation = tablesInformation.getTableInformation(table);
                    if (tableInformation == null || tableInformation.isPhysicalTable()) {
                        applyIndexes(table, tableInformation, dialect, metadata, formatter, options, targets);
                        applyUniqueKeys(table, tableInformation, dialect, metadata, formatter, options, targets);
                    }
                }
            }
        }
        LOGGER.schemaUpdateComplete();
        return tablesInformation;
    }

    @Override
    protected void applyIndexes(
            Table table,
            TableInformation tableInformation,
            Dialect dialect,
            Metadata metadata,
            Formatter formatter,
            ExecutionOptions options,
            GenerationTarget... targets) {
        final Exporter<Index> exporter = dialect.getIndexExporter();

        final Iterator<Index> indexItr = table.getIndexIterator();
        while (indexItr.hasNext()) {
            final Index index = indexItr.next();
            if (!StringHelper.isEmpty(index.getName())) {
                IndexInformation existingIndex = null;
                if (tableInformation != null) {
                    existingIndex = findMatchingIndex(index, tableInformation);
                }
                if (existingIndex == null) {
                    applySqlStrings(
                            false,
                            evaluateIndexSql(dialect, exporter.getSqlCreateStrings(index, metadata)),
                            formatter,
                            options,
                            targets
                    );
                }
            }
        }
    }

    private IndexInformation findMatchingIndex(Index index, TableInformation tableInformation) {
        return tableInformation.getIndex(Identifier.toIdentifier(index.getName()));
    }

    /**
     * CREATE is a generic SQL command used to create INDEX, and Users in H2 Database server. statement
     * <b>sqlStrings</b> include all create indexes command used to create a user-defined index in the current table of
     * database. below method will iterate over array sqlStrings, and replace each of create index statement into
     * <code>"CREATE INDEX IF NOT EXISTS ..."</code> to avoid execution error.
     * <p>
     * Implementation just affect H2 v2.0 or above, method {@link #findMatchingIndex(Index, TableInformation)} to check
     * if match index, unexpected results in class
     * {@link InformationExtractorJdbcDatabaseMetaDataImpl#getIndexes(TableInformation)} due to the change of H2 v2
     * System table, that don't adapt to hibernate criteria. so as a workaround, I add <code>IF NOT EXISTS </code> to
     * avoid the execution interruption.
     * 
     * @param dialect: current db dialect
     * @param sqlStrings: array: all create index statement
     * @return create index statement
     */
    private String[] evaluateIndexSql(Dialect dialect, String[] sqlStrings) {
        if (!H2CustomDialect.class.getName().equals(dialect.getClass().getName())) {
            return sqlStrings;
        }
        for (int i = 0; i < sqlStrings.length; i++) {
            sqlStrings[i] = sqlStrings[i].replace("create index", "CREATE INDEX IF NOT EXISTS ");
        }
        return sqlStrings;
    }

    @Override
    protected void createTable(
            Table table,
            Dialect dialect,
            Metadata metadata,
            Formatter formatter,
            ExecutionOptions options,
            GenerationTarget... targets) {
        applySqlStrings(
                false,
                MDMTableExporter.getInstance(dialect).getSqlCreateStrings(table, metadata),
                formatter,
                options,
                targets
        );
    }
}