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

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import junit.framework.TestCase;

@SuppressWarnings({ "nls" })
public class EncryptUtilTest extends TestCase {

    @Test
    public void testUpdateConfiguration() {
        String path = getClass().getResource("mdm.conf").getFile();
        path = StringUtils.substringBefore(path, "mdm.conf");
        Configurations configs = new Configurations();
        try {
            // Read data from this file
            File propertiesFile = new File(path + "config.properties");
            FileBasedConfigurationBuilder<PropertiesConfiguration> propertiesBuilder = configs.propertiesBuilder(propertiesFile);
            propertiesBuilder.setAutoSave(true);
            PropertiesConfiguration propertiesConfig = propertiesBuilder.getConfiguration();
            propertiesConfig.setProperty("database.password", "#####");
            assertEquals("#####", propertiesConfig.getProperty("database.password"));

            configs = new Configurations();
            // obtain the configuration
            FileBasedConfigurationBuilder<XMLConfiguration> builder = configs.xmlBuilder(path + "paths.xml");
            builder.setAutoSave(true);
            XMLConfiguration config = builder.getConfiguration();

            // update property
            config.setProperty("processing.paths.port", "2222");
            // save configuration
            builder.save();
            assertEquals("2222", config.getProperty("processing.paths.port"));

            HierarchicalConfiguration<ImmutableNode> sub = config.configurationAt("processing(1)", true); //$NON-NLS-1$
            sub.setProperty("paths.port", "3333");
            builder.save();
            assertEquals("3333", sub.getProperty("paths.port"));
        } catch (Exception cex) {
            // Something went wrong
        }
    }

    @Test
    public void testEncypt() throws Exception {
        String path = getClass().getResource("mdm.conf").getFile();
        System.setProperty("encryption.keys.file", path);
        AESEncryption aesEncryption = new AESEncryption();

        path = StringUtils.substringBefore(path, "mdm.conf");
        EncryptUtil.encrypt(path);

        File confFile = new File(path + "mdm.conf");
        Configurations configs = new Configurations();
        PropertiesConfiguration confConfig = configs.properties(confFile);

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
        Configurations configurations = new Configurations();
        XMLConfiguration config = configurations.xml(datasource);

        HierarchicalConfiguration<ImmutableNode> sub = config.configurationAt("datasource(0)", true);
        String password = sub.getString("master.rdbms-configuration.connection-password");
        assertEquals("sa", password);
        password = sub.getString("master.rdbms-configuration.init.connection-password");
        assertNull(password);

        sub = config.configurationAt("datasource(1)", true);
        password = sub.getString("master.rdbms-configuration.connection-password");
        assertEquals("talend123", aesEncryption.decrypt("connection-password", password));
        password = sub.getString("master.rdbms-configuration.init.connection-password");
        assertEquals("talend123", aesEncryption.decrypt("connection-password", password));
    }
}
