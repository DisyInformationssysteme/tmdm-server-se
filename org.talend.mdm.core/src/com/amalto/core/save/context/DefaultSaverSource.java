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

package com.amalto.core.save.context;

import com.amalto.core.ejb.local.XmlServerSLWrapperLocal;
import com.amalto.core.history.MutableDocument;
import com.amalto.core.metadata.MetadataRepository;
import com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK;
import com.amalto.core.objects.datamodel.ejb.local.DataModelCtrlLocal;
import com.amalto.core.save.DocumentSaverContext;
import com.amalto.core.util.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultSaverSource implements SaverSource {

    private final XmlServerSLWrapperLocal database;

    private final DataModelCtrlLocal dataModel;

    private final Map<String, MetadataRepository> repositories = new HashMap<String, MetadataRepository>();

    private final Map<String, String> schemasAsString = new HashMap<String, String>();

    private final String userName;

    public DefaultSaverSource() {
        this(null);
    }

    public DefaultSaverSource(String userName) {
        try {
            database = Util.getXmlServerCtrlLocal();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }

        try {
            dataModel = Util.getDataModelCtrlLocal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.userName = userName;
    }

    public InputStream get(String dataClusterName, String typeName, String revisionId, String[] key) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append(dataClusterName).append('.').append(typeName).append('.');
            for (int i = 0; i < key.length; i++) {
                builder.append(key[i]);
                if (i < key.length - 1) {
                    builder.append('.');
                }
            }
            String uniqueId = builder.toString();

            String documentAsString = database.getDocumentAsString(revisionId, dataClusterName, uniqueId); //$NON-NLS-1$
            if (documentAsString != null) {
                return new ByteArrayInputStream(documentAsString.getBytes("UTF-8")); //$NON-NLS-1$
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exist(String dataCluster, String typeName, String revisionId, String[] key) {
        return get(dataCluster, typeName, revisionId, key) != null;
    }

    public MetadataRepository getMetadataRepository(String dataModelName) {
        try {
            synchronized (repositories) {
                if (repositories.get(dataModelName) == null) {
                    MetadataRepository repository = new MetadataRepository();
                    InputStream schema = getSchema(dataModelName);
                    repository.load(schema);
                    repositories.put(dataModelName, repository);
                }
                return repositories.get(dataModelName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getSchema(String dataModelName) {
        try {
            synchronized (schemasAsString) {
                if (schemasAsString.get(dataModelName) == null) {
                    String schemaAsString = dataModel.getDataModel(new DataModelPOJOPK(dataModelName)).getSchema();
                    schemasAsString.put(dataModelName, schemaAsString);
                }
            }
            return new ByteArrayInputStream(schemasAsString.get(dataModelName).getBytes("UTF-8")); //$NON-NLS-1$
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUniverse() {
        try {
            return LocalUser.getLocalUser().getUniverse().getName();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputReport invokeBeforeSaving(DocumentSaverContext context, MutableDocument updateReportDocument) {
        try {
            return Util.beforeSaving(context.getType().getName(),
                    context.getDatabaseDocument().exportToString(),
                    updateReportDocument.exportToString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getCurrentUserRoles() {
        try {
            return LocalUser.getLocalUser().getRoles();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserName() {
        // Allow saver caller to override user name.
        if (userName != null) {
            return userName;
        }
        try {
            return LocalUser.getLocalUser().getUsername();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existCluster(String revisionID, String dataClusterName) {
        try {
            return database.existCluster(revisionID, dataClusterName);
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public String getConceptRevisionID(String typeName) {
        try {
            return LocalUser.getLocalUser().getUniverse().getConceptRevisionID(typeName);
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetLocalUsers() {
        try {
            LocalUser.resetLocalUsers();
        } catch (XtentisException e) {
            throw new RuntimeException(e);
        }
    }

    public void initAutoIncrement() {
        AutoIncrementGenerator.init();
    }

}
