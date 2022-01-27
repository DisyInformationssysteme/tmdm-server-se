/*
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */
package com.amalto.core.storage.hibernate.mapping;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Index;

import com.amalto.core.storage.hibernate.H2CustomDialect;

public class MDMIndex extends Index {

    private static final long serialVersionUID = 1L;

    /**
     * Override creating index statement. see {@link MDMTable#getOrCreateIndex()}
     */
    @Override
    public String sqlCreateString(
            Dialect dialect, 
            Mapping mapping, 
            String defaultCatalog, 
            String defaultSchema)
            throws HibernateException {
        String sqlIndexString = super.sqlCreateString(dialect, mapping, defaultCatalog, defaultSchema);
        if (!H2CustomDialect.class.getName().equals(dialect.getClass().getName())) {
            return sqlIndexString;
        }
        return sqlIndexString.replace("create index", "CREATE INDEX IF NOT EXISTS ");
    }
}
