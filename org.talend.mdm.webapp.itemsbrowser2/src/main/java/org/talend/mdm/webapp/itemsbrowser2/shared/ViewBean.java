// ============================================================================
//
// Copyright (C) 2006-2010 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.itemsbrowser2.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * DOC HSHU  class global comment. Detailled comment
 */
public class ViewBean implements Serializable{
    
    private List<String> viewableXpaths;
    
    public List<String> getViewableXpaths() {
        return viewableXpaths;
    }

    public void addViewableXpath(String xpath) {
        if(this.viewableXpaths==null)viewableXpaths=new ArrayList<String>();
        viewableXpaths.add(xpath);
    }

}
