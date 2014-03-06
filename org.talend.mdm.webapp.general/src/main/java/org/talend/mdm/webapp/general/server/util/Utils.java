// ============================================================================
//
// Copyright (C) 2006-2013 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.general.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.talend.mdm.webapp.general.model.GroupItem;
import org.talend.mdm.webapp.general.model.LanguageBean;
import org.talend.mdm.webapp.general.model.MenuBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.amalto.commons.core.utils.XMLUtils;
import com.amalto.core.util.Messages;
import com.amalto.core.util.MessagesFactory;
import com.amalto.webapp.core.util.Menu;
import com.amalto.webapp.core.util.SystemLocale;
import com.amalto.webapp.core.util.SystemLocaleFactory;
import com.amalto.webapp.core.util.Util;

public class Utils {

    private static final Logger LOG = Logger.getLogger(Utils.class);

    private static final String GXT_PROPERTIES = "gxt.properties"; //$NON-NLS-1$

    private static final String EXCLUDING_PROPERTIES = "excluding.properties"; //$NON-NLS-1$

    private static final String GXT_CSS_RESOURCES = "gxt_css_resource.xml"; //$NON-NLS-1$

    private static final String WELCOMECONTEXT = "welcomeportal", WELCOMEAPP = "WelcomePortal";//$NON-NLS-1$ //$NON-NLS-2$

    private static final String DEFAULT_LANG = "en"; //$NON-NLS-1$

    /** a reference to the factory used to create Gxt instances */
    private static GxtFactory gxtFactory = new GxtFactory(GXT_PROPERTIES, EXCLUDING_PROPERTIES, GXT_CSS_RESOURCES);

    private static final Messages MESSAGES = MessagesFactory.getMessages(
            "org.talend.mdm.webapp.general.client.i18n.GeneralMessages", Utils.class.getClassLoader()); //$NON-NLS-1$

