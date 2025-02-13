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

import static com.amalto.core.query.user.UserQueryBuilder.and;
import static com.amalto.core.query.user.UserQueryBuilder.eq;
import static com.amalto.core.query.user.UserQueryBuilder.from;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.XMLConstants;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Lock;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.internal.SessionImpl;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PersistentClassVisitor;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.mapping.Value;
import org.hibernate.query.Query;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.util.jmx.impl.JMXRegistrar.IndexingProgressMonitor;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Action;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.talend.mdm.commmon.metadata.ComplexTypeMetadata;
import org.talend.mdm.commmon.metadata.CompoundFieldMetadata;
import org.talend.mdm.commmon.metadata.ContainedTypeFieldMetadata;
import org.talend.mdm.commmon.metadata.DefaultMetadataVisitor;
import org.talend.mdm.commmon.metadata.EnumerationFieldMetadata;
import org.talend.mdm.commmon.metadata.FieldMetadata;
import org.talend.mdm.commmon.metadata.InboundReferences;
import org.talend.mdm.commmon.metadata.MetadataRepository;
import org.talend.mdm.commmon.metadata.MetadataUtils;
import org.talend.mdm.commmon.metadata.MetadataUtils.SortType;
import org.talend.mdm.commmon.metadata.MetadataVisitable;
import org.talend.mdm.commmon.metadata.MetadataVisitor;
import org.talend.mdm.commmon.metadata.NoSupportTypes;
import org.talend.mdm.commmon.metadata.ReferenceFieldMetadata;
import org.talend.mdm.commmon.metadata.SimpleTypeFieldMetadata;
import org.talend.mdm.commmon.metadata.TypeMetadata;
import org.talend.mdm.commmon.metadata.Types;
import org.talend.mdm.commmon.metadata.compare.Change;
import org.talend.mdm.commmon.metadata.compare.Compare;
import org.talend.mdm.commmon.metadata.compare.Compare.DiffResults;
import org.talend.mdm.commmon.metadata.compare.ImpactAnalyzer;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.amalto.core.query.optimization.ConfigurableContainsOptimizer;
import com.amalto.core.query.optimization.ContainsOptimizer;
import com.amalto.core.query.optimization.ImplicitOrderBy;
import com.amalto.core.query.optimization.IncompatibleOperators;
import com.amalto.core.query.optimization.Optimizer;
import com.amalto.core.query.optimization.RangeOptimizer;
import com.amalto.core.query.optimization.RecommendedIndexes;
import com.amalto.core.query.optimization.UpdateReportOptimizer;
import com.amalto.core.query.user.Expression;
import com.amalto.core.query.user.OrderBy;
import com.amalto.core.query.user.Select;
import com.amalto.core.query.user.UserQueryBuilder;
import com.amalto.core.query.user.UserQueryDumpConsole;
import com.amalto.core.query.user.Visitor;
import com.amalto.core.server.MetadataRepositoryAdmin;
import com.amalto.core.server.ServerContext;
import com.amalto.core.server.StorageAdmin;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageMetadataUtils;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.StorageType;
import com.amalto.core.storage.datasource.DataSource;
import com.amalto.core.storage.datasource.DataSourceDefinition;
import com.amalto.core.storage.datasource.RDBMSDataSource;
import com.amalto.core.storage.hibernate.mapping.ForeignKeyUpdateStrategy;
import com.amalto.core.storage.hibernate.mapping.MDMTable;
import com.amalto.core.storage.prepare.FullTextIndexCleaner;
import com.amalto.core.storage.prepare.JDBCStorageCleaner;
import com.amalto.core.storage.prepare.JDBCStorageInitializer;
import com.amalto.core.storage.prepare.StorageCleaner;
import com.amalto.core.storage.prepare.StorageInitializer;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.DataRecordConverter;
import com.amalto.core.storage.record.StorageConstants;
import com.amalto.core.storage.record.metadata.DataRecordMetadata;
import com.amalto.core.storage.transaction.StorageTransaction;
import com.amalto.core.storage.transaction.TransactionManager;
import static com.amalto.commons.core.utils.ValidateUtil.matchCommonRegex;

import net.sf.ehcache.CacheManager;

public class HibernateStorage implements Storage {

    private static final int MAX_DELETE_RECORDS = 1000;

    private static final int DEFAULT_FETCH_SIZE = 500;

    public static final HibernateStorage.LocalEntityResolver ENTITY_RESOLVER = new HibernateStorage.LocalEntityResolver();

    private static final String CLASS_LOADER = "com.amalto.core.storage.hibernate.DefaultStorageClassLoader"; //$NON-NLS-1$

    private static final String ALTERNATE_CLASS_LOADER = "com.amalto.core.storage.hibernate.FullStorageClassLoader"; //$NON-NLS-1$

    private static final String DELETE_FROM_STR = "delete from "; //$NON-NLS-1$

    private static final Logger LOGGER = LogManager.getLogger(HibernateStorage.class);

    private static final Optimizer[] OPTIMIZERS = new Optimizer[] { new RangeOptimizer(), // Transforms (value > n AND
                                                                                          // value < p) into
                                                                                          // (RANGE(n,p)).
            new ContainsOptimizer(), // Transforms all '*' in CONTAINS into '%'.
            new UpdateReportOptimizer() // Adds queries on super types if update report query a concept name with super
                                        // types.
    };

    private static final String FORBIDDEN_PREFIX = "x_talend_"; //$NON-NLS-1$

    private static final MetadataChecker METADATA_CHECKER = new MetadataChecker();

    // Default value is "true" (meaning the storage will try to create database if it doesn't exist).
    private static final boolean autoPrepare = Boolean
            .valueOf(MDMConfiguration.getConfiguration().getProperty("db.autoPrepare", "true")); //$NON-NLS-1$ //$NON-NLS-2$

    private static final Boolean FLUSH_ON_LOAD = Boolean
            .valueOf(MDMConfiguration.getConfiguration().getProperty("db.flush.on.load", "false")); //$NON-NLS-1$ //$NON-NLS-2$

    private final String storageName;

    private final StorageType storageType;

    private MappingRepository mappingRepository;

    private InternalRepository typeMappingRepository;

    private ClassCreator hibernateClassCreator;

    private StorageClassLoader storageClassLoader;

    private boolean isPrepared = false;

    private SessionFactory factory;

    private Metadata metadata;

    protected RDBMSDataSource dataSource;

    private MetadataRepository userMetadataRepository;

    private TableResolver tableResolver;

    private int fetchSize;

    private int batchSize;

    /**
     * Create a {@link StorageType#MASTER} storage.
     * 
     * @param storageName Name for this storage. <b>by convention</b>, this is the MDM container name.
     * @see StorageType#MASTER
     */
    public HibernateStorage(String storageName) {
        this(storageName, StorageType.MASTER);
    }

    /**
     * @param storageName Name for this storage. <b>By convention</b>, this is the MDM container name.
     * @param type Tells whether this storage is a staging area or not.
     * @see StorageType
     */
    public HibernateStorage(String storageName, StorageType type) {
        this.storageName = storageName;
        this.storageType = type;
    }

    private boolean isIndexable(TypeMetadata fieldType) {
        if (Types.MULTI_LINGUAL.equals(fieldType.getName())) {
            return false;
        }
        if (fieldType.getData(MetadataRepository.DATA_MAX_LENGTH) != null) {
            Object maxLength = fieldType.getData(MetadataRepository.DATA_MAX_LENGTH);
            if (maxLength != null && Integer.parseInt(String.valueOf(maxLength)) > dataSource.getDialectName().getTextLimit()) {
                return false; // Don't take into indexed fields long text fields
            }
        }
        return true;
    }

    @Override
    public Storage asInternal() {
        return this;
    }

    @Override
    public int getCapabilities() {
        int capabilities = StorageConstants.CAP_TRANSACTION | StorageConstants.CAP_INTEGRITY;
        if (dataSource.supportFullText()) {
            capabilities |= StorageConstants.CAP_FULL_TEXT;
        }
        return capabilities;
    }

    @Override
    public synchronized StorageTransaction newStorageTransaction() {
        assertPrepared();
        Session session = factory.openSession();
        session.setHibernateFlushMode(FlushMode.MANUAL);
        return new HibernateStorageTransaction(this, session);
    }

