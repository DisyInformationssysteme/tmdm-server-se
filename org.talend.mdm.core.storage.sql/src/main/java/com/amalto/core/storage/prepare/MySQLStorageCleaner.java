/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */

package com.amalto.core.storage.prepare;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.amalto.commons.core.utils.ValidateUtil;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.datasource.DataSource;
import com.amalto.core.storage.datasource.RDBMSDataSource;

class MySQLStorageCleaner implements StorageCleaner {

    private static final Logger LOGGER = LogManager.getLogger(MySQLStorageCleaner.class);

    private final StorageCleaner next;

    MySQLStorageCleaner(StorageCleaner next) {
        this.next = next;
    }

    @Override
    public void clean(Storage storage) {
        try {
            DataSource storageDataSource = storage.getDataSource();
            if (!(storageDataSource instanceof RDBMSDataSource)) {
                throw new IllegalArgumentException("Storage to clean does not seem to be a RDBMS storage.");
            }
            RDBMSDataSource dataSource = (RDBMSDataSource) storageDataSource;
            if (!dataSource.hasInit()) {
                throw new IllegalArgumentException("Data source '" + dataSource.getName()
                        + "' does not define initialization information.");
            }
            Connection connection = RDBMSDataSource.getConnectionToInit(dataSource);

            try {
                Statement statement = connection.createStatement();
                try {
                    statement.execute("drop database `" + ValidateUtil.matchCommonRegex(dataSource.getDatabaseName()) + "`;"); //$NON-NLS-1$ //$NON-NLS-2$
                } catch (SQLException e) {
                    // Assumes database is already dropped.
                    LOGGER.warn("Exception occurred during DROP DATABASE statement.", e);
                } finally {
                    statement.close();
                }
            } finally {
                connection.close();
            }

            next.clean(storage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
