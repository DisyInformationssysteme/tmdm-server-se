// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ForeignKeyBean extends ItemBaseModel {

    private static final long serialVersionUID = 1L;

    private String foreignKeyPath;

    private boolean showInfo = false;

    public boolean isShowInfo() {
        return showInfo;
    }

    public void setShowInfo(boolean showInfo) {
        this.showInfo = showInfo;
    }

    private Map<String, String> foreignKeyInfo = new HashMap<String, String>();

    public Map<String, String> getForeignKeyInfo() {
        return foreignKeyInfo;
    }

    public void setForeignKeyInfo(Map<String, String> foreignKeyInfo) {
        this.foreignKeyInfo = foreignKeyInfo;
    }

    public String getId() {
        return get("id"); //$NON-NLS-1$
    }

    public void setId(String id) {
        set("id", id); //$NON-NLS-1$
    }

    public String getDisplayInfo() {
        return get("displayInfo"); //$NON-NLS-1$
    }

    public void setDisplayInfo(String displayInfo) {
        set("displayInfo", displayInfo); //$NON-NLS-1$
    }

    public String getForeignKeyPath() {
        return foreignKeyPath;
    }

    public void setForeignKeyPath(String foreignKeyPath) {
        this.foreignKeyPath = foreignKeyPath;
    }

    public String getFullString() {
        return foreignKeyPath + "-" + getId(); //$NON-NLS-1$
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (showInfo && foreignKeyInfo.size() != 0) {
            Iterator<String> iterator = foreignKeyInfo.values().iterator();

            while (iterator.hasNext()) {
                sb.append(iterator.next());
                sb.append("-"); //$NON-NLS-1$
            }

            return sb.toString();
        } else {
            if (this.getProperties().keySet().size() > 1) {
                for (String key : this.getProperties().keySet()) {
                    if (!key.equals("i")) { //$NON-NLS-1$
                        sb.append(this.getProperties().get(key));
                        sb.append("-"); //$NON-NLS-1$
                    }
                }
                return sb.toString().substring(0, sb.toString().length() - 1);
            } else {
                if (getDisplayInfo() == null && foreignKeyPath == null)
                    return getId();
                else
                    return getDisplayInfo();
            }
        }
    }
}
