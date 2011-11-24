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
package org.talend.mdm.webapp.browserecords.client.util;

/**
 * DOC HSHU class global comment. Detailled comment
 */
public class LabelUtil {

    private static final String RESERVED_WORD_START_FLAG = "{";//$NON-NLS-1$

    private static final String RESERVED_WORD_END_FLAG = "}";//$NON-NLS-1$

    /**
     * DOC HSHU Comment method "isDynamicLabel".
     */
    public static boolean isDynamicLabel(String label) {
        if (label == null)
            return false;
        if (label.indexOf(RESERVED_WORD_START_FLAG) != -1)
            return true;
        return false;
    }

    public static String getNormalLabel(String label) {
        if (label.indexOf(RESERVED_WORD_START_FLAG) != -1) {
            return label.substring(0, label.indexOf(RESERVED_WORD_START_FLAG));
        } else {
            return label;
        }
    }
    
    public static String convertSpecialHTMLCharacter(String label){
        label = label.replaceAll("'", "&acute;");  //$NON-NLS-1$//$NON-NLS-2$
        label = label.replaceAll("\"", "&quot;");  //$NON-NLS-1$//$NON-NLS-2$
        return label;
    }
}