    @Override
    public void init(DataSourceDefinition dataSourceDefinition) {
        // Pick the correct datasource based on storage's type.
        DataSource dataSource = dataSourceDefinition.get(storageType);
        // Stateless components
        if (dataSource == null) {
            throw new IllegalArgumentException("Data source does not declare a section for type '" + storageType + "'."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (!(dataSource instanceof RDBMSDataSource)) {
            throw new IllegalArgumentException("Data source is expected to be a RDBMS data source."); //$NON-NLS-1$
        }
        if (dataSource.isShared()) {
            LOGGER.warn("Datasource '" + dataSource.getName() + "' (for storage type: " + storageType + ") is shared " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    + "with at least another one other storage type, please review datasource configuration."); //$NON-NLS-1$
        }
        this.dataSource = (RDBMSDataSource) dataSource;
        if (storageType == StorageType.MASTER) {
            MDMTable.setDataSource(this.dataSource);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public synchronized void prepare(MetadataRepository repository, Set<Expression> optimizedExpressions, boolean force,
            boolean dropExistingData) {
        if (!force && isPrepared) {
            return; // No op operation
        }
        if (isPrepared) {
            close();
        }
        if (dataSource == null) {
            throw new IllegalArgumentException("Datasource is not set."); //$NON-NLS-1$
        }
        // No support for data models including inheritance AND for g* XSD simple types AND fields that start with X_TALEND_
        try {
            repository.accept(METADATA_CHECKER);
            userMetadataRepository = repository;
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred during unsupported features check.", e); //$NON-NLS-1$
        }
        // Loads additional types for staging area.
        if (storageType == StorageType.STAGING) {
            userMetadataRepository = repository.copy(); // See TMDM-6938: prevents staging types to appear in master
                                                        // storage.
            userMetadataRepository.load(MetadataRepositoryAdmin.class.getResourceAsStream("stagingInternalTypes.xsd")); //$NON-NLS-1$
        }
        // Create class loader for storage's dynamically created classes.
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Class<? extends StorageClassLoader> clazz;
        try {
            try {
                clazz = (Class<? extends StorageClassLoader>) Class.forName(ALTERNATE_CLASS_LOADER);
            } catch (ClassNotFoundException e) {
                clazz = (Class<? extends StorageClassLoader>) Class.forName(CLASS_LOADER);
            }
            Constructor<? extends StorageClassLoader> constructor = clazz.getConstructor(ClassLoader.class, String.class, StorageType.class);
            storageClassLoader = constructor.newInstance(contextClassLoader, storageName, storageType);
            storageClassLoader.setDataSourceConfiguration(dataSource);
            storageClassLoader.generateHibernateConfig(); // Checks if configuration can be generated.
        } catch (Exception e) {
            throw new RuntimeException("Could not create storage class loader", e); //$NON-NLS-1$
        }
        if (dropExistingData) {
            LOGGER.info("Cleaning existing database content."); //$NON-NLS-1$
            StorageCleaner cleaner = new JDBCStorageCleaner(new FullTextIndexCleaner());
            cleaner.clean(this);
        } else {
            LOGGER.info("*NOT* cleaning existing database content."); //$NON-NLS-1$
        }
        if (autoPrepare) {
            LOGGER.info("Preparing database before schema generation."); //$NON-NLS-1$
            StorageInitializer initializer = new JDBCStorageInitializer();
            if (initializer.supportInitialization(this)) {
                if (!initializer.isInitialized(this)) {
                    initializer.initialize(this);
                } else {
                    LOGGER.info("Database is already prepared."); //$NON-NLS-1$
                }
            } else {
                LOGGER.info("Datasource is not configured for automatic initialization."); //$NON-NLS-1$
            }
        } else {
            LOGGER.info("*NOT* preparing database before schema generation."); //$NON-NLS-1$
        }
        try {
            Thread.currentThread().setContextClassLoader(storageClassLoader);
            // Mapping of data model types to RDBMS (i.e. 'flatten' representation of types).
            MetadataRepository internalRepository;
            try {
                InternalRepository typeEnhancer = getTypeEnhancer();
                internalRepository = userMetadataRepository.accept(typeEnhancer);
                mappingRepository = typeEnhancer.getMappings();
            } catch (Exception e) {
                throw new RuntimeException("Exception occurred during type mapping creation.", e); //$NON-NLS-1$
            }
            // Set fields to be indexed in database.
            Set<FieldMetadata> databaseIndexedFields = new HashSet<>();
            switch (storageType) {
            case MASTER:
                // Adds indexes on user defined fields
                for (Expression optimizedExpression : optimizedExpressions) {
                    Collection<FieldMetadata> indexedFields = RecommendedIndexes.get(optimizedExpression);
                    for (FieldMetadata indexedField : indexedFields) {
                        // TMDM-5896: Don't index Composite Key fields
                        if (indexedField instanceof CompoundFieldMetadata) {
                            continue;
                        }
                        // TMDM-5311: Don't index TEXT fields
                        TypeMetadata indexedFieldType = indexedField.getType();
                        if (!isIndexable(indexedFieldType)) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Ignore index on field '" + indexedField.getName() //$NON-NLS-1$
                                        + "' because value is stored in TEXT."); //$NON-NLS-1$
                            }
                            continue;
                        }
                        // Go up the containment tree in case containing type is anonymous.
                        ComplexTypeMetadata containingType = indexedField.getContainingType().getEntity();
                        TypeMapping mapping = mappingRepository.getMappingFromUser(containingType);
                        FieldMetadata databaseField = mapping.getDatabase(indexedField);
                        if (databaseField == null) {
                            LOGGER.error("Could not index field '" + indexedField + "' (" + indexedField.getPath() //$NON-NLS-1$ //$NON-NLS-2$
                                    + "), ignoring index."); //$NON-NLS-1$
                            continue;
                        } else if (!isIndexable(databaseField.getType())) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Ignore index on field '" + indexedField.getName() //$NON-NLS-1$
                                        + "' because value (in database mapping) is stored in TEXT."); //$NON-NLS-1$
                            }
                            continue; // Don't take into indexed fields long text fields
                        }
                        // Database specific behaviors
                        switch (dataSource.getDialectName()) {
                        case SQL_SERVER:
                            // TMDM-8144: Don't index field name on SQL Server when size > 900
                            String maxLengthStr = indexedField.getType().<String> getData(MetadataRepository.DATA_MAX_LENGTH);
                            if (maxLengthStr == null) { // go up the type inheritance tree to find max length annotation
                                TypeMetadata type = indexedField.getType();
                                while (!XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(type.getNamespace())
                                        && !type.getSuperTypes().isEmpty()) {
                                    type = type.getSuperTypes().iterator().next();
                                    maxLengthStr = type.<String> getData(MetadataRepository.DATA_MAX_LENGTH);
                                    if (maxLengthStr != null) {
                                        break;
                                    }
                                }
                            }
                            if (maxLengthStr != null) {
                                int maxLength = Integer.parseInt(maxLengthStr);
                                if (maxLength > 900) {
                                    LOGGER.warn("Skip index on field '" + indexedField.getPath() + "' (too long value)."); //$NON-NLS-1$ //$NON-NLS-2$
                                    continue;
                                }
                            }
                            break;
                        case H2:
                        case MYSQL:
                        case POSTGRES:
                        case DB2:
                        case ORACLE_10G:
                        default:
                            // Nothing to do for these databases
                            break;
                        }
                        databaseIndexedFields.add(databaseField);
                        if (!databaseField.getContainingType().isInstantiable()) {
                            Collection<ComplexTypeMetadata> roots = RecommendedIndexes.getRoots(optimizedExpression);
                            for (ComplexTypeMetadata root : roots) {
                                List<FieldMetadata> path = StorageMetadataUtils
                                        .path(mappingRepository.getMappingFromUser(root).getDatabase(), databaseField);
                                if (path.size() > 1) {
                                    databaseIndexedFields.addAll(path.subList(0, path.size() - 1));
                                } else {
                                    LOGGER.warn("Failed to properly index field '" + databaseField + "'."); //$NON-NLS-1$ //$NON-NLS-2$
                                }
                            }
                        }
                    }
                }
                break;
            case STAGING:
                if (!optimizedExpressions.isEmpty()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Ignoring " + optimizedExpressions.size() + " to optimize (disabled on staging area)."); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                // Adds "staging status" / "staging block key" / "staging task id" as indexed fields
                for (TypeMapping typeMapping : mappingRepository.getAllTypeMappings()) {
                    ComplexTypeMetadata database = typeMapping.getDatabase();
                    if (database.hasField(StorageConstants.METADATA_STAGING_STATUS)) {
                        databaseIndexedFields.add(database.getField(StorageConstants.METADATA_STAGING_STATUS));
                    }
                    if (database.hasField(StorageConstants.METADATA_STAGING_BLOCK_KEY)) {
                        databaseIndexedFields.add(database.getField(StorageConstants.METADATA_STAGING_BLOCK_KEY));
                    }
                    if (database.hasField(StorageConstants.METADATA_TASK_ID)) {
                        databaseIndexedFields.add(database.getField(StorageConstants.METADATA_TASK_ID));
                    }
                    if (database.hasField(StorageConstants.METADATA_STAGING_HAS_TASK)) {
                        databaseIndexedFields.add(database.getField(StorageConstants.METADATA_STAGING_HAS_TASK));
                    }
                    if (database.hasField(StorageConstants.METADATA_STAGING_OLD_GROUP)) {
                        databaseIndexedFields.add(database.getField(StorageConstants.METADATA_STAGING_OLD_GROUP));
                    }
                }
                break;
            case SYSTEM: // Nothing to index on SYSTEM
                break;
            }
            // Don't add FK in indexes if using H2
            if (dataSource.getDialectName() == RDBMSDataSource.DataSourceDialect.H2) {
                // H2 doesn't like indexes on PKs or FKs.
                databaseIndexedFields.removeIf(field -> field instanceof ReferenceFieldMetadata || field.isKey());
            }
            switch (dataSource.getDialectName()) {
            case ORACLE_10G:
                tableResolver = new OracleStorageTableResolver(databaseIndexedFields, dataSource.getNameMaxLength());
                break;
            default:
                tableResolver = new StorageTableResolver(databaseIndexedFields, dataSource.getNameMaxLength());
            }
            storageClassLoader.setTableResolver(tableResolver);
            // Master, Staging and System share same class creator.
            switch (storageType) {
            case MASTER:
            case STAGING:
            case SYSTEM:
                hibernateClassCreator = new ClassCreator(storageClassLoader);
                break;
            }
            // Create Hibernate classes (after some modifications to the types).
            try {
                internalRepository.accept(hibernateClassCreator);
            } catch (Exception e) {
                throw new RuntimeException("Exception occurred during dynamic classes creation.", e); //$NON-NLS-1$
            }
            // Create registry of hibernate
            StandardServiceRegistryImpl serviceRegistry = null;
            try {
                // Hibernate needs to have dynamic classes in context class loader during configuration.
                InputStream ehCacheConfig = storageClassLoader.getResourceAsStream(StorageClassLoader.EHCACHE_XML_CONFIG);
                if (ehCacheConfig != null) {
                    CacheManager.create(ehCacheConfig);
                }
                ClassLoaderService myStorageClassLoader = new StorageClassLoaderServiceImpl(storageClassLoader);
                BootstrapServiceRegistryBuilder bootBuilder = new BootstrapServiceRegistryBuilder().applyClassLoaderService(myStorageClassLoader);
                BootstrapServiceRegistry bootService = bootBuilder.build();

                // Create StandardServiceRegistryBuilder
                StandardServiceRegistryBuilder serviceRegistryBuilder = new StandardServiceRegistryBuilder(bootService);
                MDMHibernateSchemaManagementTool service = new MDMHibernateSchemaManagementTool();
                serviceRegistryBuilder.addService(SchemaManagementTool.class, service);
                serviceRegistryBuilder.configure(StorageClassLoader.HIBERNATE_CONFIG);

                serviceRegistry = (StandardServiceRegistryImpl)serviceRegistryBuilder.build();
                service.injectServices(serviceRegistry);

                ServiceBinding<MDMHibernateConfigurableServiceImpl> mdmConfigurable = new ServiceBinding<MDMHibernateConfigurableServiceImpl>(
                        serviceRegistry,
                        MDMHibernateConfigurableServiceImpl.class,
                        new MDMHibernateConfigurableServiceImpl()
                );
                serviceRegistry.configureService(mdmConfigurable);
                // Create Metadata
                metadata = new MDMMetadataSources(serviceRegistry).buildMetadata();

                if (mdmConfigurable.getService().getProperty(Environment.STATEMENT_BATCH_SIZE) == null) {
                    batchSize = 0;
                } else {
                    batchSize = Integer.parseInt(mdmConfigurable.getService().getProperty(Environment.STATEMENT_BATCH_SIZE));
                }
                if (mdmConfigurable.getService().getProperty(Environment.STATEMENT_FETCH_SIZE) == null) {
                    fetchSize = DEFAULT_FETCH_SIZE;
                } else {
                    fetchSize = Integer.parseInt(mdmConfigurable.getService().getProperty(Environment.STATEMENT_FETCH_SIZE));
                }

                // Sets default schema for Oracle
                if (dataSource.getDialectName() == RDBMSDataSource.DataSourceDialect.ORACLE_10G) {
                    mdmConfigurable.getService().setProperty(Environment.DEFAULT_SCHEMA, dataSource.getUserName());
                }
                // Logs DDL *before* initialization in case initialization fails (useful for debugging).
                if (LOGGER.isTraceEnabled()) {
                    traceDDL();
                }
                // Customize schema generation according to datasource content.
                RDBMSDataSource.SchemaGeneration schemaGeneration = dataSource.getSchemaGeneration();
                List exceptions = Collections.emptyList();
                switch (schemaGeneration) {
                case CREATE:
                    SchemaExport schemaExport = new SchemaExport();
                    schemaExport.create(EnumSet.of(TargetType.DATABASE), metadata);

                    // Exception may happen during recreation (hibernate may perform statements on tables that does
                    // not exist): these exceptions are supposed to be harmless (but log them to DEBUG just in case).
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Exception(s) occurred during schema creation:"); //$NON-NLS-1$
                        for (Object exceptionObject : schemaExport.getExceptions()) {
                            LOGGER.debug(((Exception) exceptionObject).getMessage());
                        }
                    }
                    break;
                case VALIDATE:
                    SchemaValidator schemaValidator = new SchemaValidator();
                    schemaValidator.validate(metadata); // This is supposed to throw exception on validation issue.
                    break;
                case UPDATE:
                    SchemaUpdate schemaUpdate = new SchemaUpdate();
                    schemaUpdate.execute(EnumSet.of(TargetType.DATABASE), metadata);
                    exceptions = schemaUpdate.getExceptions();
                    break;
                }
                // Throw an exception if schema update met issue(s).
                if (!exceptions.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Could not prepare database schema: "); //$NON-NLS-1$
                    Iterator iterator = exceptions.iterator();
                    while (iterator.hasNext()) {
                        Exception exception = (Exception) iterator.next();
                        if (exception instanceof SQLException) {
                            SQLException currentSQLException = (SQLException) exception;
                            while (currentSQLException != null) {
                                sb.append(currentSQLException.getMessage());
                                sb.append('\n');
                                currentSQLException = currentSQLException.getNextException();
                            }
                        } else if (exception != null) {
                            sb.append(exception.getMessage());
                        }
                        if (iterator.hasNext()) {
                            sb.append('\n');
                        }
                    }
                    throw new IllegalStateException(sb.toString());
                }

                // Create SessionFactory
                factory = metadata.buildSessionFactory();
                MDMTransactionSessionContext.declareStorage(this, factory);
            } catch (Exception e) {
                StandardServiceRegistryBuilder.destroy(serviceRegistry);
                throw new RuntimeException("Exception occurred during Hibernate initialization.", e); //$NON-NLS-1$
            }
            // All set: set prepared flag to true.
            isPrepared = true;
            LOGGER.info("Storage '" + storageName + "' (" + storageType + ") is ready."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } catch (Throwable t) {
            try {
                // This prevent PermGen OOME in case of multiple failures to start.
                close();
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Error occurred during clean up following failed prepare", e); //$NON-NLS-1$
                }
            }
            throw new RuntimeException("Could not prepare '" + storageName + "'.", t); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private void traceDDL() {
        try {
            String jbossServerTempDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
            RDBMSDataSource.DataSourceDialect dialectType = dataSource.getDialectName();
            SchemaExport export = new SchemaExport();
            export.setFormat(false);
            String filename = jbossServerTempDir + File.separator + storageName + "_" + storageType + "_" + dialectType + ".ddl"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            export.setOutputFile(filename);
            export.setDelimiter(";"); //$NON-NLS-1$
            export.execute(EnumSet.of(TargetType.SCRIPT), Action.CREATE, metadata);
            if (export.getExceptions().size() > 0) {
                for (int i = 0; i < export.getExceptions().size(); i++) {
                    LOGGER.error("Error occurred while producing ddl.", //$NON-NLS-1$
                            (Exception) export.getExceptions().get(i));
                }
            }
            LOGGER.info("DDL exported to file '" + filename + "'."); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (Exception e) {
            LOGGER.error("Error occurred while producing ddl.", e); //$NON-NLS-1$
        }
    }

    protected TypeMappingStrategy getMappingStrategy() {
        switch (storageType) {
        case SYSTEM:
            switch (dataSource.getDialectName()) {
            case DB2:
            case ORACLE_10G: // DB2 and Oracle needs to store long string values to CLOBs.
                return TypeMappingStrategy.SCATTERED_CLOB;
            default:
                return TypeMappingStrategy.SCATTERED;
            }
        case MASTER:
        case STAGING:
            switch (dataSource.getDialectName()) {
            case DB2:
            case ORACLE_10G: // DB2 and Oracle needs to store long string values to CLOBs.
                return TypeMappingStrategy.SCATTERED_CLOB;
            default:
                return TypeMappingStrategy.AUTO;
            }
        default:
            throw new IllegalArgumentException("Storage type '" + storageType + "' is not supported."); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public InternalRepository getTypeEnhancer() {
        if (typeMappingRepository == null) {
            TypeMappingStrategy mappingStrategy = getMappingStrategy();
            mappingStrategy.setUseTechnicalFK(dataSource.generateTechnicalFK());
            // TODO Not nice to setUseTechnicalFK, change this
            RDBMSDataSource.DataSourceDialect dialect = dataSource.getDialectName();
            switch (storageType) {
            case SYSTEM:
                typeMappingRepository = new SystemTypeMappingRepository(mappingStrategy, dialect);
                break;
            case MASTER:
                typeMappingRepository = new UserTypeMappingRepository(mappingStrategy, dialect);
                break;
            case STAGING:
                typeMappingRepository = new StagingTypeMappingRepository(mappingStrategy, dialect);
                break;
            default:
                throw new IllegalArgumentException("Storage type '" + storageType + "' is not supported."); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Selected type mapping strategy: " + mappingStrategy); //$NON-NLS-1$
            }
        }
        return typeMappingRepository;
    }

    @Override
    public synchronized void prepare(MetadataRepository repository, boolean dropExistingData) {
        if (!isPrepared) {
            prepare(repository, Collections.<Expression> emptySet(), false, dropExistingData);
        }
    }

    @Override
    public MetadataRepository getMetadataRepository() {
        if (!isPrepared) {
            throw new IllegalStateException("Storage '" + storageName + "' has not been prepared."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return userMetadataRepository;
    }

    @Override
    public StorageResults fetch(Expression userQuery) {
        assertPrepared();
        Session session = this.getCurrentSession();
        try {
            storageClassLoader.bind(Thread.currentThread());
            if (!ServerContext.INSTANCE.get().getTransactionManager().hasTransaction()) {
                throw new IllegalStateException("Transaction must be active during fetch operation."); //$NON-NLS-1$
            }
            // Call back closes session once calling code has consumed all results.
            Set<ResultsCallback> callbacks = Collections.<ResultsCallback> singleton(new ResultsCallback() {

                private boolean hasBeginCallBack = false;

                @Override
                public void onBeginOfResults() {
                    storageClassLoader.bind(Thread.currentThread());
                    hasBeginCallBack = true;
                }

                @Override
                public void onEndOfResults() {
                    if (hasBeginCallBack) {
                        storageClassLoader.unbind(Thread.currentThread());
                    }
                }
            });
            return internalFetch(session, userQuery, callbacks);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred during fetch operation", e); //$NON-NLS-1$
        } finally {
            this.releaseSession();
            storageClassLoader.unbind(Thread.currentThread());
        }
    }

    @Override
    public void update(DataRecord record) {
        update(Collections.singleton(record));
    }

    @Override
    public void update(Iterable<DataRecord> records) {
        assertPrepared();
        Session session = this.getCurrentSession();
        try {
            storageClassLoader.bind(Thread.currentThread());
            DataRecordConverter<Object> converter = new ObjectDataRecordConverter(storageClassLoader, session);
            for (DataRecord currentDataRecord : records) {
                TypeMapping mapping = mappingRepository.getMappingFromUser(currentDataRecord.getType());
                Wrapper o = (Wrapper) converter.convert(currentDataRecord, mapping);
                if (session.contains(o) && session.isReadOnly(o)) { // A read only instance for an update?
                    // Session#setReadOnly(...) does not always work as expected (especially in case of compound keys
                    // see TMDM-7014).
                    session.evict(o);
                    o = (Wrapper) converter.convert(currentDataRecord, mapping);
                }
                DataRecordMetadata recordMetadata = currentDataRecord.getRecordMetadata();
                Map<String, String> recordProperties = recordMetadata.getRecordProperties();
                if (!ObjectUtils.equals(recordMetadata.getTaskId(), o.taskId())) {
                    o.taskId(recordMetadata.getTaskId());
                }
                for (Map.Entry<String, String> currentProperty : recordProperties.entrySet()) {
                    String key = currentProperty.getKey();
                    String value = currentProperty.getValue();
                    ComplexTypeMetadata database = mapping.getDatabase();
                    if (database.hasField(key)) {
                        Object convertedValue = StorageMetadataUtils.convert(value, database.getField(key));
                        if (!ObjectUtils.equals(convertedValue, o.get(key))) {
                            o.set(key, convertedValue);
                        }
                    } else {
                        throw new IllegalArgumentException("Can not store value '" + key //$NON-NLS-1$
                                + "' because there is no database field '" + key + "' in type '" + mapping.getName() //$NON-NLS-1$ //$NON-NLS-2$
                                + "' (storage is '" + toString() + "')"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                session.saveOrUpdate(o);
                if (FLUSH_ON_LOAD && session.getStatistics().getEntityCount() % batchSize == 0) {
                    // Periodically flush objects to avoid using too much memory.
                    session.flush();
                    session.clear();
                }
            }
        } catch (ConstraintViolationException e) {
            throw new com.amalto.core.storage.exception.ConstraintViolationException(e);
        } catch (PropertyValueException e) {
            throw new RuntimeException("Invalid value in record to update.", e); //$NON-NLS-1$
        } catch (NonUniqueObjectException e) {
            throw new RuntimeException("Attempted to update multiple times same record within same transaction.", e); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred during update.", e); //$NON-NLS-1$
        } finally {
            this.releaseSession();
            storageClassLoader.unbind(Thread.currentThread());
        }
    }

    /**
     * 
     * Update METADATA_STAGING_HAS_TASK to true (TMDM-11110)
     * 
     * @param type
     * @param taskId
     */
    public void updateHasTask(ComplexTypeMetadata type, String taskId) {
        Session session = this.getCurrentSession();
        try {
            storageClassLoader.bind(Thread.currentThread());
            String databaseName = mappingRepository.getMappingFromUser(type).getDatabase().getName();
            String className = storageClassLoader.findClass(databaseName).getSimpleName();
            String queryString = "update " + className + " set x_talend_staging_hastask = ?0 where x_talend_task_id = ?1"; //$NON-NLS-1$ //$NON-NLS-2$
            Query query = session.createQuery(queryString).setParameter(0, true).setParameter(1, taskId);
            LOGGER.info("Calling update task statement : " + queryString);
            query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred during update METADATA_STAGING_HAS_TASK.", e); //$NON-NLS-1$
        } finally {
            this.releaseSession();
            storageClassLoader.unbind(Thread.currentThread());
        }
    }

    /**
     * Get METADATA_STAGING_OLD_GROUP list by Task ID
     * 
     * @param type
     * @param taskId
     * @return
     */
    @Override
    public List<String> getOldGroups(ComplexTypeMetadata type, String taskId) {
        Session session = this.getCurrentSession();
        try {
            storageClassLoader.bind(Thread.currentThread());
            String databaseName = mappingRepository.getMappingFromUser(type).getDatabase().getName();
            String className = storageClassLoader.findClass(databaseName).getSimpleName();
            String queryString = "SELECT distinct x_talend_staging_oldgroup FROM " + className + " WHERE x_talend_task_id = ?0"; //$NON-NLS-1$ //$NON-NLS-2$
            LOGGER.info("Calling query old group statement : " + queryString);
            return session.createQuery(queryString).setParameter(0, taskId).list();
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred during query METADATA_STAGING_OLD_GROUP.", e); //$NON-NLS-1$
        } finally {
            this.releaseSession();
            storageClassLoader.unbind(Thread.currentThread());
        }
    }

    @Override
    public void begin() {
        assertPrepared();
        StorageTransaction storageTransaction = this.getCurrentStorageTransaction();
        storageTransaction.begin();
    }

    @Override
    public void commit() {
        assertPrepared();
        StorageTransaction storageTransaction = this.getCurrentStorageTransaction();
        storageTransaction.commit();
    }

    @Override
    public void rollback() {
        assertPrepared();
        StorageTransaction storageTransaction = this.getCurrentStorageTransaction();
        storageTransaction.rollback();
    }

    private StorageTransaction getCurrentStorageTransaction() {
        TransactionManager transactionManager = ServerContext.INSTANCE.get().getTransactionManager();
        com.amalto.core.storage.transaction.Transaction currentTransaction = transactionManager.currentTransaction();
        StorageTransaction storageTransaction = currentTransaction.include(this);
        if (!storageTransaction.getCoordinator().equals(currentTransaction)) {
            LOGGER.warn("Current transaction returned by TransactionManager [" + currentTransaction //$NON-NLS-1$
                    + "] does not match storageTransaction coordinator [" + storageTransaction.getCoordinator() //$NON-NLS-1$
                    + "]. There is something wrong in TransactionManager"); //$NON-NLS-1$
        }
        return storageTransaction;
    }

    @Override
    public synchronized void end() {
        // TODO Remove
    }

    @Override
    public void reindex() {
        if (!dataSource.supportFullText()) {
            LOGGER.error("Can not reindex storage '" + storageName + "': datasource '" + dataSource.getName() //$NON-NLS-1$ //$NON-NLS-2$
                    + "' does not support full text."); //$NON-NLS-1$
            return;
        }
        LOGGER.info("Re-indexing full-text for " + storageName + "..."); //$NON-NLS-1$ //$NON-NLS-2$
        Session session = this.getCurrentSession();
        try {
            FullTextSession fullTextSession = Search.getFullTextSession(session);
            MassIndexer indexer = fullTextSession.createIndexer();
            indexer.optimizeOnFinish(true);
            indexer.optimizeAfterPurge(true);
            indexer.idFetchSize(generateIdFetchSize())
                   .threadsToLoadObjects(1)
                   .cacheMode(CacheMode.IGNORE)
                   .typesToIndexInParallel(5)
                   .batchSizeToLoadObjects(batchSize)
                   .progressMonitor(new IndexingProgressMonitor()) //a MassIndexerProgressMonitor implementation
                   .startAndWait();
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred when re-indexing full-text for " + storageName + ".", e); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            this.releaseSession();
            LOGGER.info("Re-indexing done."); //$NON-NLS-1$
        }
    }

    private int generateIdFetchSize() {
        if (dataSource.getDialectName() == RDBMSDataSource.DataSourceDialect.MYSQL) {
            // for using "stream resultset" to resolve OOM
            return Integer.MIN_VALUE;
        } else {
            return fetchSize;
        }
    }

    @Override
    public Set<String> getFullTextSuggestion(String keyword, FullTextSuggestion mode, int suggestionSize) {
        throw new UnsupportedOperationException("No support due to version of Lucene in use."); //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return storageName;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public StorageType getType() {
        return storageType;
    }
    
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public ImpactAnalyzer getImpactAnalyzer() {
        switch (storageType) {
        case MASTER:
        case STAGING:
            return new HibernateStorageDataAnaylzer(this);
        case SYSTEM:
            return diffResult -> Collections.emptyMap();
        default:
            throw new NotImplementedException("No support for storage type '" + storageType + "'."); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void analyzeChanges(Set<ComplexTypeMetadata> typesToDrop, List<Change> changes) {
        for (Change change : changes) {
            MetadataVisitable element = change.getElement();
            if (element instanceof FieldMetadata) {
                typesToDrop.add(((FieldMetadata) element).getContainingType().getEntity());
            } else if (element instanceof ComplexTypeMetadata) {
                typesToDrop.add((ComplexTypeMetadata) element);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Change '" + change.getMessage(Locale.getDefault()) + "' requires a database schema update."); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    public Set<ComplexTypeMetadata> findTypesToDelete(boolean force, Compare.DiffResults diffResults) {
        Map<ImpactAnalyzer.Impact, List<Change>> impacts = getImpactsResult(diffResults);
        Set<ComplexTypeMetadata> typesToDrop = new HashSet<>();
        for (Map.Entry<ImpactAnalyzer.Impact, List<Change>> impactCategory : impacts.entrySet()) {
            ImpactAnalyzer.Impact category = impactCategory.getKey();
            List<Change> changes = impactCategory.getValue();
            switch (category) {
            case HIGH:
                if (!changes.isEmpty()) {
                    if (force) {
                        analyzeChanges(typesToDrop, changes);
                    } else {
                        // High changes without force=true is an error
                        throw new IllegalArgumentException("Some changes require force parameter."); //$NON-NLS-1$
                    }
                }
            case MEDIUM:
                if (!impactCategory.getValue().isEmpty()) {
                    if (force) {
                        analyzeChanges(typesToDrop, changes);
                    }
                    // Change from high change: no exception if force=false (no schema update).
                }
                break;
            case LOW:
                if (LOGGER.isTraceEnabled()) {
                    for (Change change : impactCategory.getValue()) {
                        LOGGER.trace("Change '" + change.getMessage(Locale.getDefault()) //$NON-NLS-1$
                                + "' does NOT require a database schema update."); //$NON-NLS-1$
                    }
                    break;
                }
            }
        }
        return typesToDrop;
    }

    private Set<ComplexTypeMetadata> findChangedTypes(Compare.DiffResults diffResults) {
        Map<ImpactAnalyzer.Impact, List<Change>> impacts = getImpactsResult(diffResults);
        Set<ComplexTypeMetadata> typesToDrop = new HashSet<>();
        for (Map.Entry<ImpactAnalyzer.Impact, List<Change>> impactCategory : impacts.entrySet()) {
            ImpactAnalyzer.Impact category = impactCategory.getKey();
            List<Change> changes = impactCategory.getValue();
            switch (category) {
            case HIGH:
                break;
            case MEDIUM:
            case LOW:
                if (!changes.isEmpty()) {
                    analyzeChanges(typesToDrop, changes);
                }
            }
        }
        return typesToDrop;
    }

    private Set<ComplexTypeMetadata> findDependentTypesToDelete(MetadataRepository previousRepository,
            Set<ComplexTypeMetadata> typesToDrop, Set<ComplexTypeMetadata> allDependencies) {
        Set<ComplexTypeMetadata> additionalTypes = new HashSet<>();
        for (ComplexTypeMetadata typeToDrop : typesToDrop) {
            Set<ReferenceFieldMetadata> inboundReferences = previousRepository.accept(new InboundReferences(typeToDrop));
            if (!inboundReferences.isEmpty()) {
                for (ReferenceFieldMetadata inboundReference : inboundReferences) {
                    ComplexTypeMetadata entity = inboundReference.getContainingType().getEntity();
                    if (!allDependencies.contains(entity)) {
                        additionalTypes.add(entity);
                        allDependencies.add(entity);
                    }
                }
                additionalTypes.addAll(findDependentTypesToDelete(previousRepository, additionalTypes, allDependencies));
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(additionalTypes.size() + " additional type(s) scheduled for deletion (inbound references): " //$NON-NLS-1$
                    + Arrays.toString(additionalTypes.toArray()) + "."); //$NON-NLS-1$
        }
        return additionalTypes;
    }

    private void cleanFullTextIndex(List<ComplexTypeMetadata> sortedTypesToDrop) {
        if (dataSource.supportFullText()) {
            try {
                for (ComplexTypeMetadata typeMetadata : sortedTypesToDrop) {
                    try {
                        Class<?> clazz = storageClassLoader.loadClass(ClassCreator.getClassName(typeMetadata.getName()));
                        File directoryFile = new File(dataSource.getIndexDirectory() + '/' + getName() + '/' + clazz.getName());
                        if (directoryFile.exists()) {
                            final Directory directory = FSDirectory.open(directoryFile.toPath());
                            final String lockName = "delete." + typeMetadata.getName(); //$NON-NLS-1$
                            try (final Lock lock = directory.obtainLock(lockName)) {
                                String[] files = directory.listAll();
                                for (String file : files) {
                                    if (!file.endsWith(lockName)) { // Don't delete our own lock
                                        directory.deleteFile(file);
                                    }
                                }
                            }
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Removed full text directory for entity '" + typeMetadata.getName() + "' at '" //$NON-NLS-1$ //$NON-NLS-2$
                                        + directoryFile.getAbsolutePath() + "'"); //$NON-NLS-1$
                            }
                        } else {
                            LOGGER.warn("Full text index directory for entity '" + typeMetadata.getName() //$NON-NLS-1$
                                    + "' no longer exists. No need to delete it."); //$NON-NLS-1$
                        }
                    } catch (Exception e) {
                        LOGGER.error("Could not remove full text directory for '" + typeMetadata.getName() + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Could not correctly clean full text directory.", e); //$NON-NLS-1$
            }
        }
    }

    private void cleanUpdateReports(List<ComplexTypeMetadata> sortedTypesToDrop) {
        StorageAdmin storageAdmin = ServerContext.INSTANCE.get().getStorageAdmin();
        Storage storage = storageAdmin.get(XSystemObjects.DC_UPDATE_PREPORT.getName(), StorageType.MASTER);
        try {
            if (storage == null) {
                LOGGER.warn("No update report storage available."); //$NON-NLS-1$
            } else {
                ComplexTypeMetadata update = storage.getMetadataRepository().getComplexType("Update"); //$NON-NLS-1$
                storage.begin();
                for (ComplexTypeMetadata type : sortedTypesToDrop) {
                    try {
                        UserQueryBuilder qb = from(update).where(and(eq(update.getField("Concept"), type.getName()), //$NON-NLS-1$
                                eq(update.getField("DataCluster"), getName()))); //$NON-NLS-1$
                        storage.delete(qb.getExpression());
                    } catch (Exception e) {
                        LOGGER.error("Could not remove update reports for '" + type.getName() + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                storage.commit();
            }
        } catch (Exception e) {
            if (storage != null) {
                storage.rollback();
            }
            LOGGER.error("Could not correctly clean update reports", e); //$NON-NLS-1$
        }
    }

    private void cleanRecycleBins(List<ComplexTypeMetadata> sortedTypesToDrop) {
        StorageAdmin storageAdmin = ServerContext.INSTANCE.get().getStorageAdmin();
        Storage storage = storageAdmin.get(StorageAdmin.SYSTEM_STORAGE, StorageType.SYSTEM);
        try {
            if (storage == null) {
                LOGGER.warn("No system storage available."); //$NON-NLS-1$
            } else {
                ComplexTypeMetadata droppedItem = storage.getMetadataRepository().getComplexType("dropped-item-pOJO"); //$NON-NLS-1$
                storage.begin();
                for (ComplexTypeMetadata type : sortedTypesToDrop) {
                    try {
                        UserQueryBuilder qb = from(droppedItem).where(eq(droppedItem.getField("concept-name"), type.getName())); //$NON-NLS-1$
                        storage.delete(qb.getExpression());
                    } catch (Exception e) {
                        LOGGER.error("Could not remove dropped items for '" + type.getName() + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                storage.commit();
            }
        } catch (Exception e) {
            if (storage != null) {
                storage.rollback();
            }
            LOGGER.error("Could not correctly clean dropped items", e); //$NON-NLS-1$
        }
    }

    @Override
    public Set<String> findTablesToDrop(List<ComplexTypeMetadata> sortedTypesToDrop) {
        Set<String> tablesToDrop = new LinkedHashSet<>();
        TableClosureVisitor visitor = new TableClosureVisitor();
        // Drop table order should be reversed
        for (int i = sortedTypesToDrop.size() - 1; i >= 0; i--) {
            ComplexTypeMetadata typeMetadata = sortedTypesToDrop.get(i);
            String typeName = typeMetadata.getName();
            if (!typeMetadata.isInstantiable()) {
                typeName = "X_" + typeName; //$NON-NLS-1$
            }
            PersistentClass tempMetadata = metadata.getEntityBinding(ClassCreator.getClassName(typeName));
            if (tempMetadata != null) {
                tablesToDrop.addAll((Collection<String>) tempMetadata.accept(visitor));
            } else {
                LOGGER.warn("Could not find table names for type '" + typeMetadata.getName() + "'."); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        if (LOGGER.isTraceEnabled()) {
            StringBuilder aggregatedTableNames = new StringBuilder();
            for (String table : tablesToDrop) {
                aggregatedTableNames.append(table).append(' ');
            }
            LOGGER.trace("Table(s) scheduled for drop: " + aggregatedTableNames); //$NON-NLS-1$
        }
        return tablesToDrop;
    }

    private void cleanImpactedTables(List<ComplexTypeMetadata> sortedTypesToDrop) {
        Set<String> tablesToDrop = findTablesToDrop(sortedTypesToDrop);
        int totalCount = tablesToDrop.size();
        int totalRound = 0;
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(dataSource.getConnectionURL(), dataSource.getUserName(),
                    dataSource.getPassword());
            // clean all constraint foreign key being drop tables
            ForeignKeyUpdateStrategy.cleanOverdueForeignKeys(dataSource.getDialectName(), tablesToDrop, connection);
            int successCount = 0;
            while (successCount < totalCount && totalRound++ < totalCount) {
                Set<String> dropedTables = new HashSet<>();
                for (String table : tablesToDrop) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("DROP TABLE " + matchCommonRegex(table)); //$NON-NLS-1$
                        dropedTables.add(table);
                        successCount++;
                    } catch (SQLException e) {
                        LOGGER.error("Could not delete '" + table + "' in round " + totalRound
                                + '.'); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }
                }
                tablesToDrop.removeAll(dropedTables);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Successfully deleted " + successCount + " tables (out of " + totalCount + " tables) in " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        + totalRound + " rounds."); //$NON-NLS-1$
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not acquire connection to database.", e); //$NON-NLS-1$
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Unexpected error on connection close.", e); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void adapt(MetadataRepository newRepository, boolean force) {
        if (newRepository == null) {
            throw new IllegalArgumentException("New data model can not be null."); //$NON-NLS-1$
        }
        MetadataRepository previousRepository = getMetadataRepository();
        Compare.DiffResults diffResults = Compare.compare(previousRepository, newRepository);

        // Get low, medium changed types list that don't contains dependent type
        List<ComplexTypeMetadata> changedTypesToDrop = findChangedTypesToDrop(diffResults);
        // Get high, medium changed types list that contains dependent type
        List<ComplexTypeMetadata> sortedTypesToDrop = findSortedTypesToDrop(diffResults, force);

        if (sortedTypesToDrop.size() > 0) {
            cleanUpdateReports(sortedTypesToDrop);
            cleanRecycleBins(sortedTypesToDrop);
            cleanFullTextIndex(sortedTypesToDrop);
            cleanImpactedTables(sortedTypesToDrop);
        }

        changedTypesToDrop.removeAll(sortedTypesToDrop);
        if (changedTypesToDrop.size() > 0) {
            cleanUpdateReports(changedTypesToDrop);
            cleanRecycleBins(changedTypesToDrop);
        }

        // Reinitialize Hibernate
        LOGGER.info("Completing database schema update..."); //$NON-NLS-1$

        // for the liquibase
        if (!force) {
            Map<ImpactAnalyzer.Impact, List<Change>> impacts = getImpactsResult(diffResults);
            if (impacts.get(ImpactAnalyzer.Impact.HIGH).size() > 0) {
                throw new RuntimeException(
                        "Unable to complete database schema update, have High impact change but not clean impacted tabled."); //$NON-NLS-1$
            }

            Connection connection = null;
            try {
                SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) this.getCurrentSession().getSessionFactory();
                Dialect dialect = sessionFactoryImplementor.getJdbcServices().getDialect();
                connection = ((SessionImpl)this.getCurrentSession()).connection();

                LiquibaseSchemaAdapter liquibaseChange = new LiquibaseSchemaAdapter(tableResolver, dialect, (RDBMSDataSource) this.getDataSource(), this.getType());
                liquibaseChange.setMetadata(metadata);
                liquibaseChange.adapt(connection, diffResults);
            } catch (Exception e) {
                String msg = "Unable to complete database schema update, execute liquibase failed."; //$NON-NLS-1$
                LOGGER.error(msg, e);
                throw new RuntimeException(msg, e);
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        LOGGER.error("Failed to close connection for liquibase", e);
                    }
                }
            }
        }

        try {
            close(false);
            prepare(newRepository, false);
            LOGGER.info("Database schema update complete."); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RuntimeException("Unable to complete database schema update.", e); //$NON-NLS-1$
        }
    }

    private Map<ImpactAnalyzer.Impact, List<Change>> getImpactsResult(DiffResults diffResults) {
        ImpactAnalyzer analyzer = getImpactAnalyzer();
        return analyzer.analyzeImpacts(diffResults);
    }

    private List<ComplexTypeMetadata> findChangedTypesToDrop(Compare.DiffResults diffResults) {
        List<ComplexTypeMetadata> sortedTypesToDrop = new ArrayList<>();
        MetadataRepository previousRepository = getMetadataRepository();
        if (diffResults.getActions().isEmpty()) {
            LOGGER.info("No change detected, no database schema update to perform."); //$NON-NLS-1$
        } else {
            // Analyze impact to find types to delete
            Set<ComplexTypeMetadata> typesToDrop = findChangedTypes(diffResults);
            if (!typesToDrop.isEmpty()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(typesToDrop.size() + " type(s) scheduled for deletion: " + Arrays.toString(typesToDrop.toArray()) //$NON-NLS-1$
                            + "."); //$NON-NLS-1$
                }
                // Sort in dependency order
                sortedTypesToDrop = new ArrayList<>(typesToDrop);
                if (sortedTypesToDrop.size() > 1) {
                    sortedTypesToDrop = MetadataUtils.sortTypes(previousRepository, sortedTypesToDrop, SortType.LENIENT);
                }
            } else {
                LOGGER.info("Schema changes do no require to drop any database schema element."); //$NON-NLS-1$
            }
        }
        return sortedTypesToDrop;
    }

    @Override
    public List<ComplexTypeMetadata> findSortedTypesToDrop(Compare.DiffResults diffResults, boolean force) {
        List<ComplexTypeMetadata> sortedTypesToDrop = new ArrayList<>();
        MetadataRepository previousRepository = getMetadataRepository();
        if (diffResults.getActions().isEmpty()) {
            LOGGER.info("No change detected, no database schema update to perform."); //$NON-NLS-1$
        } else {
            // Analyze impact to find types to delete
            Set<ComplexTypeMetadata> typesToDrop = findTypesToDelete(force, diffResults);
            if (!typesToDrop.isEmpty()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(typesToDrop.size() + " type(s) scheduled for deletion: " + Arrays.toString(typesToDrop.toArray()) //$NON-NLS-1$
                            + "."); //$NON-NLS-1$
                }
                // Find dependent types to delete
                Set<ComplexTypeMetadata> allDependencies = new HashSet<>(typesToDrop);
                Set<ComplexTypeMetadata> dependentTypesToDrop = findDependentTypesToDelete(previousRepository, typesToDrop, allDependencies);
                typesToDrop.addAll(dependentTypesToDrop);
                // Sort in dependency order
                sortedTypesToDrop = new ArrayList<>(typesToDrop);
                if (sortedTypesToDrop.size() > 1) {
                    sortedTypesToDrop = MetadataUtils.sortTypes(previousRepository, sortedTypesToDrop, SortType.LENIENT);
                }
            } else {
                LOGGER.info("Schema changes do no require to drop any database schema element."); //$NON-NLS-1$
            }
        }
        return sortedTypesToDrop;
    }

    @Override
    public synchronized boolean isClosed() {
        return factory == null || factory.isClosed();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void delete(Expression userQuery) {
        Session session = this.getCurrentSession();
        try {
            storageClassLoader.bind(Thread.currentThread());
            // Session session = factory.getCurrentSession();
            userQuery = userQuery.normalize(); // First do a normalize for correct optimization detection.
            // Check if optimized delete for one type (and no filter) is applicable
            if (userQuery instanceof Select) {
                Select select = (Select) userQuery;
                List<ComplexTypeMetadata> types = select.getTypes();
                if (types.size() == 1 && select.getCondition() == null) {
                    FlushMode previousFlushMode = session.getHibernateFlushMode();
                    try {
                        session.setHibernateFlushMode(FlushMode.ALWAYS); // Force Hibernate to actually send SQL query to database during delete.
                        ComplexTypeMetadata mainType = types.get(0);
                        TypeMapping mapping = mappingRepository.getMappingFromUser(mainType);
                        // Compute (and eventually sort) types to delete
                        List<ComplexTypeMetadata> typesToDelete;
                        MetadataRepository internalRepository = typeMappingRepository.getInternalRepository();
                        if (mapping instanceof ScatteredTypeMapping) {
                            MetadataVisitor<List<ComplexTypeMetadata>> transitiveClosure = new TypeTransitiveClosure();
                            List<ComplexTypeMetadata> typeClosure = mapping.getDatabase().accept(transitiveClosure);
                            typesToDelete = MetadataUtils.sortTypes(internalRepository, typeClosure);
                        } else {
                            Collection<ComplexTypeMetadata> subTypes = mapping.getDatabase().getSubTypes();
                            if (subTypes.isEmpty()) {
                                typesToDelete = Collections.singletonList(mapping.getDatabase());
                            } else {
                                typesToDelete = new ArrayList<>(subTypes.size() + 1);
                                typesToDelete.add(mapping.getDatabase());
                                typesToDelete.addAll(subTypes);
                            }
                        }
                        Map<ComplexTypeMetadata, Map<String, List>> recordsToDeleteMap = new HashMap<>();
                        for (ComplexTypeMetadata typeToDelete : typesToDelete) {
                            typeToDelete.getFields().stream()
                                    .filter(item -> item.isMany() && item instanceof ReferenceFieldMetadata).forEach(type -> {
                                String formattedTableName = matchCommonRegex(tableResolver.getCollectionTable(type));
                                session.createSQLQuery(DELETE_FROM_STR + formattedTableName).executeUpdate();
                            });
                            InboundReferences inboundReferences = new InboundReferences(typeToDelete);
                            Set<ReferenceFieldMetadata> references = internalRepository.accept(inboundReferences);
                            // Empty values from intermediate tables to this non instantiable type and unset inbound
                            // references
                            if (typeToDelete.equals(mainType)) {
                                for (ReferenceFieldMetadata reference : references) {
                                    if (reference.isMany()) {
                                        // No need to check for mandatory collections of references since constraint
                                        // cannot be expressed in db schema
                                        String formattedTableName = matchCommonRegex(tableResolver.getCollectionTable(reference));
                                        session.createSQLQuery(DELETE_FROM_STR + formattedTableName).executeUpdate();
                                    } else {
                                        String referenceTableName = matchCommonRegex(tableResolver.get(reference.getContainingType()));
                                        if (referenceTableName.startsWith("X_ANONYMOUS")) { //$NON-NLS-1$
                                            session.createSQLQuery(DELETE_FROM_STR + referenceTableName).executeUpdate();
                                        } else if(referenceTableName.startsWith("X_")){ //$NON-NLS-1$
                                            //if used the inherit and it's a subType, the table name used the superType's name
                                            Collection<TypeMetadata> superTypes = reference.getContainingType().getSuperTypes();
                                            if (!superTypes.isEmpty()) {
                                                referenceTableName = tableResolver
                                                        .get((ComplexTypeMetadata) superTypes.iterator().next());
                                            }
                                            //update the reference field to null
                                            String setToNullHql = "UPDATE " + referenceTableName //$NON-NLS-1$
                                                    + " SET " + matchCommonRegex(tableResolver //$NON-NLS-1$
                                                            .get(reference.getReferencedField(), reference.getName()))
                                                    + " = NULL"; //$NON-NLS-1$
                                            session.createSQLQuery(setToNullHql).executeUpdate();
                                        }
                                    }
                                }
                            } else {
                                HashMap<String, List> fieldsCondition = new HashMap<>();
                                boolean isNeedToDropDeleteType = false;
                                for (ReferenceFieldMetadata reference : references) {
                                    if (reference.getContainingType().equals(mainType)) {
                                        isNeedToDropDeleteType = true;
                                        if (reference.isMany()) {
                                            // No need to check for mandatory collections of references since constraint
                                            // cannot
                                            // be expressed in db schema
                                            String formattedTableName = tableResolver.getCollectionTable(reference);
                                            session.createSQLQuery(
                                                    DELETE_FROM_STR + matchCommonRegex(formattedTableName))
                                                    .executeUpdate();
                                        } else {
                                            String columnName = tableResolver.get(reference.getReferencedField());
                                            String referenceTableName;
                                            if(reference.getContainingType().getSuperTypes().isEmpty()){
                                                referenceTableName = tableResolver.get(reference.getContainingType());
                                            } else {
                                                ComplexTypeMetadata superType = (ComplexTypeMetadata)reference.getContainingType().getSuperTypes().iterator().next();
                                                referenceTableName = tableResolver.get(superType);
                                            }
                                            if (reference.getReferencedField() instanceof CompoundFieldMetadata) {
                                                FieldMetadata[] fields = ((CompoundFieldMetadata) reference.getReferencedField())
                                                        .getFields();
                                                for (FieldMetadata field : fields) {
                                                    List list = session
                                                            .createSQLQuery("select " //$NON-NLS-1$
                                                                    + matchCommonRegex(
                                                                            tableResolver.get(field, reference.getName()))
                                                                    + " from " //$NON-NLS-1$
                                                                    + matchCommonRegex(referenceTableName))
                                                            .list();
                                                    if (list != null && !list.isEmpty()) {
                                                        fieldsCondition.put(columnName, list);
                                                    } else {
                                                        isNeedToDropDeleteType = false;
                                                    }
                                                }
                                            } else {
                                                List list = session.createSQLQuery("select " //$NON-NLS-1$
                                                        + matchCommonRegex(tableResolver
                                                                .get(reference.getReferencedField(), reference.getName()))
                                                        + " from " + matchCommonRegex(referenceTableName)).list(); //$NON-NLS-1$
                                                if (list != null && !list.isEmpty()) {
                                                    if (fieldsCondition.containsKey(columnName)) {
                                                        List originList = fieldsCondition.get(columnName);
                                                        originList.addAll(list);
                                                        fieldsCondition.replace(columnName, fieldsCondition.get(columnName), originList);
                                                    } else {
                                                        fieldsCondition.put(columnName, list);
                                                    }
                                                } else {
                                                    isNeedToDropDeleteType = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (isNeedToDropDeleteType) {
                                    recordsToDeleteMap.put(typeToDelete, fieldsCondition);
                                }
                            }
                        }
                        deleteData(session, mapping.getDatabase(), new HashMap<>(), mapping);
                        typesToDelete.forEach(typeToDelete -> {
                            if (recordsToDeleteMap.containsKey(typeToDelete)) {
                                deleteData(session, typeToDelete, recordsToDeleteMap.get(typeToDelete), mapping);
                            }
                        });
                    } finally {
                        session.setHibernateFlushMode(previousFlushMode);
                    }
                    return;
                }
            }
            // Generic fall back for deletions (filter)
            if (userQuery instanceof Select) {
                ((Select) userQuery).setForUpdate(true);
            }
            Iterable<DataRecord> records = internalFetch(session, userQuery, Collections.<ResultsCallback> emptySet());
            for (DataRecord currentDataRecord : records) {
                List<String> types = new ArrayList<>();
                if (userQuery instanceof Select) {
                    List<ComplexTypeMetadata> selectTypes = ((Select) userQuery).getTypes();
                    for (ComplexTypeMetadata complexTypeMetadata : selectTypes) {
                        Collection<ComplexTypeMetadata> subTypes = complexTypeMetadata.getSubTypes();
                        for (ComplexTypeMetadata subComplexTypeMetadata : subTypes) {
                            if (subComplexTypeMetadata.isInstantiable()) {
                                types.add(subComplexTypeMetadata.getName());
                            }
                        }
                        types.add(complexTypeMetadata.getName());
                    }
                }
                if (types.isEmpty() || types.contains(currentDataRecord.getType().getName())) {
                    delete(currentDataRecord);
                }
            }
        } catch (ConstraintViolationException e) {
            throw new com.amalto.core.storage.exception.ConstraintViolationException(e);
        } catch (HibernateException e) {
            throw new RuntimeException(e);
        } finally {
            this.releaseSession();
            storageClassLoader.unbind(Thread.currentThread());
        }
    }

    @SuppressWarnings("rawtypes")
    private void deleteData(Session session, ComplexTypeMetadata typeToDelete, Map<String, List> condition, TypeMapping mapping) {
        try {
            for (FieldMetadata field : typeToDelete.getFields()) {
                if (field.isMany()) {
                    String formattedTableName = tableResolver.getCollectionTable(field);
                    String deleteFormattedTableSQL = DELETE_FROM_STR + matchCommonRegex(formattedTableName);
                    deleteDataWithConditionForRepeatedField(session, condition, deleteFormattedTableSQL);
                }
            }
            // Delete the type instances
            String className = storageClassLoader.getClassFromType(typeToDelete).getName();

            String hql = DELETE_FROM_STR + className;
            deleteDataWithCondition(session, condition, hql);

            // Clean up full text indexes
            if (dataSource.supportFullText()) {
                FullTextSession fullTextSession = Search.getFullTextSession(session);
                Set<Class<?>> indexedTypes = fullTextSession.getSearchFactory().getIndexedTypes();
                Class<? extends Wrapper> entityType = storageClassLoader.getClassFromType(mapping.getDatabase());
                if (indexedTypes.contains(entityType)) {
                    fullTextSession.purgeAll(entityType);
                } else {
                    LOGGER.warn("Unable to delete full text indexes for '" + entityType + "' (not indexed)."); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to delete table '" + storageClassLoader.getClassFromType(typeToDelete).getName() + "'."); //$NON-NLS-1$ //$NON-NLS-2$
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void deleteDataWithConditionForRepeatedField(Session session, Map<String, List> condition, String sql) {
        if (condition.isEmpty()) {
            Query query = session.createSQLQuery(sql);
            query.executeUpdate();
            return;
        }

        sql = sql + " where "; //$NON-NLS-1$
        for (Entry<String, List> fieldEntry : condition.entrySet()) {
            List<String> list = fieldEntry.getValue();

            for (int i = 0; i < list.size(); i = i + MAX_DELETE_RECORDS) {
                String conditionString = StringUtils.EMPTY;
                StringBuffer buffer = new StringBuffer();
                int toIndex = i + MAX_DELETE_RECORDS;
                if (toIndex > list.size()) {
                    toIndex = list.size();
                }
                List tmp = list.subList(i, toIndex);
                for (int j = 0; j < tmp.size(); j++) {
                    if (tmp.get(j) != null) {
                        matchCommonRegex(tmp.get(j).toString());
                    }
                    buffer.append("'").append(tmp.get(j)).append("'"); //$NON-NLS-1$//$NON-NLS-2$
                    if (j != tmp.size() - 1) {
                        buffer.append(','); //$NON-NLS-1$
                    }
                }
                if (buffer.length() > 0) {
                    conditionString = conditionString + matchCommonRegex(fieldEntry.getKey()) + " in (" //$NON-NLS-1$
                            + buffer.toString() + ')'; //$NON-NLS-2$
                }
                session.createSQLQuery(sql + conditionString).executeUpdate();
            }
        }
    }

    private void deleteDataWithCondition(Session session, Map<String, List> condition, String hql) {
        if (condition.isEmpty()) {
            Query query = session.createQuery(hql);
            query.executeUpdate();
            return;
        }

        StringBuilder hqlBuilder = new StringBuilder(hql + " where ");//$NON-NLS-1$
        for (Entry<String, List> fieldEntry : condition.entrySet()) {
            if (!hqlBuilder.toString().endsWith("where ")) { //$NON-NLS-1$
                hqlBuilder.append(" and "); //$NON-NLS-1$
            }
            hqlBuilder.append(fieldEntry.getKey()).append(" in (:").append(fieldEntry.getKey()).append(')'); //$NON-NLS-1$//$NON-NLS-2$
        }
        hql = hqlBuilder.toString();
        Query query;
        for (Entry<String, List> fieldEntry : condition.entrySet()) {
            query = session.createQuery(hql);
            List list = fieldEntry.getValue();
            for (int i = 0; i < list.size(); i = i + MAX_DELETE_RECORDS) {
                int toIndex = i + MAX_DELETE_RECORDS;
                if (toIndex > list.size()) {
                    toIndex = list.size();
                }
                query.setParameterList(fieldEntry.getKey(), list.subList(i, toIndex));
                query.executeUpdate();
            }
        }
    }

    @Override
    public void delete(DataRecord record) {
        Session session = this.getCurrentSession();
        try {
            storageClassLoader.bind(Thread.currentThread());
            // Session session = factory.getCurrentSession();
            ComplexTypeMetadata currentType = record.getType();
            TypeMapping mapping = mappingRepository.getMappingFromUser(currentType);
            if (mapping == null) {
                throw new IllegalArgumentException("Type '" + currentType.getName() + "' does not have a database mapping."); //$NON-NLS-1$ //$NON-NLS-2$
            }
            Class<?> clazz = storageClassLoader.getClassFromType(mapping.getDatabase());

            Serializable idValue;
            Collection<FieldMetadata> keyFields = currentType.getKeyFields();
            if (keyFields.size() == 1) {
                idValue = (Serializable) record.get(keyFields.iterator().next());
            } else {
                List<Object> compositeIdValues = new LinkedList<>();
                for (FieldMetadata keyField : keyFields) {
                    compositeIdValues.add(record.get(keyField));
                }
                idValue = ObjectDataRecordConverter.createCompositeId(storageClassLoader, clazz, compositeIdValues);
            }

            Wrapper object = (Wrapper) session.get(clazz, idValue, LockOptions.READ);
            if (object != null) {
                session.delete(object);
            } else {
                LOGGER.warn("Instance of type '" + currentType.getName() + "' and ID '" + idValue.toString() //$NON-NLS-1$ //$NON-NLS-2$
                        + "' has already been deleted within same transaction."); //$NON-NLS-1$
            }
        } catch (ConstraintViolationException e) {
            throw new com.amalto.core.storage.exception.ConstraintViolationException(e);
        } catch (HibernateException e) {
            throw new RuntimeException(e);
        } finally {
            this.releaseSession();
            storageClassLoader.unbind(Thread.currentThread());
        }
    }

    @Override
    public synchronized void close() {
        LOGGER.info("Closing storage '" + storageName + "' (" + storageType + ")."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        MDMTransactionSessionContext.forgetStorage(factory);
        try {
            if (storageClassLoader != null) {
                storageClassLoader.bind(Thread.currentThread());
            }
            // TMDM-8117: Excludes storage from transaction and rollback any pending transaction for proper close()
            TransactionManager transactionManager = ServerContext.INSTANCE.get().getTransactionManager();
            List<String> transactions = transactionManager.list();
            for (String transaction : transactions) {
                com.amalto.core.storage.transaction.Transaction activeTransaction = transactionManager.get(transaction);
                if (activeTransaction == null) {
                    throw new IllegalStateException("Transaction [" + transaction + "] should have been removed from current transactions.\n");
                }
                StorageTransaction storageTransaction = activeTransaction.exclude(this);
                if (storageTransaction != null) {
                    storageTransaction.rollback();
                }
            }
            // Close Hibernate session
            if (factory != null) {
                factory.close();
                factory = null; // SessionFactory#close() documentation advises to remove all references to SessionFactory.
            }
        } catch (HibernateException | NullPointerException e) {
            throw new RuntimeException("An exception occurred while closing storage '" + storageName + "' (" + storageType + ").\n" + e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } finally {
            if (storageClassLoader != null) {
                storageClassLoader.unbind(Thread.currentThread()); // TMDM-5934: Prevent restoring a closed classloader.
                storageClassLoader.close();
                storageClassLoader = null;
            }
            isPrepared = false;
            metadata = null;
        }
        // Reset caches
        ListIterator.resetTypeReaders();
        ScrollableIterator.resetTypeReaders();
        LOGGER.info("Storage '" + storageName + "' (" + storageType + ") closed."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public void close(boolean dropExistingData) {
        // Close hibernate so all connections get released before drop schema.
        close();
        if (dropExistingData) { // Drop schema if asked for...
            LOGGER.info("Deleting data and schema of storage '" + storageName + "' (" + storageType + ")."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            JDBCStorageCleaner cleaner = new JDBCStorageCleaner(new FullTextIndexCleaner());
            cleaner.clean(this);
            LOGGER.info("Data and schema of storage '" + storageName + "' (" + storageType + ") deleted."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    private StorageResults internalFetch(Session session, Expression userQuery, Set<ResultsCallback> callbacks) {
        // Always normalize the query to ensure query has expected format.
        Expression expression = userQuery.normalize();
        if (expression instanceof Select) {
            Select select = (Select) expression;
            // Contains optimizations (use of full text, disable it...)
            ConfigurableContainsOptimizer containsOptimizer = new ConfigurableContainsOptimizer(dataSource);
            containsOptimizer.optimize(select);
            // Remove implicit order by id if view set order by to None
            try {
                if (!OrderBy.OrderByNone.get()) {
                    // Implicit order by id for databases that need a order by (e.g. Postgres).
                    ImplicitOrderBy implicitOrderBy = new ImplicitOrderBy(dataSource);
                    implicitOrderBy.optimize(select);
                }
            } finally {
                OrderBy.OrderByNone.remove();
            }

            // Other optimizations
            for (Optimizer optimizer : OPTIMIZERS) {
                optimizer.optimize(select);
            }
        }
        expression = userQuery.normalize();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Query after optimizations:"); //$NON-NLS-1$
            userQuery.accept(new UserQueryDumpConsole(LOGGER));
        }
        // Analyze query
        SelectAnalyzer selectAnalysis = new SelectAnalyzer(mappingRepository, storageClassLoader, session, callbacks, this,
                tableResolver);
        Visitor<StorageResults> queryHandler = userQuery.accept(selectAnalysis);
        // Transform query using mappings
        Expression internalExpression = expression;
        if (expression instanceof Select) {
            List<ComplexTypeMetadata> types = ((Select) expression).getTypes();
            boolean isInternal = true;
            for (ComplexTypeMetadata type : types) {
                TypeMapping mapping = mappingRepository.getMappingFromUser(type);
                if (mapping != null) {
                    isInternal &= mapping.getDatabase() == type;
                }
            }
            if (!isInternal) {
                MappingExpressionTransformer transformer = new MappingExpressionTransformer(mappingRepository);
                // Normalize should not be needed, but adds it as safety
                internalExpression = expression.accept(transformer).normalize();
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Internal query after mappings:"); //$NON-NLS-1$
                userQuery.accept(new UserQueryDumpConsole(LOGGER, Level.TRACE));
            }
        }
        // Late database changes
        if (internalExpression instanceof Select) {
            Select select = (Select) internalExpression;
            // Query may use operators not compatible with underlying database.
            Optimizer incompatibleOperators = new IncompatibleOperators(dataSource);
            incompatibleOperators.optimize(select);
            select.normalize();
        }
        // Evaluate query
        return internalExpression.accept(queryHandler);
    }

    private void assertPrepared() {
        if (!isPrepared) {
            throw new IllegalStateException("Storage has not been prepared."); //$NON-NLS-1$
        }
        if (storageClassLoader == null || storageClassLoader.isClosed()) {
            throw new IllegalStateException("Storage has been closed."); //$NON-NLS-1$
        }
    }

    public StorageClassLoader getClassLoader() {
        return storageClassLoader;
    }

    protected Session getCurrentSession() {
        TransactionManager transactionManager = ServerContext.INSTANCE.get().getTransactionManager();
        com.amalto.core.storage.transaction.Transaction currentTransaction = transactionManager.currentTransaction();
        HibernateStorageTransaction storageTransaction = (HibernateStorageTransaction) currentTransaction.include(this);
        storageTransaction.acquireLock();
        return storageTransaction.getSession();

    }

    private void releaseSession() {
        TransactionManager transactionManager = ServerContext.INSTANCE.get().getTransactionManager();
        com.amalto.core.storage.transaction.Transaction currentTransaction = transactionManager.currentTransaction();
        HibernateStorageTransaction storageTransaction = (HibernateStorageTransaction) currentTransaction.include(this);
        storageTransaction.releaseLock();
    }

    @Override
    public String toString() {
        return storageName + '(' + storageType + ')';
    }

    private static class MetadataChecker extends DefaultMetadataVisitor<Object> {

        final Set<TypeMetadata> processedTypes = new HashSet<>();

        private static void assertField(FieldMetadata field) {
            if (field.getName().toLowerCase().startsWith(FORBIDDEN_PREFIX)) {
                throw new IllegalArgumentException("Field '" + field.getName() + "' of type '" //$NON-NLS-1$ //$NON-NLS-2$
                        + field.getContainingType().getName() + "' is not allowed to start with " + FORBIDDEN_PREFIX); //$NON-NLS-1$
            }
        }

        @Override
        public Object visit(SimpleTypeFieldMetadata simpleField) {
            String simpleFieldTypeName = simpleField.getType().getName();
            if (NoSupportTypes.getType(simpleFieldTypeName) != null) {
                throw new IllegalArgumentException("No support for field type '" + simpleFieldTypeName + "' (field '" //$NON-NLS-1$ //$NON-NLS-2$
                        + simpleField.getName() + "' of type '" + simpleField.getContainingType().getName() + "')."); //$NON-NLS-1$ //$NON-NLS-2$
            }
            assertField(simpleField);
            return super.visit(simpleField);
        }

        @Override
        public Object visit(ReferenceFieldMetadata referenceField) {
            assertField(referenceField);
            return super.visit(referenceField);
        }

        @Override
        public Object visit(ContainedTypeFieldMetadata containedField) {
            assertField(containedField);
            if (processedTypes.contains(containedField.getContainedType())) {
                return null;
            } else {
                processedTypes.add(containedField.getContainedType());
            }
            return super.visit(containedField);
        }

        @Override
        public Object visit(EnumerationFieldMetadata enumField) {
            assertField(enumField);
            return super.visit(enumField);
        }
    }

    private static class LocalEntityResolver implements EntityResolver {

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            if (StorageClassLoader.CONFIGURATION_PUBLIC_ID.equals(publicId)) {
                InputStream resourceAsStream = HibernateStorage.class.getResourceAsStream("hibernate.cfg.dtd"); //$NON-NLS-1$
                if (resourceAsStream == null) {
                    throw new IllegalStateException("Expected class path to contain Hibernate configuration DTD."); //$NON-NLS-1$
                }
                return new InputSource(resourceAsStream);
            } else if (StorageClassLoader.MAPPING_PUBLIC_ID.equals(publicId)) {
                InputStream resourceAsStream = HibernateStorage.class.getResourceAsStream("hibernate.hbm.dtd"); //$NON-NLS-1$
                if (resourceAsStream == null) {
                    throw new IllegalStateException("Expected class path to contain Hibernate mapping DTD."); //$NON-NLS-1$
                }
                return new InputSource(resourceAsStream);
            }
            return null;
        }
    }

    private static class TypeTransitiveClosure extends DefaultMetadataVisitor<List<ComplexTypeMetadata>> {

        private final List<ComplexTypeMetadata> types = new LinkedList<>();

        @Override
        public List<ComplexTypeMetadata> visit(ComplexTypeMetadata complexType) {
            if (types.isEmpty() || !complexType.isInstantiable()) {
                types.add(complexType);
                types.addAll(complexType.getSubTypes());
                for (ComplexTypeMetadata sub : complexType.getSubTypes()) {
                    super.visit(sub);
                }
                super.visit(complexType);
            }
            return types;
        }

        @Override
        public List<ComplexTypeMetadata> visit(ReferenceFieldMetadata referenceField) {
            ComplexTypeMetadata referencedType = referenceField.getReferencedType();
            referencedType.accept(this);
            return types;
        }
    }

    private class TableClosureVisitor implements PersistentClassVisitor {

        @SuppressWarnings("rawtypes")
        private List<String> getTableNames(PersistentClass persistentClass) {
            List<String> orderedTableNames = new LinkedList<>();
            // Add main table
            orderedTableNames.add(persistentClass.getTable().getName());
            // Add field's table
            Iterator propertyClosureIterator = persistentClass.getPropertyClosureIterator();
            ValueVisitor visitor = new ValueVisitor();
            while (propertyClosureIterator.hasNext()) {
                Property property = (Property) propertyClosureIterator.next();
                String tableName = (String) property.getValue().accept(visitor);
                if (!orderedTableNames.contains(tableName)) {
                    Value value = property.getValue();
                    // to-many, mapping(middle) table should before main table
                    if (value instanceof org.hibernate.mapping.Collection) {
                        orderedTableNames.add(0, tableName);
                    }
                    if (value instanceof ToOne) { // to-one
                        PersistentClass referencedEntityClass = metadata.getEntityBinding(((ToOne) value).getReferencedEntityName());
                        String entityName = StringUtils.substringAfterLast(referencedEntityClass.getEntityName(), "."); //$NON-NLS-1$
                        // only deal with nested types, not including entities
                        if (userMetadataRepository.getComplexType(entityName) == null) {
                            List<String> referencedEntityTables = getReferencedTableNames(referencedEntityClass);
                            for (String table : referencedEntityTables) {
                                if (!orderedTableNames.contains(table)) {
                                    orderedTableNames.add(table);
                                }
                            }
                            referencedEntityTables = getTableNames(referencedEntityClass);
                            for (String table : referencedEntityTables) {
                                if (!orderedTableNames.contains(table)) {
                                    orderedTableNames.add(table);
                                }
                            }
                        }
                    }
                }
            }
            return orderedTableNames;
        }

        @SuppressWarnings("rawtypes")
        private List<String> getReferencedTableNames(PersistentClass referencedPersistentClass) {
            List<String> orderedTableNames = new LinkedList<>();
            Iterator<PersistentClass> classMappingIterator = metadata.getEntityBindings().iterator();
            while (classMappingIterator.hasNext()) {
                PersistentClass persistentClass = classMappingIterator.next();
                // Add field's table
                Iterator propertyClosureIterator = persistentClass.getPropertyClosureIterator();
                ValueVisitor visitor = new ValueVisitor();
                while (propertyClosureIterator.hasNext()) {
                    Property property = (Property) propertyClosureIterator.next();
                    String tableName = (String) property.getValue().accept(visitor);
                    if (!orderedTableNames.contains(tableName)) {
                        Value value = property.getValue();
                        if (value instanceof org.hibernate.mapping.Collection) {
                            orderedTableNames.add(0, tableName);
                            String middleTableName = value.getTable().getName();
                            if (StringUtils.isNoneBlank(middleTableName) && !orderedTableNames.contains(middleTableName)) {
                                orderedTableNames.add(middleTableName);
                            }
                        }
                        if (value instanceof ToOne) {
                            PersistentClass referencedEntityClass = metadata.getEntityBinding(((ToOne) value).getReferencedEntityName());
                            if (referencedPersistentClass.getEntityName().equals(referencedEntityClass.getEntityName())) {
                                if (!orderedTableNames.contains(persistentClass.getTable().getName())) {
                                    orderedTableNames.add(persistentClass.getTable().getName());
                                }
                            }
                        }
                    }
                }
            }
            return orderedTableNames;
        }

        @Override
        public Object accept(RootClass class1) {
            return getTableNames(class1);
        }

        @Override
        public Object accept(UnionSubclass subclass) {
            return getTableNames(subclass);
        }

        @Override
        public Object accept(SingleTableSubclass subclass) {
            return getTableNames(subclass);
        }

        @Override
        public Object accept(JoinedSubclass subclass) {
            return getTableNames(subclass);
        }

        @Override
        public Object accept(Subclass subclass) {
            return getTableNames(subclass);
        }

        private class ValueVisitor implements org.hibernate.mapping.ValueVisitor {

            @Override
            public Object accept(Bag bag) {
                return bag.getCollectionTable().getName();
            }

            @Override
            public Object accept(IdentifierBag bag) {
                return bag.getCollectionTable().getName();
            }

            @Override
            public Object accept(org.hibernate.mapping.List list) {
                return list.getCollectionTable().getName();
            }

            @Override
            public Object accept(PrimitiveArray primitiveArray) {
                return primitiveArray.getCollectionTable().getName();
            }

            @Override
            public Object accept(Array list) {
                return list.getCollectionTable().getName();
            }

            @Override
            public Object accept(org.hibernate.mapping.Map map) {
                return map.getCollectionTable().getName();
            }

            @Override
            public Object accept(OneToMany many) {
                return many.getTable().getName();
            }

            @Override
            public Object accept(org.hibernate.mapping.Set set) {
                return set.getCollectionTable().getName();
            }

            @Override
            public Object accept(Any any) {
                return any.getTable().getName();
            }

            @Override
            public Object accept(SimpleValue value) {
                return value.getTable().getName();
            }

            @Override
            public Object accept(DependantValue value) {
                return value.getTable().getName();
            }

            @Override
            public Object accept(Component component) {
                return component.getTable().getName();
            }

            @Override
            public Object accept(ManyToOne mto) {
                return metadata.getEntityBinding(mto.getReferencedEntityName()).getTable().getName();
            }

            @Override
            public Object accept(OneToOne oto) {
                return metadata.getEntityBinding(oto.getReferencedEntityName()).getTable().getName();
            }
        }
    }
}