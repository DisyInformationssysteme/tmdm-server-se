/*
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
 * 
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 * 
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */
package com.amalto.commons.core.utils;

import java.util.regex.Pattern;

public class ValidateUtil {

    public static String matchCommonRegex(String newText) {
        String headRegex = "\\w(-|\\w)*"; //$NON-NLS-1$
        String tailRegex = ".*\\w";//$NON-NLS-1$

        if (matches(headRegex, tailRegex, newText)) {
            return newText;
        } else {
            throw new IllegalArgumentException("The database/table/constraint name contains invalid character!");
        }      
    }

    private static boolean matches(String regex, String tailRegex, String newText) {
        boolean match = true;
        if (tailRegex != null) {
            Pattern tailPattern = Pattern.compile(tailRegex);
            match = tailPattern.matcher(newText).matches();
        }

        if (match) {
            Pattern pattern = Pattern.compile(regex);
            return pattern.matcher(newText).matches();
        }

        return false;
    }
}
