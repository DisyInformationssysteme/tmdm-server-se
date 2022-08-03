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

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.talend.mdm.commmon.util.core.EncryptUtil;
import org.talend.mdm.commmon.util.core.MDMConfiguration;
import org.talend.mdm.commmon.util.core.AESEncryption;

import junit.framework.TestCase;

@SuppressWarnings({ "nls" })
public class EncryptUtilTest extends TestCase {

    @Test
    public void testEncypt() throws Exception {
        String path = getClass().getResource("mdm.conf").getFile();
        System.setProperty("encryption.keys.file", path);
        AESEncryption aesEncryption = new AESEncryption();
        
        path = StringUtils.substringBefore(path, "mdm.conf");
        EncryptUtil.encrypt(path);

        File confFile = new File(path + "mdm.conf");
        PropertiesConfiguration confConfig = new PropertiesConfiguration();
        confConfig.setDelimiterParsingDisabled(true);
        confConfig.load(confFile);
     
        String adminPassword = confConfig.getString(MDMConfiguration.ADMIN_PASSWORD);
        assertNotEquals("talend", adminPassword);
        assertEquals("talend", aesEncryption.decrypt(MDMConfiguration.ADMIN_PASSWORD, adminPassword));
		
        String technicalPassword = confConfig.getString(MDMConfiguration.TECHNICAL_PASSWORD);
        assertNotEquals("install", technicalPassword);
        assertEquals("install", aesEncryption.decrypt(MDMConfiguration.TECHNICAL_PASSWORD, technicalPassword));
        
        String amqPassword = confConfig.getString(EncryptUtil.ACTIVEMQ_PASSWORD);
        assertNotEquals("test", amqPassword);
        assertEquals("test", aesEncryption.decrypt(EncryptUtil.ACTIVEMQ_PASSWORD, amqPassword));
        
        File datasource = new File(path + "datasources.xml");
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        config.load(datasource);

        HierarchicalConfiguration sub = config.configurationAt("datasource(0)");
        String password = sub.getString("master.rdbms-configuration.connection-password");
        assertEquals("sa", password);
        password = sub.getString("master.rdbms-configuration.init.connection-password");
        assertNull(password);

        sub = config.configurationAt("datasource(1)");
        password = sub.getString("master.rdbms-configuration.connection-password");
        assertNotEquals("talend123", password);
        assertEquals("talend123", aesEncryption.decrypt("connection-password", password));

        password = sub.getString("master.rdbms-configuration.init.connection-password");
        assertNotEquals("talend123", password);
        assertEquals("talend123", aesEncryption.decrypt("connection-password", password));

    }
}