    public static void getJavascriptImportDetail(List<String> imports) {
        try {
            getJavascriptImportDetail(Menu.getRootMenu(), imports, 1, 1);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static int getSubMenus(Menu menu, String language, List<MenuBean> rows, int level, int i) {
        for (String key : menu.getSubMenus().keySet()) {
            Menu subMenu = menu.getSubMenus().get(key);
            String context = subMenu.getContext();
            String application = subMenu.getApplication();
            if ("updatereport".equals(context) && "UpdateReport".equals(application)) { //$NON-NLS-1$//$NON-NLS-2$
                context = "journal"; //$NON-NLS-1$
                application = "Journal"; //$NON-NLS-1$
            }
            if (gxtFactory.isExcluded(context, application)) {
                continue;
            }
            MenuBean item = new MenuBean();
            item.setId(i);
            item.setLevel(level);
            item.setContext(context);
            item.setIcon(subMenu.getIcon());
            String name = subMenu.getLabels().get(language);
            if (name == null) {
                name = subMenu.getLabels().get(DEFAULT_LANG); // fallback to default
            }
            item.setName(name);
            item.setApplication(application == null ? "" : application); //$NON-NLS-1$
            disabledMenuItemIf(subMenu, item, language);
            rows.add(item);
            i++;
            if (subMenu.getSubMenus().size() > 0) {
                i = getSubMenus(subMenu, language, rows, level + 1, i);
            }
        }
        return i;
    }

    private static void disabledMenuItemIf(Menu menu, MenuBean menuBean, String language) {
        if ("stagingarea".equals(menu.getContext()) && "Stagingarea".equals(menu.getApplication())) { //$NON-NLS-1$ //$NON-NLS-2$
            menuBean.setDisabledDesc(MESSAGES.getMessage(new Locale(language), "stagingarea_unavailable")); //$NON-NLS-1$
            boolean disabled = true;
            try {
                disabled = !Util.getPort().supportStaging().is_true();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            menuBean.setDisabled(disabled);
        }
    }

    public static ArrayList<String> getCssImport() throws Exception {
        ArrayList<String> imports = new ArrayList<String>();
        getCssImportDetail(Menu.getRootMenu(), imports, 1, 1);
        return imports;
    }

    public static ArrayList<String> getJavascriptImport() throws Exception {
        ArrayList<String> imports = new ArrayList<String>();
        getJavascriptImportDetail(Menu.getRootMenu(), imports, 1, 1);
        // FIXME: This is a workaround for 4.2 only
        // complementItemsbrowser(imports);
        completeThirdPartJS(imports);
        return imports;
    }

    private static int getCssImportDetail(Menu menu, List<String> imports, int level, int i) throws Exception {

        for (String key : menu.getSubMenus().keySet()) {
            Menu subMenu = menu.getSubMenus().get(key);

            if (subMenu.getContext() != null) {
                if (gxtFactory.isExcluded(subMenu.getContext(), subMenu.getApplication())) {
                    continue;
                }
                GxtProjectModel gxtPro = gxtFactory.getGxtCss(subMenu.getContext(), subMenu.getApplication());
                if (gxtPro != null) {
                    List<String> csses = gxtPro.getCss_addresses();
                    if (csses != null) {
                        for (String css : csses) {
                            imports.add("<link rel='stylesheet' type='text/css' href='" + css + "'/>\n"); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    }
                }
                i++;
            }
            if (subMenu.getSubMenus().size() > 0) {
                i = getJavascriptImportDetail(subMenu, imports, level + 1, i);
            }
        }
        return i;
    }

    private static int getJavascriptImportDetail(Menu menu, List<String> imports, int level, int i) throws Exception {
        if (menu.getParent() == null) {
            // add welcome by default
            Menu welMenu = new Menu();
            welMenu.setApplication(WELCOMEAPP);
            welMenu.setContext(WELCOMECONTEXT);
            menu.getSubMenus().put(WELCOMECONTEXT, welMenu);
        }

        for (String key : menu.getSubMenus().keySet()) {
            Menu subMenu = menu.getSubMenus().get(key);

            if (subMenu.getContext() != null) {
                String context = subMenu.getContext();
                String application = subMenu.getApplication();
                if ("updatereport".equals(context) && "UpdateReport".equals(application)) { //$NON-NLS-1$//$NON-NLS-2$
                    context = "journal"; //$NON-NLS-1$
                    application = "Journal"; //$NON-NLS-1$
                }
                if (gxtFactory.isExcluded(context, application)) {
                    continue;
                }
                String gxtEntryModule = gxtFactory.getGxtEntryModule(context, application);

                if (gxtEntryModule == null || context.equals("itemsbrowser2")) { //$NON-NLS-1$
                    String tmp = "<script type=\"text/javascript\" src=\"/" + context + "/secure/dwr/interface/" //$NON-NLS-1$ //$NON-NLS-2$
                            + application + "Interface.js\"></script>\n"; //$NON-NLS-1$
                    if (!imports.contains(tmp)) {
                        imports.add(tmp);
                    }
                    tmp = "<script type=\"text/javascript\" src=\"/" + context + "/secure/js/" //$NON-NLS-1$ //$NON-NLS-2$
                            + application + ".js\"></script>\n"; //$NON-NLS-1$
                    if (!imports.contains(tmp)) {
                        imports.add(tmp);
                    }
                    if (context.equals("itemsbrowser2")) { //$NON-NLS-1$
                        tmp = "<script type=\"text/javascript\" src=\"/" + context + "/" + gxtEntryModule + "/" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                + gxtEntryModule + ".nocache.js\"></script>\n"; //$NON-NLS-1$
                        imports.add(tmp);
                    }
                } else {
                    String tmp = "<script type=\"text/javascript\" src=\"/" + context + "/" + gxtEntryModule + "/" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            + gxtEntryModule + ".nocache.js\"></script>\n"; //$NON-NLS-1$
                    if ("browserecords".equals(context) && "browserecords".equals(gxtEntryModule)) { //$NON-NLS-1$ //$NON-NLS-2$
                        imports.add("<script type=\"text/javascript\" src=\"/" + context + "/secure/dwr/interface/ItemsBrowserInterface.js\"></script>"); //$NON-NLS-1$//$NON-NLS-2$
                        imports.add("<script type=\"text/javascript\" src=\"/" + context + "/secure/js/ImprovedDWRProxy.js\"></script>"); //$NON-NLS-1$//$NON-NLS-2$
                        imports.add("<script type=\"text/javascript\" src=\"/" + context + "/secure/js/SearchEntityPanel.js\"></script>"); //$NON-NLS-1$//$NON-NLS-2$
                    }
                    if (!imports.contains(tmp)) {
                        imports.add(tmp);
                    }
                }
                if (context.equals("stagingarea")) { //$NON-NLS-1$
                    String tmp = "<script type=\"text/javascript\" src=\"/stagingarea/stagingareabrowse/stagingareabrowse.nocache.js\"></script>"; //$NON-NLS-1$
                    imports.add(tmp);
                }
                i++;
            }
            if (subMenu.getSubMenus().size() > 0) {
                i = getJavascriptImportDetail(subMenu, imports, level + 1, i);
            }
        }
        return i;
    }

    private static void completeThirdPartJS(ArrayList<String> imports) {
        imports.add("<script type=\"text/javascript\" src=\"/talendmdm/secure/dwr/interface/WidgetInterface.js\"></script>\n");//$NON-NLS-1$
        imports.add("<script language=\"javascript\" src=\"/core/secure/gxt/resources/flash/swfobject.js\"></script>"); //$NON-NLS-1$
    }

    public static String getCommonImport() {
        return
        // EXT & YUI
        "<script type=\"text/javascript\" src=\"/core/secure/yui-2.4.0/build/utilities/utilities.js\"></script>\n" //$NON-NLS-1$
                + "<script type=\"text/javascript\" src=\"/core/secure/yui-2.4.0/build/yuiloader/yuiloader-beta.js\"></script>\n" //$NON-NLS-1$
                + // "<script src=\"/core/secure/ext-2.2/adapter/yui/yui-utilities.js\" type=\"text/javascript\"></script>\n"+
                "<script type=\"text/javascript\" src=\"/core/secure/ext-2.2/adapter/yui/ext-yui-adapter.js\"></script>\n" //$NON-NLS-1$
                + "<script type=\"text/javascript\" src=\"/core/secure/ext-2.2/ext-all-debug.js\"></script>\n" //$NON-NLS-1$
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"/core/secure/ext-2.2/resources/css/ext-all_compatible.css\" />\n" //$NON-NLS-1$
                + // EXT-UX
                "<script type=\"text/javascript\" src=\"/core/secure/ext.ux/editablecolumntree/ColumnNodeUI.js\"></script>\n" //$NON-NLS-1$
                + "<script type=\"text/javascript\" src=\"/core/secure/ext.ux/editablecolumntree/treeSerializer.js\"></script>\n" //$NON-NLS-1$
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"/core/secure/ext.ux/editablecolumntree/editable-column-tree.css\" />\n" //$NON-NLS-1$
                + "<script type=\"text/javascript\" src=\"/core/secure/ext.ux/MultiSelectTreePanel.js\"></script>\n" //$NON-NLS-1$
                + "<script type=\"text/javascript\" src=\"/core/secure/ext.ux/i18n/PropertyReader.js\"></script>\n" //$NON-NLS-1$
                + "<script type=\"text/javascript\" src=\"/core/secure/ext.ux/i18n/Bundle.js\"></script>\n" //$NON-NLS-1$
                + // Firefox3 Fixes
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"/core/secure/css/firefox3-fix.css\" />\n" //$NON-NLS-1$
                + // CORE
                "<script type=\"text/javascript\" src=\"/general/proxy_core.js\"></script>\n" //$NON-NLS-1$
                + "<script type=\"text/javascript\" src=\"/core/secure/dwr/interface/LayoutInterface.js\"></script>\n" //$NON-NLS-1$
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"/core/secure/css/webapp-core.css\" />\n" //$NON-NLS-1$
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"/core/secure/css/amalto-menus.css\" />\n" //$NON-NLS-1$
                + // Proxy DWR <-> Ext
                "<script type=\"text/javascript\" src=\"/core/secure/ext.ux/DWRAction.js\"></script>\n" //$NON-NLS-1$
                + "<script type=\"text/javascript\" src=\"/core/secure/ext.ux/DWRProxy.js\"></script>\n" //$NON-NLS-1$
                + // "<script type=\"text/javascript\" src=\"/core/secure/ext.ux/DWRProxy.js\"></script>\n"+
                  // utility class
                "<script type=\"text/javascript\" src=\"/core/secure/js/bgutil.js\"></script>\n" //$NON-NLS-1$
                + // graph class
                "<script type=\"text/javascript\" src=\"/core/secure/js/raphael-min.js\"></script>\n" //$NON-NLS-1$
                + // DWR
                "<script language=\"javascript1.2\" type='text/javascript' src='/core/secure/dwr/engine.js'></script>\n" //$NON-NLS-1$
                + "<script language=\"javascript1.2\" type='text/javascript' src='/core/secure/dwr/util.js'></script>\n" //$NON-NLS-1$
                + // Simile Wiget
                "<script src=\"/core/secure/timeline/timeline_js/timeline-api.js\" type=\"text/javascript\"></script>\n" //$NON-NLS-1$
                + "<script src=\"/core/secure/timeline/timeline_ajax/simile-ajax-api.js\" type=\"text/javascript\"></script>\n" //$NON-NLS-1$
                + "<link rel=\"stylesheet\" href=\"/core/secure/timeline/css/default.css\" type=\"text/css\">"; //$NON-NLS-1$
    }

    public static List<LanguageBean> getLanguages(String selectedLang) throws Exception {

        List<LanguageBean> languages = new ArrayList<LanguageBean>();
        Map<String, SystemLocale> supportedLocales = SystemLocaleFactory.getInstance().getSupportedLocales();
        for (String iso : supportedLocales.keySet()) {
            SystemLocale systemLocale = supportedLocales.get(iso);
            LanguageBean lang = new LanguageBean();
            lang.setText(systemLocale.getLabel());
            lang.setValue(systemLocale.getIso());
            lang.setDateTimeFormat(systemLocale.getDateTimeFormat());
            if (lang.getValue().equals(selectedLang)) {
                lang.setSelected(true);
            }
            languages.add(lang);
        }

        if (selectedLang == null) {
            languages.get(0).setSelected(true);
        }

        return languages;
    }

    public static List<GroupItem> getGroupItems(String language) throws IOException, SAXException, ParserConfigurationException {
        InputStream is = Utils.class.getResourceAsStream("/MenuGroup.xml"); //$NON-NLS-1$
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);
        Element root = doc.getDocumentElement();
        String defaultLang = root.getAttribute("defaultLang"); //$NON-NLS-1$
        NodeList nodes = root.getChildNodes();
        List<GroupItem> giList = new ArrayList<GroupItem>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (node.getNodeName().equals("groupitem")) { //$NON-NLS-1$ 
                    GroupItem giNew = new GroupItem();
                    Node langNode = node.getAttributes().getNamedItem(language);
                    if (langNode == null) {
                        langNode = node.getAttributes().getNamedItem(defaultLang);
                    }
                    giNew.setGroupHeader(langNode.getNodeValue());
                    NodeList items = node.getChildNodes();
                    List<String> menuItems = new ArrayList<String>();
                    for (int k = 0; k < items.getLength(); k++) {
                        Node item = items.item(k);
                        if (item.getNodeName().equals("item")) { //$NON-NLS-1$
                            menuItems.add(item.getTextContent());
                        }
                    }
                    giNew.setMenuItems(menuItems);
                    giList.add(giNew);
                }
            }
        }

        return giList;
    }

    public static String setLanguage(String xml, String language) throws Exception {
        Document doc = XMLUtils.parse(xml);
        if (doc.getElementsByTagName("language") != null) { //$NON-NLS-1$
            doc.getElementsByTagName("language").item(0).setTextContent(language); //$NON-NLS-1$
            return XMLUtils.nodeToString(doc);
        }
        return xml;
    }
}
