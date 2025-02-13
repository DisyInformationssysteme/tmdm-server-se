/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */
package com.amalto.core.storage.inmemory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.SessionLocal;
import org.h2.jdbc.JdbcConnection;
import org.talend.mdm.commmon.metadata.MetadataRepository;

import com.amalto.core.query.user.Expression;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.datasource.DataSourceDefinition;
import com.amalto.core.storage.datasource.RDBMSDataSource;
import com.amalto.core.storage.datasource.RDBMSDataSourceBuilder;
import com.amalto.core.storage.hibernate.HibernateStorage;

public class MemoryStorage extends HibernateStorage {

    protected static final Logger LOGGER = LogManager.getLogger(MemoryStorage.class);

    public MemoryStorage(String storageName, StorageType type) {
        super(storageName, type);
    }

    @Override
    public void init(DataSourceDefinition dataSourceDefinition) {
        RDBMSDataSourceBuilder builder = RDBMSDataSourceBuilder.newBuilder();
        builder.driverClassName("org.h2.Driver"); //$NON-NLS-1$
        builder.dialect(RDBMSDataSource.DataSourceDialect.H2).connectionURL("jdbc:h2:mem:" + getName() + ";DB_CLOSE_DELAY=-1"); //$NON-NLS-1$ //$NON-NLS-2$
        // Don't initialize a huge connection pool (not needed).
        builder.connectionPoolMinSize(1).connectionPoolMaxSize(1);
        builder.generateConstraints(false);
        dataSource = builder.build();
    }

    @Override
    public void close() {
        super.close();
        try {
            Class.forName(dataSource.getDriverClassName()).newInstance();
            Connection connection = DriverManager.getConnection(dataSource.getConnectionURL(), dataSource.getUserName(),
                    dataSource.getPassword());
            JdbcConnection h2Connection = (JdbcConnection) connection;
            Session h2Session = (Session) h2Connection.getSession();
            if (h2Session instanceof SessionLocal) {
                SessionLocal sessionLocal = (SessionLocal)h2Session;
                Database h2Database = sessionLocal.getDatabase();
                LOGGER.info("In-memory h2 db '" + h2Database.getName() + "' will be closed.");
                h2Database.shutdownImmediately();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("In-memory h2 db '" + h2Database.getName() + "' has been shutted down.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void prepare(MetadataRepository repository, boolean dropExistingData) {
        if (dropExistingData) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No need to drop existing data for a in-memory storage.");
            }
        }
        super.prepare(repository, false);
    }

    @Override
    public synchronized void prepare(MetadataRepository repository, Set<Expression> optimizedExpressions, boolean force,
            boolean dropExistingData) {
        if (dropExistingData) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No need to drop existing data for a in-memory storage.");
            }
        }
        super.prepare(repository, optimizedExpressions, force, false);
    }
}
