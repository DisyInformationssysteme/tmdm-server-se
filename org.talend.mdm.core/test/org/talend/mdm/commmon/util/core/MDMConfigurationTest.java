/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */
package org.talend.mdm.commmon.util.core;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import junit.framework.TestCase;


public class MDMConfigurationTest extends TestCase {

    @SuppressWarnings({ "nls" })
    public void testGetProperties() throws Exception {
        Object[] args = { true, true };

        String filePath = getClass().getResource("mdm1.conf").getFile();
        System.setProperty("encryption.keys.file", filePath);
        MDMConfiguration configuration = MDMConfiguration.createConfiguration(filePath, true);
        Method method = configuration.getClass().getDeclaredMethod("getProperties", new Class[] { boolean.class, boolean.class });
        method.setAccessible(true);
        Properties properties = (Properties) method.invoke(configuration, args);     
        assertEquals("talend", properties.getProperty("admin.password"));
        assertEquals("install", properties.getProperty("technical.password"));

        Properties propertiesInFile = new Properties();
        FileInputStream in = new FileInputStream(filePath);
        propertiesInFile.load(in);
        assertEquals("4S5Jn1hvYKwNYEdokE2fbSqOg5cpodDda0vAEamlJf2Dnw==", propertiesInFile.getProperty("admin.password"));
        assertEquals("dQmd66T0OOVFGqeK4ji7CZBh4hYwBWOTY9aGA+4bd7u7/BE=", propertiesInFile.getProperty("technical.password"));

        Field field = configuration.getClass().getDeclaredField("instance");
        field.setAccessible(true);
        field.set(configuration, null);
        configuration = MDMConfiguration.createConfiguration(getClass().getResource("mdm2.conf").getFile(), true);
        method = configuration.getClass().getDeclaredMethod("getProperties", new Class[] { boolean.class, boolean.class });
        method.setAccessible(true);
        properties = (Properties) method.invoke(configuration, args);
        assertEquals(StringUtils.EMPTY, properties.getProperty("admin.password"));
        assertEquals(StringUtils.EMPTY, properties.getProperty("technical.password"));
    }
}
