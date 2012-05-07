// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.base.client.model;

import java.util.Date;

public enum DataTypeConstants implements DataType {

    // TODO support more types
    BOOLEAN("boolean", false), //$NON-NLS-1$
    DATE("date", new Date()), //$NON-NLS-1$
    DATETIME("dateTime", new Date()), //$NON-NLS-1$
    DECIMAL("decimal", 0.0), //$NON-NLS-1$
    DOUBLE("double", 0.0), //$NON-NLS-1$
    FLOAT("float", 0.0), //$NON-NLS-1$
    INT("int", 0), //$NON-NLS-1$
    INTEGER("integer", 0), //$NON-NLS-1$
    LONG("long", 0), //$NON-NLS-1$
    SHORT("short", 0), //$NON-NLS-1$
    STRING("string", ""), //$NON-NLS-1$ //$NON-NLS-2$

    UUID("UUID", ""), //$NON-NLS-1$ //$NON-NLS-2$
    AUTO_INCREMENT("AUTO_INCREMENT", ""), //$NON-NLS-1$ //$NON-NLS-2$
    PICTURE("PICTURE", ""), //$NON-NLS-1$ //$NON-NLS-2$ // Change picture default value to empty
    URL("URL", "@@http://"), //$NON-NLS-1$ //$NON-NLS-2$

    UNKNOW("unknow", "");//$NON-NLS-1$ //$NON-NLS-2$

    String typeName;

    Object defaultValue;

    DataTypeConstants(String typeName, Object defaultValue) {
        this.typeName = typeName;
        this.defaultValue = defaultValue;
    }

    public String getTypeName() {
        return typeName;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    /*
     * (non-Jsdoc)
     * 
     * @see org.talend.mdm.webapp.itemsbrowser2.client.model.DataType#getBaseTypeName()
     */
    public String getBaseTypeName() {
        return getTypeName();
    }

}
