/*
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.storage.hibernate.mapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.log4j.Logger;

import com.amalto.core.storage.datasource.RDBMSDataSource;
import com.amalto.core.storage.datasource.RDBMSDataSource.DataSourceDialect;

/**
 * Utility class to find all foreign key constraints in kinds of database, like oracle, MS server, postgresql etc. then
 * drop the overdue constraint for special <b>DROP</b> statement. Then recreate All Foreign Keys with the purpose of
 * Reference a Table in database.
 * <p>
 * for HIGH LEVEL changed, to get different query foreign key constraint script that does everything you need, fixing
 * the primary-foreign key relationship while preserving all existing data. If not, you will have a script, already
 * started, that performs most of what you need to do and should be able to drop it for your needs.
 * </p>
 * @author hwzhu
 *
 */
public final class ForeignKeyUpdateStrategy {

    private static final Logger LOGGER = Logger.getLogger(ForeignKeyUpdateStrategy.class);

    public static void cleanOverdueForeignKeys(RDBMSDataSource.DataSourceDialect dialect, Set<String> tablesToDrop,
            Connection connection) throws SQLException {

        DataSourceDialect[] dialectList = DataSourceDialect.values();
        String constraintDropString = null;
        for (DataSourceDialect item : dialectList) {
            if (item == dialect) {
                constraintDropString = getConstraintDropString(dialect, connection);
                break;
            }
        }
        cleanConstraintFK(constraintDropString, tablesToDrop, connection, dialect);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Drop constraints referencing tables successfully.");
        }
    }

    private static String getConstraintDropString(RDBMSDataSource.DataSourceDialect dialect, Connection connection) throws SQLException {
        String constraintDropString = null;
        switch (dialect) {
        case POSTGRES:
            constraintDropString = "SELECT TC.table_name, TC.constraint_name, CU.table_name "
                    + "FROM information_schema.table_constraints TC "
                    + "INNER JOIN information_schema.constraint_column_usage CU "
                    + "ON TC.constraint_name = CU.constraint_name "
                    + "WHERE TC.constraint_name LIKE 'fk_%'";
            break;
        case MYSQL:
            constraintDropString = "SELECT TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME "
                    + "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE "
                    + "WHERE CONSTRAINT_NAME like 'FK_%' AND TABLE_SCHEMA = '" + connection.getCatalog() + "'";
            break;
        case SQL_SERVER:
            constraintDropString = "SELECT OBJECT_NAME(f.parent_object_id) AS TableName, f.name, "
                    + "OBJECT_NAME (f.referenced_object_id) AS ReferenceTableName "
                    + "FROM sys.foreign_keys AS f "
                    + "INNER JOIN sys.foreign_key_columns AS fc ON f.OBJECT_ID = fc.constraint_object_id";
            break;
        case ORACLE_10G:
            constraintDropString = "SELECT A.TABLE_NAME, A.CONSTRAINT_NAME, c_pk.TABLE_NAME "
                    + "FROM all_cons_columns a JOIN all_constraints c ON a.owner = c.owner "
                    + "AND a.constraint_name = c.constraint_name "
                    + "JOIN all_constraints c_pk ON c.r_owner = c_pk.owner "
                    + "AND c.r_constraint_name = c_pk.constraint_name "
                    + "WHERE a.CONSTRAINT_NAME like 'FK_%'";
            break;
        default:
            LOGGER.warn("The MDM server does not support the database type " + dialect.name());
        }
        return constraintDropString;
    }

    private static void cleanConstraintFK(String fkSQL, Set<String> tablesToDrop, Connection connection, 
            RDBMSDataSource.DataSourceDialect dialect) {
        if (Objects.isNull(fkSQL)) {
            return;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Execute the following command to find all foreign key constraints: " + fkSQL);
        }

        Map<String, String> fkMap = new HashMap<>();
        PreparedStatement fkStatement = null;
        try {
            fkStatement = connection.prepareStatement(fkSQL);
            ResultSet rs = fkStatement.executeQuery();
            while (rs.next()) {
                // 1: table_name, 2: constraint_name, 3: reference_table_name
                fkMap.put(rs.getString(2).toLowerCase(), rs.getString(1));
                boolean isInclude = false;
                //If drop table is referenced by other table, also add corresponding table to collection tablesToDrop
                for (Iterator<String> it = tablesToDrop.iterator(); it.hasNext();) {
                    if (it.next().equalsIgnoreCase(rs.getString(3))) {
                        isInclude = true;
                        break;
                    }
                }
                if (isInclude) {
                    tablesToDrop.add(rs.getString(1));
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("The table [" + rs.getString(3) + "] was referenced by table  [" + rs.getString(1) + "]");
                    }
                }
            }
        } catch (SQLException e1) {
            throw new RuntimeException("Could not acquire connection to database.", e1); //$NON-NLS-1$
        } finally {
            try {
                if (fkStatement != null) {
                    fkStatement.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Unexpected error when closing connection.", e); //$NON-NLS-1$
            }
        }
        for (Iterator<Map.Entry<String, String>> iterator = fkMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, String> entry = iterator.next();
            boolean isInclude = false;
            for (Iterator<String> it = tablesToDrop.iterator(); it.hasNext();) {
                if (it.next().equalsIgnoreCase(entry.getValue())) {
                    isInclude = true;
                    break;
                }
            }
            if (isInclude) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE " + entry.getValue() + " DROP CONSTRAINT " + entry.getKey()); //$NON-NLS-1$
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Execute Drop Constraint Statement: ALTER TABLE " + entry.getValue() + " DROP CONSTRAINT "
                                + entry.getKey());
                    }
                } catch (SQLException e) {
                    // if failed to delete foreign key constraint of table, don't throw new exception, continue to
                    // drop table at next step.
                }
            }
        }
    }
}