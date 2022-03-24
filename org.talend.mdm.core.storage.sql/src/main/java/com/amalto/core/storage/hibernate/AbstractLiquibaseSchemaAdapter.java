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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.talend.mdm.commmon.metadata.compare.Compare;

import com.amalto.core.storage.HibernateStorageUtils;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.datasource.RDBMSDataSource;

import liquibase.change.AbstractChange;
import liquibase.change.core.DropIndexChange;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.precondition.core.IndexExistsPrecondition;
import liquibase.precondition.core.PreconditionContainer;
import liquibase.serializer.core.xml.XMLChangeLogSerializer;

public abstract class AbstractLiquibaseSchemaAdapter {

    protected static final Logger LOGGER = LogManager.getLogger(AbstractLiquibaseSchemaAdapter.class);

    private static final String SEPARATOR = "-"; //$NON-NLS-1$

    public static final String DATA_LIQUIBASE_CHANGELOG_PATH = "/data/liquibase-changelog/"; //$NON-NLS-1$

    public static final String MDM_ROOT = "mdm.root"; //$NON-NLS-1$

    protected RDBMSDataSource dataSource;

    protected StorageType storageType;

    public AbstractLiquibaseSchemaAdapter(RDBMSDataSource dataSource,StorageType storageType) {
        this.dataSource = dataSource;
        this.storageType = storageType;
    }

    /**
     * Each change which describes the change/refactoring to apply to the database,
     * Liquibase supports multiple descriptive changes for all major database.
     * @param connection : current connection object.
     * @param diffResults
     * @throws Exception
     */
    public abstract void adapt(Connection connection, Compare.DiffResults diffResults) throws Exception;

    protected DatabaseChangeLog getChangeLogFilePath(List<AbstractChange> changeType) {
        // create a changelog
        liquibase.changelog.DatabaseChangeLog databaseChangeLog = new liquibase.changelog.DatabaseChangeLog();

        for (AbstractChange change : changeType) {

            // create a changeset
            liquibase.changelog.ChangeSet changeSet = new liquibase.changelog.ChangeSet(UUID.randomUUID().toString(),
                    "administrator", false, false, StringUtils.EMPTY, null, null, true, null, databaseChangeLog); //$NON-NLS-1$
            changeSet.addChange(change);

            // add created changeset to changelog
            databaseChangeLog.addChangeSet(changeSet);
            if (change instanceof DropIndexChange && HibernateStorageUtils.isSQLServer(dataSource.getDialectName())
                    && storageType == StorageType.MASTER) {
                PreconditionContainer preconditionContainer = new PreconditionContainer();
                preconditionContainer.setOnFail(PreconditionContainer.FailOption.MARK_RAN.toString());

                DropIndexChange dropIndexChange = (DropIndexChange) change;
                IndexExistsPrecondition indexExistsPrecondition = new IndexExistsPrecondition();
                indexExistsPrecondition.setSchemaName(dropIndexChange.getSchemaName());
                indexExistsPrecondition.setCatalogName(dropIndexChange.getCatalogName());
                indexExistsPrecondition.setTableName(dropIndexChange.getTableName());
                indexExistsPrecondition.setIndexName(dropIndexChange.getIndexName());

                preconditionContainer.addNestedPrecondition(indexExistsPrecondition);
                changeSet.setPreconditions(preconditionContainer);
            }
        }

        return generateChangeLogFile(databaseChangeLog);
    }

    protected DatabaseChangeLog generateChangeLogFile(liquibase.changelog.DatabaseChangeLog databaseChangeLog) {
        // create a new serializer
        XMLChangeLogSerializer xmlChangeLogSerializer = new XMLChangeLogSerializer();
        DatabaseChangeLog changeLog = new DatabaseChangeLog();
        String fileName = String.join(SEPARATOR, dateFormat(System.currentTimeMillis(), "yyyyMMddHHmm"), //$NON-NLS-1$
                Long.toString(System.currentTimeMillis()), storageType.name() + ".xml"); //$NON-NLS-1$
        Path fileDir = Paths.get(System.getProperty(MDM_ROOT), DATA_LIQUIBASE_CHANGELOG_PATH,
                dateFormat(System.currentTimeMillis(), "yyyyMMdd")); //$NON-NLS-1$
        File changeLogFile = null;
        try {
            Path filePath = Paths.get(Files.createDirectories(fileDir).toString(), fileName);
            changeLogFile = Files.createFile(filePath).toFile();
            LOGGER.info("File %s created successfully!", fileDir); // $NON-NLS-1$
        } catch (IOException e1) {
            throw new RuntimeException("Failed to create Liquibase log file.", e1); // $NON-NLS-1$
        }

        try (FileOutputStream baos = new FileOutputStream(changeLogFile);) {
            xmlChangeLogSerializer.write(databaseChangeLog.getChangeSets(), baos);
            changeLog = new DatabaseChangeLog(changeLogFile.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Writing liquibase change log file failed.", e); //$NON-NLS-1$
        }
        return changeLog;
    }

    private static String dateFormat(long date, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        sdf.setTimeZone(gmt);
        sdf.setLenient(true);
        return sdf.format(new Date(date));
    }
}