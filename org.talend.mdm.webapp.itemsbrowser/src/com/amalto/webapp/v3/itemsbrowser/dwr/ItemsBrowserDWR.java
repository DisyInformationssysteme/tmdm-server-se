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
package com.amalto.webapp.v3.itemsbrowser.dwr;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.jacc.PolicyContextException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.xerces.dom.ElementNSImpl;
import org.apache.xerces.dom.TextImpl;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.dom4j.Attribute;
import org.exolab.castor.types.Date;
import org.exolab.castor.types.Time;
import org.jboss.dom4j.DocumentException;
import org.jboss.dom4j.io.SAXReader;
import org.talend.mdm.commmon.util.datamodel.management.BusinessConcept;
import org.talend.mdm.commmon.util.datamodel.management.ReusableType;
import org.talend.mdm.commmon.util.webapp.XSystemObjects;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.amalto.core.ejb.ItemPOJOPK;
import com.amalto.core.objects.datacluster.ejb.DataClusterPOJOPK;
import com.amalto.core.util.CVCException;
import com.amalto.core.util.LocalUser;
import com.amalto.core.util.Messages;
import com.amalto.core.util.MessagesFactory;
import com.amalto.webapp.core.bean.ComboItemBean;
import com.amalto.webapp.core.bean.Configuration;
import com.amalto.webapp.core.bean.ListRange;
import com.amalto.webapp.core.bean.UpdateReportItem;
import com.amalto.webapp.core.dmagent.SchemaWebAgent;
import com.amalto.webapp.core.dwr.CommonDWR;
import com.amalto.webapp.core.json.JSONArray;
import com.amalto.webapp.core.json.JSONObject;
import com.amalto.webapp.core.util.RoutingException;
import com.amalto.webapp.core.util.Util;
import com.amalto.webapp.core.util.XmlUtil;
import com.amalto.webapp.core.util.XtentisWebappException;
import com.amalto.webapp.util.webservices.WSBoolean;
import com.amalto.webapp.util.webservices.WSByteArray;
import com.amalto.webapp.util.webservices.WSConceptKey;
import com.amalto.webapp.util.webservices.WSCount;
import com.amalto.webapp.util.webservices.WSDataClusterPK;
import com.amalto.webapp.util.webservices.WSDataModelPK;
import com.amalto.webapp.util.webservices.WSDeleteItem;
import com.amalto.webapp.util.webservices.WSDropItem;
import com.amalto.webapp.util.webservices.WSDroppedItemPK;
import com.amalto.webapp.util.webservices.WSExecuteTransformerV2;
import com.amalto.webapp.util.webservices.WSExistsDataCluster;
import com.amalto.webapp.util.webservices.WSExistsItem;
import com.amalto.webapp.util.webservices.WSGetBusinessConceptKey;
import com.amalto.webapp.util.webservices.WSGetBusinessConcepts;
import com.amalto.webapp.util.webservices.WSGetDataModel;
import com.amalto.webapp.util.webservices.WSGetItem;
import com.amalto.webapp.util.webservices.WSGetItemPKsByCriteria;
import com.amalto.webapp.util.webservices.WSGetItemPKsByFullCriteria;
import com.amalto.webapp.util.webservices.WSGetTransformer;
import com.amalto.webapp.util.webservices.WSGetTransformerPKs;
import com.amalto.webapp.util.webservices.WSGetView;
import com.amalto.webapp.util.webservices.WSGetViewPKs;
import com.amalto.webapp.util.webservices.WSItem;
import com.amalto.webapp.util.webservices.WSItemPK;
import com.amalto.webapp.util.webservices.WSItemPKsByCriteriaResponse;
import com.amalto.webapp.util.webservices.WSPutItem;
import com.amalto.webapp.util.webservices.WSPutItemWithReport;
import com.amalto.webapp.util.webservices.WSStringArray;
import com.amalto.webapp.util.webservices.WSStringPredicate;
import com.amalto.webapp.util.webservices.WSTransformer;
import com.amalto.webapp.util.webservices.WSTransformerContext;
import com.amalto.webapp.util.webservices.WSTransformerContextPipelinePipelineItem;
import com.amalto.webapp.util.webservices.WSTransformerPK;
import com.amalto.webapp.util.webservices.WSTransformerV2PK;
import com.amalto.webapp.util.webservices.WSTypedContent;
import com.amalto.webapp.util.webservices.WSView;
import com.amalto.webapp.util.webservices.WSViewPK;
import com.amalto.webapp.util.webservices.WSWhereAnd;
import com.amalto.webapp.util.webservices.WSWhereCondition;
import com.amalto.webapp.util.webservices.WSWhereItem;
import com.amalto.webapp.util.webservices.WSWhereOperator;
import com.amalto.webapp.util.webservices.WSWhereOr;
import com.amalto.webapp.util.webservices.WSXPathsSearch;
import com.amalto.webapp.v3.itemsbrowser.bean.BrowseItem;
import com.amalto.webapp.v3.itemsbrowser.bean.Criteria;
import com.amalto.webapp.v3.itemsbrowser.bean.DisplayRule;
import com.amalto.webapp.v3.itemsbrowser.bean.ForeignKeyDrawer;
import com.amalto.webapp.v3.itemsbrowser.bean.ItemResult;
import com.amalto.webapp.v3.itemsbrowser.bean.Restriction;
import com.amalto.webapp.v3.itemsbrowser.bean.SearchTempalteName;
import com.amalto.webapp.v3.itemsbrowser.bean.SubTypeBean;
import com.amalto.webapp.v3.itemsbrowser.bean.TreeNode;
import com.amalto.webapp.v3.itemsbrowser.bean.View;
import com.amalto.webapp.v3.itemsbrowser.bean.WhereCriteria;
import com.amalto.webapp.v3.itemsbrowser.util.BusinessConceptForBrowser;
import com.amalto.webapp.v3.itemsbrowser.util.DisplayRulesUtil;
import com.amalto.webapp.v3.itemsbrowser.util.DynamicLabelUtil;
import com.amalto.webapp.v3.itemsbrowser.util.PropsUtils;
import com.amalto.webapp.v3.itemsbrowser.util.XpathUtil;
import com.sun.org.apache.xpath.internal.XPathAPI;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.impl.AnnotationImpl;
import com.sun.xml.xsom.impl.FacetImpl;

/**
 * cluster
 * 
 * 
 * @author asaintguilhem
 * 
 */

public class ItemsBrowserDWR {

    private static final Logger LOG = Logger.getLogger(ItemsBrowserDWR.class);

    private static final String DOC_STATUS_NEW = "DOC_STATUS_NEW"; //$NON-NLS-1$

    private static final String DOC_STATUS_EDIT = "DOC_STATUS_EDIT"; //$NON-NLS-1$

    private static final String AUTO_INCREMENT = "(Auto)"; //$NON-NLS-1$

    private static final Messages MESSAGES = MessagesFactory.getMessages(
            "com.amalto.webapp.v3.itemsbrowser.dwr.messages", ItemsBrowserDWR.class.getClassLoader()); //$NON-NLS-1$

    private static Object locker = new Object();

    public ItemsBrowserDWR() {
        super();
    }

    /**
     * return a list of "browse items" views
     * 
     * @param language
     * @return a map name->description
     * @throws RemoteException
     * @throws Exception
     */
    public Map<String, String> getViewsList(String language) throws RemoteException, Exception {
        Configuration config = Configuration.getInstance(true);
        String model = config.getModel();
        String dataCluster = config.getCluster();

        if (model == null || model.length() == 0) {
            throw new Exception("The Data Model can't be empty!"); //$NON-NLS-1$
        } else {
            // fix bug0017075, syn the to property to PROVISIONING
            Configuration.initialize(dataCluster, model);
        }

        String[] businessConcept = Util.getPort().getBusinessConcepts(new WSGetBusinessConcepts(new WSDataModelPK(model)))
                .getStrings();
        ArrayList<String> bc = new ArrayList<String>();
        for (int i = 0; i < businessConcept.length; i++) {
            bc.add(businessConcept[i]);
        }
        WSViewPK[] wsViewsPK = Util.getPort().getViewPKs(new WSGetViewPKs("Browse_items.*")).getWsViewPK(); //$NON-NLS-1$

        TreeMap<String, String> views = new TreeMap<String, String>();
        Pattern p = Pattern.compile(".*\\[" + language.toUpperCase() + ":(.*?)\\].*", Pattern.DOTALL); //$NON-NLS-1$ //$NON-NLS-2$
        for (int i = 0; i < wsViewsPK.length; i++) {
            WSView wsview = Util.getPort().getView(new WSGetView(wsViewsPK[i]));
            String concept = wsview.getName().replaceAll("Browse_items_", "").replaceAll("#.*", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            if ( // wsviews[i].getWsDataClusterPK().getPk().equals(cluster)
            // && wsviews[i].getWsDataModelPK().getPk().equals(model) &&
            bc.contains(concept)) {
                String viewDesc = p.matcher(!wsview.getDescription().equals("") ? wsview.getDescription() : wsview.getName()) //$NON-NLS-1$
                        .replaceAll("$1"); //$NON-NLS-1$
                views.put(wsview.getName(), viewDesc.equals("") ? wsview.getName() : viewDesc); //$NON-NLS-1$
            }
        }
        return CommonDWR.getMapSortedByValue(views);
    }

    public View getView(String viewPK, String language) {
        try {
            WebContext ctx = WebContextFactory.get();
            String concept = CommonDWR.getConceptFromBrowseItemView(viewPK);
            Configuration config = Configuration.getInstance();
            String model = config.getModel();
            View view = new View(viewPK, language);
            WSConceptKey key = Util.getPort().getBusinessConceptKey(
                    new WSGetBusinessConceptKey(new WSDataModelPK(model), concept));

            String[] keys = key.getFields();
            keys = Arrays.copyOf(keys, keys.length);
            for (int i = 0; i < keys.length; i++) {
                if (".".equals(key.getSelector())) //$NON-NLS-1$
                    keys[i] = "/" + concept + "/" + keys[i]; //$NON-NLS-1$  //$NON-NLS-2$
                else
                    keys[i] = key.getSelector() + keys[i];
            }
            view.setKeys(keys);
            ctx.getSession().setAttribute("foreignKeys", keys); //$NON-NLS-1$
            view.setMetaDataTypes(getMetaDataTypes(view));
            return view;
        } catch (RemoteException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * return a list of viewable elements o a browse items list used for column header of a grid
     * 
     * @param viewPK
     * @param language
     * @return an array of label
     */

    public String[] getViewables(String viewPK, String language) {
        WebContext ctx = WebContextFactory.get();
        ctx.getSession().setAttribute("viewNameItems", null); //$NON-NLS-1$
        try {
            Configuration config = Configuration.getInstance();
            String[] viewables = new View(viewPK, language).getViewables();
            String[] labelViewables = new String[viewables.length];
            HashMap<String, String> xpathToLabel = CommonDWR.getFieldsByDataModel(config.getModel(),
                    CommonDWR.getConceptFromBrowseItemView(viewPK), language, true);
            for (int i = 0; i < viewables.length; i++) {
                String labelViewable = ""; //$NON-NLS-1$
                String path = viewables[i];
                String label = xpathToLabel.get(viewables[i]);
                if (label == null) {
                    labelViewable = path;
                } else {
                    if (DynamicLabelUtil.isDynamicLabel(label)) {
                        String field = Util.getFieldFromPath(path);// get field
                        labelViewable = (field == null ? path : field);
                    } else {
                        labelViewable = label;
                    }
                }
                labelViewables[i] = labelViewable;
            }
            return labelViewables;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }

    }

    public TreeNode getRootNode(String concept, String language) throws RemoteException, Exception {
        return getRootNode2(concept, null, -1, language);
    }

    public TreeNode getRootNode2(String concept, String[] ids, int docIndex, String language) throws RemoteException, Exception {

        Configuration config = Configuration.getInstance(true);
        String dataModelPK = config.getModel();
        Map<String, XSElementDecl> map = CommonDWR.getConceptMap(dataModelPK);
        XSElementDecl decl = map.get(concept);
        if (decl == null) {
            String err = "Concept '" + concept + "' is not found in model '" + dataModelPK + "'"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            LOG.error(err);
            return null;
        }
        XSAnnotation xsa = decl.getAnnotation();
        TreeNode rootNode = new TreeNode();
        ArrayList<String> roles = Util.getAjaxSubject().getRoles();
        rootNode.fetchAnnotations(xsa, roles, language);

        try {
            updateRootNodeByPKInfo(config.getCluster(), concept, ids, rootNode, docIndex);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        // reset the foreignKeys
        prepareSessionForItemDetails(concept, language);
        return rootNode;

    }

    private void updateRootNodeByPKInfo(String dataClusterPK, String concept, String[] ids, TreeNode rootNode, int docIndex)
            throws Exception {
        WebContext ctx = WebContextFactory.get();
        if (ids != null) {
            WSItem wsItem = Util.getPort().getItem(new WSGetItem(new WSItemPK(new WSDataClusterPK(dataClusterPK), concept, ids)));
            rootNode.setTaskId(wsItem.getTaskId());
            ctx.getSession().setAttribute("existItem", true); //$NON-NLS-1$
            Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
            Document document = Util.parse(wsItem.getContent());
            if (d != null && !document.isEqualNode(d)) {
                ctx.getSession().setAttribute("itemDocument" + docIndex, document); //$NON-NLS-1$
            }
            if (rootNode.getPrimaryKeyInfo() != null && rootNode.getPrimaryKeyInfo().size() > 0 && ids != null) {

                StringBuilder gettedValue = new StringBuilder();
                for (String pkInfoPath : rootNode.getPrimaryKeyInfo()) {
                    if (pkInfoPath != null && pkInfoPath.length() > 0) {
                        String pkInfo = Util.getFirstTextNode(document, pkInfoPath);
                        if (pkInfo != null) {
                            if (gettedValue.length() == 0)
                                gettedValue.append(pkInfo);
                            else
                                gettedValue.append("-").append(pkInfo); //$NON-NLS-1$
                            ;
                        }
                    }
                }

                rootNode.setName(gettedValue.toString());

                // update session
                if (docIndex != -1) {
                    ctx.getSession().setAttribute("itemDocument" + docIndex + "_wsItem", wsItem); //$NON-NLS-1$ //$NON-NLS-2$
                    ctx.getSession().setAttribute("itemDocument" + docIndex, document); //$NON-NLS-1$
                }
                // FIXME: when extractUsingTransformerThroughView
            }
        } else {
            ctx.getSession().removeAttribute("xpathToTreeNode"); //$NON-NLS-1$
            ctx.getSession().removeAttribute("existItem"); //$NON-NLS-1$
        }
    }

    /**
     * DOC HSHU Comment method "reloadItem".
     * 
     * @throws Exception
     */
    public void reloadItem(String concept, String[] ids, int docIndex) throws Exception {

        Configuration config = Configuration.getInstance();
        String dataClusterPK = config.getCluster();

        WSItem wsItem = Util.getPort().getItem(new WSGetItem(new WSItemPK(new WSDataClusterPK(dataClusterPK), concept, ids)));
        Document document = Util.parse(wsItem.getContent());

        WebContext ctx = WebContextFactory.get();
        ctx.getSession().setAttribute("itemDocument" + docIndex + "_xmlstring", wsItem.getContent()); //$NON-NLS-1$ //$NON-NLS-2$
        ctx.getSession().setAttribute("itemDocument" + docIndex + "_wsItem", wsItem); //$NON-NLS-1$ //$NON-NLS-2$
        ctx.getSession().setAttribute("itemDocument" + docIndex, document); //$NON-NLS-1$
        ctx.getSession().setAttribute("itemDocument" + docIndex + "_backup", Util.copyDocument(document)); //$NON-NLS-1$ //$NON-NLS-2$

        ctx.getSession().setAttribute("itemDocument" + docIndex + "_status", DOC_STATUS_EDIT);//$NON-NLS-1$ //$NON-NLS-2$
        ctx.getSession().setAttribute("cloneXpath" + docIndex, null); //$NON-NLS-1$   
    }

    /**
     * start to parse the xsd. set the maps : idToParticle, idToXpath and the list : nodeAutorization in the session
     * 
     * @param concept
     * @param ids
     * @param nodeId the id of the root node in yui tree
     * @return an error or succes message
     */
    public String setTree(String concept, String viewName, String[] ids, int nodeId, boolean foreignKey, int docIndex,
            boolean refresh) throws Exception {
        WebContext ctx = WebContextFactory.get();
        try {
            // fix bug 0019565, maybe the following code cause the pb
            // if (ids == null) {
            //                String[] idsExist = (String[]) ctx.getSession().getAttribute("treeIdxToIDS" + docIndex); //$NON-NLS-1$
            // if (idsExist != null && idsExist.length > 0) {
            // ids = idsExist;
            // }
            // }
            Configuration config = Configuration.getInstance();
            String dataModelPK = config.getModel();
            String dataClusterPK = config.getCluster();
            String xsd = Util.getPort().getDataModel(new WSGetDataModel(new WSDataModelPK(dataModelPK))).getXsdSchema();
            Map<String, XSElementDecl> map = com.amalto.core.util.Util.getConceptMap(xsd);
            XSElementDecl xsed = map.get(concept);
            XSComplexType xsct = (XSComplexType) (xsed.getType());

            BusinessConcept businessConcept = SchemaWebAgent.getInstance().getBusinessConcept(concept);
            businessConcept.load();
            ctx.getSession().setAttribute("itemDocument_businessConcept" + docIndex, businessConcept); //$NON-NLS-1$

            // set status
            if (ids != null) {
                ctx.getSession().setAttribute("itemDocument" + docIndex + "_status", DOC_STATUS_EDIT); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                ctx.getSession().setAttribute("itemDocument" + docIndex + "_status", DOC_STATUS_NEW); //$NON-NLS-1$ //$NON-NLS-2$
            }

            // get item
            if (ids != null) {

                WSItem wsItem = null;
                // fix bug 0019565
                //                if (ctx.getSession().getAttribute("itemDocument" + docIndex + "_wsItem") == null) //$NON-NLS-1$ //$NON-NLS-2$
                wsItem = Util.getPort().getItem(new WSGetItem(new WSItemPK(new WSDataClusterPK(dataClusterPK), concept, ids)));
                // else {
                //                    wsItem = (WSItem) ctx.getSession().getAttribute("itemDocument" + docIndex + "_wsItem");//$NON-NLS-1$ //$NON-NLS-2$
                // }

                try {
                    extractUsingTransformerThroughView(concept, viewName, ids, dataModelPK, dataClusterPK, map, wsItem);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }

                Document document = null;
                document = Util.parse(wsItem.getContent());
                // fix bug 0019565
                //                if (ctx.getSession().getAttribute("itemDocument" + docIndex) == null) { //$NON-NLS-1$
                // document = Util.parse(wsItem.getContent());
                // } else {
                //                    document = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
                // }

                ctx.getSession().setAttribute("itemDocument" + docIndex + "_backup", Util.copyDocument(document)); //$NON-NLS-1$ //$NON-NLS-2$
                // update the node according to schema
                // if("sequence".equals(com.amalto.core.util.Util.getConceptModelType(concept, xsd))) {
                Node newNode = com.amalto.core.util.Util.updateNodeBySchema(concept, xsd, document.getDocumentElement());
                document = newNode.getOwnerDocument();
                // }
                if (foreignKey)
                    ctx.getSession().setAttribute("itemDocumentFK", document); //$NON-NLS-1$
                else {
                    // remember the last insert time
                    ctx.getSession().setAttribute("itemDocument" + docIndex + "_wsItem", wsItem); //$NON-NLS-1$ //$NON-NLS-2$
                    ctx.getSession().setAttribute("itemDocument" + docIndex, document); //$NON-NLS-1$
                }
            } else if (!refresh) {
                createItem(concept, docIndex);
            }

            // apply display rules
            try {

                DisplayRulesUtil displayRulesUtil = new DisplayRulesUtil(xsed);
                ctx.getSession().setAttribute("itemDocument_displayRulesUtil" + docIndex, displayRulesUtil); //$NON-NLS-1$

                ctx.getSession().setAttribute("polymToBusinessConcept" + docIndex, new HashMap<List, BusinessConcept>());//$NON-NLS-1$
                ctx.getSession().setAttribute("polymToDisplayRulesUtil" + docIndex, new HashMap<List, DisplayRulesUtil>());//$NON-NLS-1$
                // updateDspRules(docIndex, itemDocument, concept);

            } catch (Exception e) {
                throw new XtentisWebappException("Exception happened during parsing display rules! ", e); //$NON-NLS-1$
            }

            ctx.getSession().setAttribute("xpathToPolymType" + docIndex, new HashMap<String, String>()); //$NON-NLS-1$
            ctx.getSession().setAttribute("xpathToPolymFKType" + docIndex, new HashMap<String, String>()); //$NON-NLS-1$           

            HashMap<Integer, XSParticle> idToParticle;
            if (ctx.getSession().getAttribute("idToParticle") == null) { //$NON-NLS-1$
                idToParticle = new HashMap<Integer, XSParticle>();
            } else {
                idToParticle = (HashMap<Integer, XSParticle>) ctx.getSession().getAttribute("idToParticle"); //$NON-NLS-1$
            }
            idToParticle.put(nodeId, xsct.getContentType().asParticle());
            ctx.getSession().setAttribute("idToParticle", idToParticle); //$NON-NLS-1$

            HashMap<Integer, String> idToXpath;
            if (ctx.getSession().getAttribute("idToXpath" + docIndex) == null) { //$NON-NLS-1$
                idToXpath = new HashMap<Integer, String>();
            } else {
                idToXpath = (HashMap<Integer, String>) ctx.getSession().getAttribute("idToXpath" + docIndex); //$NON-NLS-1$
            }
            idToXpath.put(nodeId, "/" + concept);//$NON-NLS-1$
            ctx.getSession().setAttribute("idToXpath" + docIndex, idToXpath); //$NON-NLS-1$

            HashMap<String, XSParticle> xpathToParticle = new HashMap<String, XSParticle>();
            xpathToParticle.put("/" + concept, xsct.getContentType().asParticle());//$NON-NLS-1$
            ctx.getSession().setAttribute("xpathToParticle", xpathToParticle); //$NON-NLS-1$

            ArrayList<String> nodeAutorization = new ArrayList<String>();
            ctx.getSession().setAttribute("nodeAutorization", nodeAutorization); //$NON-NLS-1$

            // added by lzhang, clean the update status session
            ctx.getSession().setAttribute("updatedPath" + docIndex, new HashMap<String, UpdateReportItem>()); //$NON-NLS-1$   
            ctx.getSession().setAttribute("cloneXpath" + docIndex, null); //$NON-NLS-1$  

            // added by lzhang
            if (ctx.getSession().getAttribute("itemDocument" + docIndex + "_xmlstring") != null) //$NON-NLS-1$ //$NON-NLS-2$
                ctx.getSession().setAttribute("itemDocument" + docIndex + "_xmlstring", null);//$NON-NLS-1$ //$NON-NLS-2$

            return ids != null ? Util.joinStrings(ids, ".") : null; //$NON-NLS-1$

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    private void updateDspRules(int docIndex, Document itemDocument, String concept) throws Exception {
        Configuration config = Configuration.getInstance();
        String dataModelPK = config.getModel();
        WebContext ctx = WebContextFactory.get();
        if (ctx.getSession().getAttribute("itemDocument_displayRulesUtil" + docIndex) == null) //$NON-NLS-1$
            return;


        HashMap<List, BusinessConceptForBrowser> polymToBusinessConcept = (HashMap<List, BusinessConceptForBrowser>) ctx
                .getSession().getAttribute("polymToBusinessConcept" + docIndex); //$NON-NLS-1$  
        HashMap<List, DisplayRulesUtil> polymToDisplayRulesUtil = (HashMap<List, DisplayRulesUtil>) ctx.getSession()
                .getAttribute("polymToDisplayRulesUtil" + docIndex); //$NON-NLS-1$  

        HashMap<String, String> xpathToPolymType = (HashMap<String, String>) ctx.getSession().getAttribute(
                "xpathToPolymType" + docIndex); //$NON-NLS-1$  

        HashMap<String, String> SortedxpathToPolymType = null;

        Document doc = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
        DisplayRulesUtil displayRulesUtil = null;
        BusinessConceptForBrowser businessConcept = getBusinessConcept(polymToBusinessConcept, xpathToPolymType);
        List<DisplayRule> dspRules = new ArrayList<DisplayRule>();
        Map<String, String> defaultValueRules = null;
        Map<String, String> visibleRules = null;
        String rulesStyle = null;

        if (xpathToPolymType != null && xpathToPolymType.size() > 0) {
            List polymList = new ArrayList();
            for (Iterator<String> iterator = xpathToPolymType.keySet().iterator(); iterator.hasNext();) {
                String subType = xpathToPolymType.get(iterator.next());
                // if (!polymList.contains(subType))
                polymList.add(subType);
            }

            if (businessConcept == null) {
                SortedxpathToPolymType = sortPolymTypeByXpath(xpathToPolymType, doc);
                String xsd = Util.getPort().getDataModel(new WSGetDataModel(new WSDataModelPK(dataModelPK))).getXsdSchema();
                Document xsdDoc = Util.parse(xsd);
                List<Node> rawNodeList = new ArrayList<Node>();
                for (Iterator<String> iterator = SortedxpathToPolymType.keySet().iterator(); iterator.hasNext();) {
                    String xpath = (String) iterator.next();
                    if (xpath.indexOf("[") > -1) { //$NON-NLS-1$
                        String mainPath = XpathUtil.getMainXpath(xpath);
                        String basePath = "//xsd:element[@name=\"" + mainPath.substring(mainPath.lastIndexOf("/") + 1) + "\"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$                       
                        Node rawNode = Util.getNodeList(xsdDoc, basePath).item(0);
                        Node backupNode = rawNode.cloneNode(true);
                        for (int index = 0; index < backupNode.getAttributes().getLength(); index++) {
                            if (backupNode.getAttributes().item(index).getNodeName().equals("name")) { //$NON-NLS-1$
                                backupNode.getAttributes().item(index).getFirstChild().setNodeValue(getDspXpathName(xpath));
                            }
                            if (backupNode.getAttributes().item(index).getNodeName().equals("type")) { //$NON-NLS-1$
                                backupNode.getAttributes().item(index).getFirstChild()
                                        .setNodeValue(SortedxpathToPolymType.get(xpath));
                            }
                        }
                        // synchronize node in document
                        // Util.getNodeList(itemDocument, xpath).item(0).set
                        rawNode.getParentNode().insertBefore(backupNode, rawNode);
                        rawNodeList.add(rawNode);
                    } else {
                        String basePath = "//xsd:element[@name=\"" + xpath.substring(xpath.lastIndexOf("/") + 1) + "\"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        for (int index = 0; index < Util.getNodeList(xsdDoc, basePath).item(0).getAttributes().getLength(); index++) {
                            if (Util.getNodeList(xsdDoc, basePath).item(0).getAttributes().item(index).getNodeName()
                                    .equals("type")) { //$NON-NLS-1$
                                Util.getNodeList(xsdDoc, basePath).item(0).getAttributes().item(index).getFirstChild()
                                        .setNodeValue(SortedxpathToPolymType.get(xpath));
                                break;
                            }
                        }
                    }

                }
                // remove basic node
                if (!rawNodeList.isEmpty())
                    for (Node node : rawNodeList) {
                        if (node.getParentNode() != null)
                            node.getParentNode().removeChild(node);
                    }

                Map<String, XSElementDecl> map = com.amalto.core.util.Util.getConceptMap(CommonDWR
                        .getXMLStringFromDocument(xsdDoc));
                XSElementDecl xsed = map.get(concept);

                businessConcept = new BusinessConceptForBrowser(xsed);
                businessConcept.load();
                polymToBusinessConcept.put(polymList, businessConcept);

                displayRulesUtil = new DisplayRulesUtil(xsed);
                polymToDisplayRulesUtil.put(polymList, displayRulesUtil);
            } else
                displayRulesUtil = polymToDisplayRulesUtil.get(polymList);

            defaultValueRules = businessConcept.getDefaultValueRulesMap();
            visibleRules = businessConcept.getVisibleRulesMap();
        } else {
            BusinessConcept businessConcept2 = (BusinessConcept) ctx.getSession().getAttribute(
                    "itemDocument_businessConcept" + docIndex); //$NON-NLS-1$
            displayRulesUtil = (DisplayRulesUtil) ctx.getSession().getAttribute("itemDocument_displayRulesUtil" + docIndex); //$NON-NLS-1$

            defaultValueRules = businessConcept2.getDefaultValueRulesMap();
            visibleRules = businessConcept2.getVisibleRulesMap();
        }

        if (defaultValueRules.size() > 0) {
            Document tmpDocument = Util.copyDocument(doc);
            for (Iterator<String> iterator = defaultValueRules.keySet().iterator(); iterator.hasNext();) {
                String xPath = (String) iterator.next();
                if (Util.getNodeList(tmpDocument, xPath).getLength() > 0
                        && Util.getNodeList(tmpDocument, xPath).item(0).getFirstChild() != null)
                    Util.getNodeList(tmpDocument, xPath).item(0).getFirstChild().setNodeValue(""); //$NON-NLS-1$
            }

            try {
                rulesStyle = displayRulesUtil.genDefaultValueStyle(XmlUtil.parseDocument(doc));
                org.dom4j.Document transformedDocumentValue = XmlUtil.styleDocument(tmpDocument, rulesStyle);
                for (Iterator<String> iterator = defaultValueRules.keySet().iterator(); iterator.hasNext();) {
                    String xpath = (String) iterator.next();
                    String value = displayRulesUtil.evalDefaultValueRuleResult(transformedDocumentValue, xpath);
                    if (value != null) {
                        dspRules.add(new DisplayRule(BusinessConcept.APPINFO_X_DEFAULT_VALUE_RULE, xpath, value));
                    }
                }
                tmpDocument = null;
                transformedDocumentValue = null;
            } catch (Exception e) {
                throw new XtentisWebappException(MESSAGES.getMessage("style.error"), e); //$NON-NLS-1$
            }
        }

        if (visibleRules.size() > 0) {
            if (rulesStyle == null)
                rulesStyle = displayRulesUtil.genDefaultValueStyle(XmlUtil.parseDocument(doc));
            org.dom4j.Document transformedDocumentVisible = XmlUtil.styleDocument(doc, rulesStyle);
            rulesStyle = displayRulesUtil.genVisibleStyle(transformedDocumentVisible);
            transformedDocumentVisible = XmlUtil.styleDocument(transformedDocumentVisible, rulesStyle);
            for (Iterator<String> iterator = visibleRules.keySet().iterator(); iterator.hasNext();) {
                String xpath = (String) iterator.next();
                String value = displayRulesUtil.evalVisibleRuleResult(transformedDocumentVisible, xpath);
                if (value != null && value.equals("false")) {//$NON-NLS-1$
                    dspRules.add(new DisplayRule(BusinessConcept.APPINFO_X_VISIBLE_RULE, xpath, "false"));//$NON-NLS-1$
                } else if (value == null || (value != null && value.equals("true"))) {//$NON-NLS-1$
                    dspRules.add(new DisplayRule(BusinessConcept.APPINFO_X_VISIBLE_RULE, xpath, "true"));//$NON-NLS-1$
                }
            }
            transformedDocumentVisible = null;
        }

        ctx.getSession().setAttribute("displayRules" + docIndex, dspRules); //$NON-NLS-1$ 
    }

    private HashMap<String, String> sortPolymTypeByXpath(HashMap<String, String> xpathToPolymType, Document doc) {
        LinkedHashMap<String, String> tmpMap = new LinkedHashMap<String, String>();
        List<String> tmpList = new ArrayList<String>();
        int i = 0;
        for (Iterator<String> iterator = xpathToPolymType.keySet().iterator(); iterator.hasNext();) {
            i++;
            String xpath = (String) iterator.next();
            if (tmpList.isEmpty())
                tmpList.add(xpath);
            else {
                for (int index = tmpList.size() - 1; index > -1; index--) {
                    String tmpXpath = tmpList.get(index);
                    try {
                        if (ifLargePosi(Util.getNodeList(doc, tmpXpath).item(0), Util.getNodeList(doc, xpath).item(0))) {
                            if (index + 1 >= tmpList.size())
                                tmpList.add(xpath);
                            else
                                tmpList.add(index + 1, xpath);
                            break;
                        }
                    } catch (DOMException e) {
                        LOG.error(e.getMessage(), e);
                        return tmpMap;
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                        return tmpMap;
                    }
                }
                if (tmpList.size() < i)
                    tmpList.add(0, xpath);
            }
        }

        for (int index = 0; index < tmpList.size(); index++) {
            tmpMap.put(tmpList.get(index), xpathToPolymType.get(tmpList.get(index)));
        }

        return tmpMap;
    }

    private boolean ifLargePosi(Node node, Node refNode) {
        if (node.equals(refNode))
            return true;
        if (node.hasChildNodes()) {
            for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                if (node.getChildNodes().item(i).equals(refNode))
                    return true;
                if (ifLargePosi(node.getChildNodes().item(i), refNode))
                    return true;
            }
        }
        while (node.getNextSibling() != null) {
            if (node.getNextSibling().equals(refNode))
                return true;
            for (int i = 0; i < node.getNextSibling().getChildNodes().getLength(); i++) {
                if (node.getNextSibling().getChildNodes().item(i).equals(refNode))
                    return true;
                if (ifLargePosi(node.getNextSibling().getChildNodes().item(i), refNode))
                    return true;
            }
            node = node.getNextSibling();
        }
        return false;
    }

    private BusinessConceptForBrowser getBusinessConcept(HashMap<List, BusinessConceptForBrowser> polymToBusinessConcept,
            HashMap<String, String> xpathToPolymType) {
        for (List keyList : polymToBusinessConcept.keySet()) {
            if (!keyList.isEmpty()) {
                List tmpList = new ArrayList();
                Iterator it = keyList.iterator();
                while (it.hasNext())
                    tmpList.add(it.next());

                for (Iterator<String> iterator = xpathToPolymType.keySet().iterator(); iterator.hasNext();) {
                    String xpath = iterator.next();
                    if (!tmpList.contains(xpathToPolymType.get(xpath))) {
                        return null;
                    } else if (tmpList.isEmpty()) {
                        return null;
                    } else if (tmpList.contains(xpathToPolymType.get(xpath)))
                        tmpList.remove(xpathToPolymType.get(xpath));
                }
                if (tmpList.isEmpty())
                    return polymToBusinessConcept.get(keyList);
            }
        }
        return null;
    }

    private String getDspXpathName(String unifiedXPath) {// add separator in left and right sides to avoid same named in
        // xsd
        return unifiedXPath.substring(unifiedXPath.lastIndexOf("/") + 1).replaceAll("\\[", "00000").replaceAll("\\]", "00000"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    /**
     * @param concept
     * @param ids
     * @param dataModelPK
     * @param dataClusterPK
     * @param map
     * @param wsItem
     * @throws RemoteException
     * @throws XtentisWebappException
     * @throws UnsupportedEncodingException
     * @throws Exception
     * @throws XPathExpressionException
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerConfigurationException
     * @throws TransformerException
     * 
     * 1.see if there is a job in the view 2.invoke the job. 3.convert the job's return value into xml doc, 4.convert
     * the wsItem's xml String value into xml doc, 5.cover wsItem's xml with job's xml value. step 6 and 7 must do
     * first. 6.add properties into ViewPOJO. 7.add properties into webservice parameter.
     */
    private void extractUsingTransformerThroughView(String concept, String viewName, String[] ids, String dataModelPK,
            String dataClusterPK, Map<String, XSElementDecl> map, WSItem wsItem) throws RemoteException, XtentisWebappException,
            UnsupportedEncodingException, Exception, XPathExpressionException, TransformerFactoryConfigurationError,
            TransformerConfigurationException, TransformerException {
        if (viewName == null || viewName.length() == 0)
            return;

        WSView view = Util.getPort().getView(new WSGetView(new WSViewPK(viewName)));

        if ((null != view.getTransformerPK() && view.getTransformerPK().length() != 0) && view.getIsTransformerActive().is_true()) {
            String transformerPK = view.getTransformerPK();
            // FIXME: consider about revision
            // String itemPK = dataClusterPK + "." + concept + "." + Util.joinStrings(ids, ".");
            String passToProcessContent = wsItem.getContent();

            WSTypedContent typedContent = new WSTypedContent(null, new WSByteArray(passToProcessContent.getBytes("UTF-8")), //$NON-NLS-1$
                    "text/xml; charset=UTF-8"); //$NON-NLS-1$

            WSTransformerContext wsTransformerContext = new WSTransformerContext(new WSTransformerV2PK(transformerPK), null, null);

            WSExecuteTransformerV2 wsExecuteTransformerV2 = new WSExecuteTransformerV2(wsTransformerContext, typedContent);
            // check binding transformer
            // we can leverage the exception mechanism also
            boolean isATransformerExist = false;
            WSTransformerPK[] wst = Util.getPort().getTransformerPKs(new WSGetTransformerPKs("*")).getWsTransformerPK(); //$NON-NLS-1$
            for (int i = 0; i < wst.length; i++) {
                if (wst[i].getPk().equals(transformerPK)) {
                    isATransformerExist = true;
                    break;
                }
            }
            // execute
            WSTransformer wsTransformer = Util.getPort().getTransformer(new WSGetTransformer(new WSTransformerPK(transformerPK)));
            if (wsTransformer.getPluginSpecs() == null || wsTransformer.getPluginSpecs().length == 0)
                throw new Exception("The Plugin Specs of this process is undefined! "); //$NON-NLS-1$
            WSTransformerContextPipelinePipelineItem[] entries = null;
            if (isATransformerExist) {

                entries = Util.getPort().executeTransformerV2(wsExecuteTransformerV2).getPipeline().getPipelineItem();

            } else {
                // return false;
                throw new Exception("The target process is not existed! "); //$NON-NLS-1$
            }

            WSTransformerContextPipelinePipelineItem entrie = null;
            boolean flag = false;
            // FIXME:use 'output' as spec.
            for (int i = 0; i < entries.length; i++) {
                if ("output".equals(entries[i].getVariable())) { //$NON-NLS-1$
                    entrie = entries[i];
                    flag = !flag;
                    break;
                }
            }
            if (!flag) {
                for (int i = 0; i < entries.length; i++) {
                    if ("_DEFAULT_".equals(entries[i].getVariable())) { //$NON-NLS-1$
                        entrie = entries[i];
                        break;
                    }
                }
            }
            String xmlStringFromProcess;
            if (entrie.getWsTypedContent().getWsBytes().getBytes() != null
                    && entrie.getWsTypedContent().getWsBytes().getBytes().length != 0) {
                xmlStringFromProcess = new String(entrie.getWsTypedContent().getWsBytes().getBytes(), "UTF-8"); //$NON-NLS-1$
            } else {
                xmlStringFromProcess = null;
            }

            if (null != xmlStringFromProcess && xmlStringFromProcess.length() != 0) {
                Document wsItemDoc = Util.parse(wsItem.getContent());
                Document jobDoc = Util.parse(xmlStringFromProcess);

                ArrayList<String> lookupFieldsForWSItemDoc = new ArrayList<String>();
                XSElementDecl elementDecl = map.get(concept);
                XSAnnotation xsa = elementDecl.getAnnotation();
                if (xsa != null && xsa.getAnnotation() != null) {
                    Element el = (Element) xsa.getAnnotation();
                    NodeList annotList = el.getChildNodes();
                    for (int k = 0; k < annotList.getLength(); k++) {
                        if ("appinfo".equals(annotList.item(k).getLocalName())) { //$NON-NLS-1$
                            Node source = annotList.item(k).getAttributes().getNamedItem("source"); //$NON-NLS-1$
                            if (source == null)
                                continue;
                            String appinfoSource = annotList.item(k).getAttributes().getNamedItem("source").getNodeValue(); //$NON-NLS-1$
                            if ("X_Lookup_Field".equals(appinfoSource)) { //$NON-NLS-1$

                                lookupFieldsForWSItemDoc.add(annotList.item(k).getFirstChild().getNodeValue());
                            }
                        }
                    }
                }

                // TODO String
                String searchPrefix;
                NodeList attrNodeList = Util.getNodeList(jobDoc, "/results/item/attr"); //$NON-NLS-1$
                if (attrNodeList != null && attrNodeList.getLength() > 0)
                    searchPrefix = "/results/item/attr/"; //$NON-NLS-1$
                else
                    searchPrefix = ""; //$NON-NLS-1$

                for (Iterator<String> iterator = lookupFieldsForWSItemDoc.iterator(); iterator.hasNext();) {
                    String xpath = iterator.next();
                    String firstValue = Util.getFirstTextNode(jobDoc, searchPrefix + xpath);// FIXME:use first node
                    if (null != firstValue && firstValue.length() != 0) {
                        XObject xObjectWSItem = XPathAPI.eval(wsItemDoc, xpath, wsItemDoc);
                        if (xObjectWSItem != null) {
                            NodeList wSItemNodes = xObjectWSItem.nodelist();
                            if (wSItemNodes.item(0) != null) {
                                wSItemNodes.item(0).setTextContent(firstValue);
                            }
                        }
                    }
                }
                wsItem.setContent(Util.nodeToString(wsItemDoc));
            }
        }
    }

    private int getBiggestIdFromidToParticle(HashMap<Integer, String> idToXpath) {
        int ret = 1;
        for (Integer v : idToXpath.keySet()) {
            if (v > ret) {
                ret = v;
            }
        }
        return ret;
    }

    private void setChildrenWithKeyMask(int id, String language, boolean foreignKey, int docIndex, boolean maskKey,
            boolean choice, XSParticle xsp, ArrayList<TreeNode> list, HashMap<String, TreeNode> xpathToTreeNode,
            String parentXpath) throws ParseException {

        WebContext ctx = WebContextFactory.get();
        HashMap<Integer, XSParticle> idToParticle = (HashMap<Integer, XSParticle>) ctx.getSession().getAttribute("idToParticle"); //$NON-NLS-1$
        HashMap<Integer, String> idToXpath = (HashMap<Integer, String>) ctx.getSession().getAttribute("idToXpath" + docIndex); //$NON-NLS-1$        
        if (parentXpath == null)
            parentXpath = idToXpath.get(id);
        // aiming added see 0009563
        if (xsp.getTerm().asModelGroup() != null) { // is complex type
            XSParticle[] xsps = xsp.getTerm().asModelGroup().getChildren();
            if ("choice".equals(xsp.getTerm().asModelGroup().getCompositor().toString())) //$NON-NLS-1$
                choice = true;
            for (int i = 0; i < xsps.length; i++) {
                setChildrenWithKeyMask(id, language, foreignKey, docIndex, maskKey, choice, xsps[i], list, xpathToTreeNode,
                        parentXpath);
            }
        }
        if (xsp.getTerm().asElementDecl() == null)
            return;
        // end

        HashMap<String, XSParticle> xpathToParticle = (HashMap<String, XSParticle>) ctx.getSession().getAttribute(
                "xpathToParticle"); //$NON-NLS-1$
        HashMap<String, String> xpathToPolymType = (HashMap<String, String>) ctx.getSession().getAttribute(
                "xpathToPolymType" + docIndex); //$NON-NLS-1$
        ArrayList<String> nodeAutorization = (ArrayList<String>) ctx.getSession().getAttribute("nodeAutorization"); //$NON-NLS-1$
        Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
        // reload the xmlstring
        String xmlString = (String) ctx.getSession().getAttribute("itemDocument" + docIndex + "_xmlstring"); //$NON-NLS-1$ //$NON-NLS-2$
        String[] cloneXpath = (String[]) ctx.getSession().getAttribute("cloneXpath" + docIndex); //$NON-NLS-1$
        if (xmlString != null && cloneXpath == null) {
            try {
                d = Util.parse(xmlString);
            } catch (Exception e) {
            }
        }
        Document bakDoc = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex + "_backup"); //$NON-NLS-1$ //$NON-NLS-2$
        String[] keys = (String[]) ctx.getSession().getAttribute("foreignKeys"); //$NON-NLS-1$

        ArrayList<String> roles = new ArrayList<String>();
        try {
            roles = Util.getAjaxSubject().getRoles();
        } catch (PolicyContextException e1) {
            LOG.error(e1.getMessage(), e1);
        }

        if (foreignKey)
            d = (Document) ctx.getSession().getAttribute("itemDocumentFK"); //$NON-NLS-1$
        TreeNode treeNode = new TreeNode();

        try {
            // judge polymiorphism
            String bindingType = xsp.getTerm().asElementDecl().getType().getName();
            List<ReusableType> subTypes = SchemaWebAgent.getInstance().getMySubtypes(bindingType);
            if (subTypes != null && subTypes.size() > 0) {
                treeNode.setPolymiorphism(true);
                treeNode.setAbstract(new ReusableType(xsp.getTerm().asElementDecl().getType()).isAbstract());
                ArrayList<SubTypeBean> subTypesName = new ArrayList<SubTypeBean>();
                for (ReusableType reusableType : subTypes) {
                    if (!reusableType.isAbstract()) {
                        SubTypeBean subTypeBean = new SubTypeBean();
                        reusableType.load();
                        subTypeBean.setName(reusableType.getName());
                        subTypeBean.setLabel(reusableType.getLabelMap().get(language) == null ? reusableType.getName()
                                : reusableType.getLabelMap().get(language));
                        subTypeBean.setOrderValue(reusableType.getOrderValue());
                        subTypesName.add(subTypeBean);
                    }
                }
                Collections.sort(subTypesName);
                treeNode.setSubTypes(subTypesName);
            }
            if (((String) ctx.getSession().getAttribute("itemDocument" + docIndex + "_status")).equals(DOC_STATUS_EDIT)) { //$NON-NLS-1$ //$NON-NLS-2$                
                String xpath = parentXpath + "/" + xsp.getTerm().asElementDecl().getName(); //$NON-NLS-1$
                if (bakDoc != null) {
                    getRealTypeFromDoc(xpathToPolymType, bakDoc, xpath, xpath, treeNode);
                }

            } else if (cloneXpath != null) {
                // new document and clone complex type node
                String xpath = parentXpath + "/" + xsp.getTerm().asElementDecl().getName(); //$NON-NLS-1$
                getRealTypeFromDoc(xpathToPolymType, d, xpath.startsWith(cloneXpath[1]) ? xpath.replace(cloneXpath[1],
                        cloneXpath[0]) : xpath, xpath, treeNode);

            }

        } catch (Exception e2) {
            LOG.error(e2.getMessage(), e2);
        }

        treeNode.setChoice(choice);
        String xpath = parentXpath + "/" + xsp.getTerm().asElementDecl().getName(); //$NON-NLS-1$
        treeNode.setBindingPath(xpath);
        if (ctx.getSession().getAttribute("existItem") == null && xpathToTreeNode.containsKey(xpath)) //$NON-NLS-1$
            treeNode.setDisplayDefalutValue(xpathToTreeNode.get(xpath).isDisplayDefalutValue());
        if (ctx.getSession().getAttribute("existItem") != null) //$NON-NLS-1$
            treeNode.setDisplayDefalutValue(false);
        // aiming modify see 9642 some node's parent is null
        String parentxpath = XpathUtil.getMainXpath(parentXpath); // parent xpath maybe A.fileds[1]
        if (xpathToTreeNode.containsKey(parentxpath)) {
            treeNode.setParent(xpathToTreeNode.get(parentxpath));
        }
        // end
        if (xpathToTreeNode.containsKey(parentXpath))
            treeNode.setParent(xpathToTreeNode.get(parentXpath));

        int maxOccurs = xsp.getMaxOccurs();
        // idToXpath.put(nodeCount,xpath);//keep map <node id -> xpath> in the session
        treeNode.setName(xsp.getTerm().asElementDecl().getName());
        treeNode.setLabel(xsp.getTerm().asElementDecl().getName());
        treeNode.setDocumentation(""); //$NON-NLS-1$
        treeNode.setVisible(true);

        String typeNameTmp;
        if (xsp.getTerm().asElementDecl().getType().getName() != null)
            typeNameTmp = xsp.getTerm().asElementDecl().getType().getName();
        else
            typeNameTmp = ""; //$NON-NLS-1$

        // annotation support
        XSAnnotation xsa = xsp.getTerm().asElementDecl().getAnnotation();
        try {
            treeNode.fetchAnnotations(xsa, roles, language);
        } catch (Exception e1) {
            LOG.error(e1.getMessage(), e1);
        }

        // attribute support
        try {
            treeNode.fetchAttributes(bakDoc, xpath);
        } catch (Exception e1) {
            LOG.error(e1.getMessage(), e1);
        }
        // dynamic load may cause YAHOO.widget.TreeView.nodeCount < the real created nodes count, so we should
        // set nodeCount as the biggest number in idToXpath
        int bigId = getBiggestIdFromidToParticle(idToXpath);
        if (nodeCount <= bigId) {
            nodeCount = (++bigId);
        }
        treeNode.setTypeName(typeNameTmp);
        treeNode.setXmlTag(xsp.getTerm().asElementDecl().getName());
        treeNode.setNodeId(nodeCount);
        treeNode.setMaxOccurs(maxOccurs);
        treeNode.setMinOccurs(xsp.getMinOccurs());
        treeNode.setNillable(xsp.getTerm().asElementDecl().isNillable());
        ArrayList<String> infos = treeNode.getForeignKeyInfo();

        try {
            String value = Util.getFirstTextNode(d, xpath);
            treeNode.setValue(value);
            if (infos != null && treeNode.isRetrieveFKinfos()) {

                // max occurs > 1 support and do not get foreignkeylist by here.
                if (value != null && value.length() != 0 && !(maxOccurs < 0 || maxOccurs > 1)) {
                    String gettedforeignKey = treeNode.getForeignKey();
                    if (treeNode.getUsingforeignKey() != null && treeNode.getUsingforeignKey().trim().length() > 0)
                        gettedforeignKey = treeNode.getUsingforeignKey();
                    treeNode.setValueInfo(getFKInfo(value, gettedforeignKey, infos));
                }

            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        // this child is a complex type
        if (xsp.getTerm().asElementDecl().getType().isComplexType() == true) {
            XSParticle particle = xsp.getTerm().asElementDecl().getType().asComplexType().getContentType().asParticle();
            idToParticle.put(nodeCount, particle);
            if (!treeNode.isReadOnly()) {
                nodeAutorization.add(xpath);
            }
            treeNode.setType("complex"); //$NON-NLS-1$

            xpathToTreeNode.put(xpath, treeNode);
            if (maxOccurs < 0 || maxOccurs > 1) { // maxoccurs<0 is unbounded
                try {
                    NodeList nodeList = Util.getNodeList(d, xpath);
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        String specificXpath = xpath + '[' + (i + 1) + ']';
                        idToXpath.put(nodeCount, specificXpath); 
                        xpathToParticle.put(specificXpath, particle); 
                        TreeNode treeNodeTmp = (TreeNode) treeNode.clone();
                        treeNodeTmp.setNodeId(nodeCount);
                        treeNodeTmp.setBindingPath(specificXpath); 
                        idToParticle.put(nodeCount, particle);
                        if (bakDoc != null) {
                            getRealTypeFromDoc(xpathToPolymType, bakDoc, specificXpath, specificXpath, treeNodeTmp);
                        } else if (cloneXpath != null) {
                            // new document and clone complex type node
                            getRealTypeFromDoc(xpathToPolymType, d, specificXpath.startsWith(cloneXpath[1]) ? specificXpath
                                    .replace(cloneXpath[1], cloneXpath[0]) : specificXpath, specificXpath, treeNodeTmp);
                        }
                        // TODO check addThisNode
                        if (treeNodeTmp.isVisible()) {
                            list.add(treeNodeTmp);
                            nodeCount++;
                        }
                        xpathToTreeNode.put(xpath + "[" + (i + 1) + "]", treeNodeTmp); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    if (nodeList.getLength() == 0) {
                        idToXpath.put(nodeCount, xpath);
                        xpathToParticle.put(xpath, particle);
                        if (treeNode.isVisible() == true) {
                            list.add(treeNode);
                            nodeCount++;
                        }
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            } else {
                idToXpath.put(nodeCount, xpath);
                xpathToParticle.put(xpath, particle);
                if (treeNode.isVisible() == true) {
                    list.add(treeNode);
                    nodeCount++;
                }
            }
        }
        // this child is a simple type
        else {
            idToParticle.put(nodeCount, null);
            treeNode.setType("simple"); //$NON-NLS-1$

            // restriction support
            ArrayList<Restriction> restrictions = new ArrayList<Restriction>();
            ArrayList<String> enumeration = new ArrayList<String>();
            XSRestrictionSimpleType restirctionType = xsp.getTerm().asElementDecl().getType().asSimpleType().asRestriction();
            if (restirctionType != null) {
                Iterator<XSFacet> it = restirctionType.iterateDeclaredFacets();
                while (it.hasNext()) {
                    XSFacet xsf = it.next();
                    if ("enumeration".equals(xsf.getName())) { //$NON-NLS-1$
                        enumeration.add(xsf.getValue().toString());
                    } else {
                        Restriction r = new Restriction(xsf.getName(), xsf.getValue().toString());
                        restrictions.add(r);
                    }
                }
            }
            treeNode.setEnumeration(enumeration);
            treeNode.setRestrictions(restrictions);

            // the user cannot edit any field when a foreign key is displayed
            if (foreignKey) {
                treeNode.setReadOnly(true);
            }
            for (int i = 0; i < keys.length; i++) {
                if (xpath.equals(keys[i])) {
                    treeNode.setKey(true);
                    treeNode.setKeyIndex(i);
                }

            }

            // max occurs > 1 support
            try {
                if (maxOccurs < 0 || maxOccurs > 1) {
                    NodeList nodeList = Util.getNodeList(d, xpath);

                    for (int i = 0; i < nodeList.getLength(); i++) {
                        if (!treeNode.isReadOnly())
                            nodeAutorization.add(xpath + "[" + (i + 1) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                        idToXpath.put(nodeCount, xpath + "[" + (i + 1) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                        TreeNode treeNodeTmp = (TreeNode) treeNode.clone();
                        String value = nodeList.item(i).getTextContent();
                        treeNodeTmp.setValue(value);
                        treeNodeTmp.setBindingPath(xpath + "[" + (i + 1) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                        if (nodeList.item(i).getFirstChild() != null && infos != null && treeNode.isRetrieveFKinfos()
                                && treeNode.getForeignKey() != null) {

                            treeNodeTmp.setValueInfo(getFKInfo(value, treeNode.getForeignKey(), infos));
                        }

                        treeNodeTmp.setNodeId(nodeCount);
                        // TODO check addThisNode
                        if (treeNodeTmp.isVisible()) {
                            list.add(treeNodeTmp);
                            nodeCount++;
                        }

                        xpathToTreeNode.put(xpath + "[" + (i + 1) + "]", treeNodeTmp); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    if (nodeList.getLength() == 0) {
                        if (!treeNode.isReadOnly())
                            nodeAutorization.add(xpath);
                        idToXpath.put(nodeCount, xpath);
                        if (treeNode.isVisible() == true) {
                            list.add(treeNode);
                            nodeCount++;
                        }
                    }
                } else {
                    if (!treeNode.isReadOnly())
                        nodeAutorization.add(xpath);
                    idToXpath.put(nodeCount, xpath);

                    treeNode.setValue(Util.getFirstTextNode(d, xpath));

                    // key is readonly for editing record.
                    if (treeNode.isKey() && treeNode.getValue() != null) {
                        treeNode.setReadOnly(true);
                    }

                    if (treeNode.isVisible() == true) {
                        list.add(treeNode);
                        nodeCount++;
                    }
                    xpathToTreeNode.put(xpath, treeNode);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        if (treeNode.isKey()
                && (treeNode.getTypeName().trim().toUpperCase().equals("UUID") || treeNode.getTypeName().trim().toUpperCase() //$NON-NLS-1$
                        .equals("AUTO_INCREMENT"))) { //$NON-NLS-1$
            ctx.getSession().setAttribute("hasAutoChangeField" + docIndex, true); //$NON-NLS-1$
        }

        if (maskKey && treeNode.isKey()) {
            String oldPath = treeNode.getValue();
            //treeNode.setValue(""); //$NON-NLS-1$
            if (treeNode.getTypeName().trim().toUpperCase().equals("UUID") //$NON-NLS-1$
                    || treeNode.getTypeName().trim().toUpperCase().equals("AUTO_INCREMENT") || (treeNode.getValue() != null && treeNode.getValue().trim().length() > 0)) { //$NON-NLS-1$
                treeNode.setReadOnly(true);
            } else {
                treeNode.setReadOnly(false);
            }

            HashMap<String, UpdateReportItem> updatedPath;
            if (ctx.getSession().getAttribute("updatedPath" + docIndex) != null) { //$NON-NLS-1$
                updatedPath = (HashMap<String, UpdateReportItem>) ctx.getSession().getAttribute("updatedPath" + docIndex); //$NON-NLS-1$
            } else {
                updatedPath = new HashMap<String, UpdateReportItem>();
            }
            ctx.getSession().setAttribute("updatedPath" + docIndex, updatedPath); //$NON-NLS-1$
            // modified by lzhang, fix 0020073
            if (!treeNode.isReadOnly())
                updatedPath.put(xpath, new UpdateReportItem(xpath, oldPath, "")); //$NON-NLS-1$

        }

    }

    private void getRealTypeFromDoc(HashMap<String, String> xpathToPolymType, Document doc, String docXpath, String storeXpath,
            TreeNode treeNode) throws Exception {
        NodeList tmpNodeList = Util.getNodeList(doc, docXpath);
        if (tmpNodeList != null && tmpNodeList.getLength() > 0) {
            if (tmpNodeList.item(0) instanceof Element) {
                Element firstElem = (Element) tmpNodeList.item(0);
                String realType = firstElem.getAttribute("xsi:type"); //$NON-NLS-1$
                if (realType != null && !realType.isEmpty()) {
                    treeNode.setRealType(realType);
                    xpathToPolymType.put(XpathUtil.unifyXPath(storeXpath), realType);
                }
            }
        }
    }

    /**
     * get FK info according to key
     * 
     * @param key :like [a][b]
     * @param foreignkey
     * @param fkInfos
     * @return
     */
    private String getFKInfo(String key, String foreignkey, List<String> fkInfos) {
        try {
            if (key == null || key.trim().length() == 0)
                return null;

            List<String> ids = new ArrayList<String>();

            if (!key.matches("^\\[(.*?)\\]$")) { //$NON-NLS-1$
                ids.add(key);
            } else {
                Pattern p = Pattern.compile("\\[(.*?)\\]"); //$NON-NLS-1$
                Matcher m = p.matcher(key);
                while (m.find()) {
                    ids.add(m.group(1));
                }
            }

            // Collections.reverse(ids);
            String concept = Util.getForeignPathFromPath(foreignkey);
            concept = concept.split("/")[0]; //$NON-NLS-1$
            Configuration config = Configuration.getInstance();
            String dataClusterPK = config.getCluster();

            WSItemPK wsItem = new WSItemPK(new WSDataClusterPK(dataClusterPK), concept, (String[]) ids.toArray(new String[ids
                    .size()]));
            WSItem item = Util.getPort().getItem(new WSGetItem(wsItem));
            if (item != null) {
                String content = item.getContent();
                Node node = Util.parse(content).getDocumentElement();
                if (fkInfos.size() > 0){
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < fkInfos.size(); i++) {
                        String info = fkInfos.get(i);
                        JXPathContext jxpContext = JXPathContext.newContext(node);
                        jxpContext.setLenient(true);
                        info = info.replaceFirst(concept + "/", ""); //$NON-NLS-1$ //$NON-NLS-2$
                        String fkinfo = (String) jxpContext.getValue(info, String.class);
                        if (fkinfo != null && fkinfo.length() != 0) {
                            sb.append(fkinfo);
                        }
                        if (i < fkInfos.size() - 1 && fkInfos.size() > 1) {
                            sb.append("-"); //$NON-NLS-1$
                        }
                    }
                    return sb.toString();
                } else {
                    return key;
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return key;
        }
        return null;
    }

    private int nodeCount; // aiming added to record the node count;

    /**
     * give the children of a node
     * 
     * @param id the id of the node in yui
     * @param nodeCount the internal count of nodes in yui tree
     * @param language
     * @return an array of TreeNode
     * @throws ParseException
     */
    // TreeNode parentNode,
    public TreeNode[] getChildren(int id, int nodeCount, String language, boolean foreignKey, int docIndex,
            String selectedExtendType) throws Exception {
        TreeNode[] nodes = getChildrenWithKeyMask(id, nodeCount, language, foreignKey, docIndex, false, selectedExtendType, null);
        return nodes;
    }

    public TreeNode[] getChildrenWithBindPath(int id, int nodeCount, String language, boolean foreignKey, int docIndex,
            String selectedExtendType, String bindpath) throws Exception {
        TreeNode[] nodes = getChildrenWithKeyMask(id, nodeCount, language, foreignKey, docIndex, false, selectedExtendType,
                bindpath);
        return nodes;
    }

    /**
     * DOC HSHU Comment method "handleDynamicLable".
     * 
     * @param nodes
     * @param docIndex
     * @throws Exception
     * 
     */
    private void handleDynamicLable(TreeNode[] nodes, int docIndex) throws XtentisWebappException {
        try {
            Document document = (Document) WebContextFactory.get().getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
            org.dom4j.Document parsedDocument = XmlUtil.parseDocument(document);
            if (nodes != null) {
                for (int i = 0; i < nodes.length; i++) {
                    String label = nodes[i].getName();
                    if (DynamicLabelUtil.isDynamicLabel(label)) {

                        label = replaceForeignPath(nodes[i].getBindingPath(), label, parsedDocument);

                        String stylesheet = DynamicLabelUtil.genStyle(nodes[i].getBindingPath(), label);
                        String parsedLabel = DynamicLabelUtil.getParsedLabel(DynamicLabelUtil.styleDocument(parsedDocument,
                                stylesheet));

                        if (parsedLabel != null) {
                            nodes[i].setName(parsedLabel);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new XtentisWebappException("Exception happened during parsing dynamic label! "); //$NON-NLS-1$
        }
    }

    private String replaceForeignPath(String basePath, String dynamicLabel, org.dom4j.Document doc) throws Exception {

        Pattern pattern = Pattern.compile("\\{.*?\\}");//$NON-NLS-1$
        Matcher matcher = pattern.matcher(dynamicLabel);
        List<String> dynamicPathes = new ArrayList<String>();
        while (matcher.find()) {
            dynamicPathes.add(matcher.group().replaceAll("^\\{", "").replaceAll("\\}$", ""));//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }

        Configuration config = Configuration.getInstance();
        String dataModelPK = config.getModel();
        String xsd = Util.getPort().getDataModel(new WSGetDataModel(new WSDataModelPK(dataModelPK))).getXsdSchema();
        Map<String, XSElementDecl> map = com.amalto.core.util.Util.getConceptMap(xsd);
        Map<String, XSType> typeMap = com.amalto.core.util.Util.getConceptTypeMap(xsd);
        basePath = basePath.startsWith("/") ? basePath.substring(1) : basePath; //$NON-NLS-1$
        XSElementDecl xsed = map.get(basePath.split("/")[0]); //$NON-NLS-1$

        for (String dyPath : dynamicPathes) {
            org.dom4j.Element baseEl = (org.dom4j.Element) doc.selectSingleNode(basePath);
            try {
                List els = (List) baseEl.selectNodes(dyPath);
                if (els == null)
                    continue;
                String multiValue = ""; //$NON-NLS-1$
                if (els.size() > 0){

	                for (int i = 0; i < els.size();i++){
		                List<org.dom4j.Element> pathNodes = getPathNode((org.dom4j.Element) els.get(i));
		                String key = ((org.dom4j.Element)els.get(i)).getStringValue();
		                Object[] fkObj = getForeign(xsed, pathNodes, 0, typeMap);
		                if (fkObj != null && ((List<String>)fkObj[1]).size() > 0) {
		                    String foreignkey = (String) fkObj[0];
		                    List<String> fkInfos = (List<String>) fkObj[1];

		                    String fkInfoStr = getFKInfo(key, foreignkey, fkInfos);
                            multiValue += fkInfoStr == null ? "" : fkInfoStr; //$NON-NLS-1$

		                } else {
                            multiValue += key == null ? "" : key; //$NON-NLS-1$
		                }
	                }
	                
	                dynamicLabel = dynamicLabel.replace("{" + dyPath + "}", multiValue == null ? "" : multiValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                }
            } catch (Exception e) {
                continue;
            }
        }
        return dynamicLabel;
    }

    private List<org.dom4j.Element> getPathNode(org.dom4j.Element el) {
        List<org.dom4j.Element> pathEls = new ArrayList<org.dom4j.Element>();
        org.dom4j.Element currentEl = el;
        while (currentEl != null) {
            pathEls.add(0, currentEl);
            currentEl = currentEl.getParent();
        }
        return pathEls;
    }

    private Object[] getForeign(XSElementDecl xsed, List<org.dom4j.Element> pathNodes, int pos, Map<String, XSType> typeMap) {

        XSType xsct = null;
        org.dom4j.Element el = pathNodes.get(pos);
        Attribute attr = el.attribute("type"); //$NON-NLS-1$
        String xsiType = attr == null ? null : attr.getStringValue();
        if (xsiType != null && !xsiType.equals("")) { //$NON-NLS-1$
            xsct = typeMap.get(xsiType);
        } else {
            xsct = xsed.getType();
        }

        if (pos < pathNodes.size() - 1) {
            XSParticle[] xsp = ((XSComplexType) xsct).getContentType().asParticle().getTerm().asModelGroup().getChildren();
            for (XSParticle xs : xsp) {
                List<XSElementDecl> dels = getElementDecls(xs);
                for (XSElementDecl del : dels) {
                    if (del.getName().equals(pathNodes.get(pos + 1).getName())) {
                        Object[] fkObj = getForeign(del, pathNodes, pos + 1, typeMap);
                        if (fkObj != null) {
                            return fkObj;
                        }
                    }
                }
            }
        } else {
            XSAnnotation anno = xsed.getAnnotation();
            if (anno != null) {
                Element annotation = (Element) anno.getAnnotation();
                if (annotation != null) {
                    NodeList annotList = annotation.getChildNodes();
                    if (annotList != null) {
                        Object[] fkObj = new Object[2];
                        String fk = null;
                        List<String> fkInfo = new ArrayList<String>();
                        for (int k = 0; k < annotList.getLength(); k++) {
                            if ("appinfo".equals(annotList.item(k).getLocalName())) { //$NON-NLS-1$ 
                                Node source = annotList.item(k).getAttributes().getNamedItem("source"); //$NON-NLS-1$
                                if (source == null)
                                    continue;
                                String appinfoSource = source.getNodeValue();
                                if ("X_ForeignKey".equals(appinfoSource)) { //$NON-NLS-1$
                                    fk = annotList.item(k).getFirstChild().getNodeValue();
                                    ;
                                } else if ("X_ForeignKeyInfo".equals(appinfoSource)) { //$NON-NLS-1$
                                    fkInfo.add(annotList.item(k).getFirstChild().getNodeValue());
                                }
                            }
                        }
                        if (fk != null) {
                            fkObj[0] = fk;
                            fkObj[1] = fkInfo;
                            return fkObj;
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<XSElementDecl> getElementDecls(XSParticle xs) {
        List<XSElementDecl> elDecls = new ArrayList<XSElementDecl>();
        if (xs.getTerm().asModelGroup() != null) { // is complex type
            XSParticle[] xsps = xs.getTerm().asModelGroup().getChildren();
            for (int i = 0; i < xsps.length; i++) {
                elDecls.addAll(getElementDecls(xsps[i]));
            }
        }
        XSElementDecl del = xs.getTerm().asElementDecl();
        if (del != null) {
            elDecls.add(del);
        }
        return elDecls;
    }

    private TreeNode[] handleDisplayRules(TreeNode[] nodes, int docIndex) throws XtentisWebappException {
        try {
            List<DisplayRule> dspRules = (List<DisplayRule>) WebContextFactory.get().getSession()
                    .getAttribute("displayRules" + docIndex); //$NON-NLS-1$
            if (nodes != null) {
                List<TreeNode> nodesList = new ArrayList(Arrays.asList(nodes));
                for (int i = 0; i < nodes.length; i++) {
                    DisplayRulesUtil.filterByDisplayRules(nodesList, nodes[i], dspRules, docIndex);
                }

                nodes = nodesList.toArray(new TreeNode[nodesList.size()]);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new XtentisWebappException("Exception happened during parsing display rules! "); //$NON-NLS-1$
        }
        return nodes;
    }

    public TreeNode[] getChildrenWithKeyMask(int id, int nodeCount, String language, boolean foreignKey, int docIndex,
            boolean maskKey, String selectedExtendType, String bindpath) throws Exception {
        synchronized (locker) {

            WebContext ctx = WebContextFactory.get();
            HashMap<Integer, XSParticle> idToParticle = (HashMap<Integer, XSParticle>) ctx.getSession().getAttribute(
                    "idToParticle"); //$NON-NLS-1$
            HashMap<Integer, String> idToXpath = (HashMap<Integer, String>) ctx.getSession().getAttribute("idToXpath" + docIndex); //$NON-NLS-1$
            Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
            HashMap<String, TreeNode> xpathToTreeNode = (HashMap<String, TreeNode>) ctx.getSession().getAttribute(
                    "xpathToTreeNode"); //$NON-NLS-1$
            selectedExtendType = (selectedExtendType != null && selectedExtendType.equals("") ? null : selectedExtendType);//$NON-NLS-1$

            String xpath = idToXpath.get(id);
            try {
                if (bindpath != null)
                    xpath = bindpath;
                // update doc
                if (selectedExtendType != null && selectedExtendType.length() > 0) {
                    Node tagertNode = Util.getNodeList(d, xpath).item(0);
                    if (tagertNode != null) {
                        NodeList childNodes = tagertNode.getChildNodes();
                        int listLength = childNodes.getLength();
                        for (int i = 0; i < listLength; i++) {
                            Util.removeAll(childNodes.item(0), (short) -1, null);
                        }
                        setChildrenWithValue(SchemaWebAgent.getInstance().getReusableType(selectedExtendType).getXsParticle(),
                                xpath, docIndex, true);
                        // added by lzhang, add xsi:type in order to deep clone
                        ((Element) tagertNode).setAttributeNS(
                                "http://www.w3.org/2001/XMLSchema-instance", "xsi:type", selectedExtendType); //$NON-NLS-1$; //$NON-NLS-2$; 
                        // synchornize with back doc
                        Document doc = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex + "_backup"); //$NON-NLS-1$ //$NON-NLS-2$
                        if (doc != null && Util.getNodeList(doc, xpath).item(0) != null) {
                            ((Element) Util.getNodeList(doc, xpath).item(0)).setAttributeNS(
                                    "http://www.w3.org/2001/XMLSchema-instance", "xsi:type", selectedExtendType); //$NON-NLS-1$; //$NON-NLS-2$; 
                        }
                    }
                }
                // keep changes when refresh
                HashMap<String, UpdateReportItem> updatedPath;
                if (ctx.getSession().getAttribute("updatedPath" + docIndex) != null) { //$NON-NLS-1$
                    updatedPath = (HashMap<String, UpdateReportItem>) ctx.getSession().getAttribute("updatedPath" + docIndex); //$NON-NLS-1$
                } else {
                    updatedPath = new HashMap<String, UpdateReportItem>();
                }
                if (updatedPath.size() > 0) {
                    for (Iterator<String> iterator = updatedPath.keySet().iterator(); iterator.hasNext();) {
                        String updatePath = iterator.next();
                        UpdateReportItem updateReportItem = updatedPath.get(updatePath);
                        NodeList gettedNodeList = Util.getNodeList(d, updatePath);
                        if (gettedNodeList != null && gettedNodeList.getLength() > 0) {
                            Node node = gettedNodeList.item(0);
                            if (updateReportItem != null)
                                node.setTextContent(updateReportItem.getNewValue());
                        }
                    }
                }
                // update polym-map
                HashMap<String, String> xpathToPolymType = (HashMap<String, String>) ctx.getSession().getAttribute(
                        "xpathToPolymType" + docIndex); //$NON-NLS-1$
                if (xpathToPolymType != null) {
                    if (selectedExtendType == null)
                        xpathToPolymType.remove(xpath);
                    else
                        xpathToPolymType.put(XpathUtil.unifyXPath(xpath), selectedExtendType);
                }

            } catch (Exception e1) {
                LOG.error(e1.getMessage(), e1);
            }

            if (xpathToTreeNode == null)
                xpathToTreeNode = new HashMap<String, TreeNode>();

            if (foreignKey)
                d = (Document) ctx.getSession().getAttribute("itemDocumentFK"); //$NON-NLS-1$

            boolean choice = false;

            XSParticle[] xsp = null;
            if (idToParticle == null)
                return null;
            XSParticle selXsp = idToParticle.get(id);

            if (selXsp == null && bindpath != null) {// simple type case, no children
                // fix bug 0023102, sometimes the complex arrow disappear after you operation Browse record a while
                String pxpath = bindpath;
                if (pxpath.startsWith("/")) { //$NON-NLS-1$
                    pxpath = pxpath.substring(1, pxpath.length());
                }
                if (pxpath != null) {
                    String concept = pxpath.split("/")[0]; //$NON-NLS-1$
                    Map<String, XSParticle> xpath2particle = SchemaWebAgent.getInstance().getXPath2ParticleMap(concept);
                    XSParticle particle = xpath2particle.get(pxpath);
                    if (particle != null && particle.getTerm().asElementDecl().getType().isComplexType()) {
                        selXsp = particle;
                    }
                }
            }
            if (selXsp == null)
                return null;
            this.nodeCount = nodeCount;// aiming added
            if (selXsp.getTerm().asModelGroup() == null) {
                selXsp = selXsp.getTerm().asElementDecl().getType().asComplexType().getContentType().asParticle();
            }
            xsp = selXsp.getTerm().asModelGroup().getChildren();
            if ("choice".equals(selXsp.getTerm().asModelGroup().getCompositor().toString())) //$NON-NLS-1$
                choice = true;

            try {
                // replace xsp is this a proper way?
                if (selectedExtendType != null && selectedExtendType.length() > 0)
                    xsp = SchemaWebAgent.getInstance().getReusableType(selectedExtendType).getXsParticle().getTerm()
                            .asModelGroup().getChildren();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }

            ArrayList<TreeNode> list = new ArrayList<TreeNode>();
            // iterate over children
            for (int j = 0; j < xsp.length; j++) {
                setChildrenWithKeyMask(id, language, foreignKey, docIndex, maskKey, choice, xsp[j], list, xpathToTreeNode,
                        bindpath);
            }

            // update dsp rules
            updateDspRules(docIndex, d, xpath.substring(1, xpath.indexOf("/", 1) > -1 ? xpath.indexOf("/", 1) : xpath.length())//$NON-NLS-1$//$NON-NLS-2$
                    .trim());

            if (xpathToTreeNode != null) {
                ctx.getSession().setAttribute("xpathToTreeNode", xpathToTreeNode); //$NON-NLS-1$
            }

            TreeNode[] rtnNodes = list.toArray(new TreeNode[list.size()]);

            handleDynamicLable(rtnNodes, docIndex);// FIXME: performance maybe a problem
            rtnNodes = handleDisplayRules(rtnNodes, docIndex);
            return rtnNodes;
        }
    }

    private void clearChildrenValue(Node node) {
        if (node.getFirstChild() != null && node.getFirstChild().getNodeType() == Node.TEXT_NODE) {
            node.getFirstChild().setNodeValue(""); //$NON-NLS-1$
        }
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            clearChildrenValue(list.item(i));
        }
    }

    // added by lzhang, update treenode value if it has DspRules
    public String updateNodeDspValue(int docIndex, int newId) {
        WebContext ctx = WebContextFactory.get();
        List<DisplayRule> dspRules = (List<DisplayRule>) WebContextFactory.get().getSession()
                .getAttribute("displayRules" + docIndex); //$NON-NLS-1$
        HashMap<Integer, String> idToXpath = (HashMap<Integer, String>) ctx.getSession().getAttribute("idToXpath" + docIndex); //$NON-NLS-1$
        String sourcePath = idToXpath.get(newId);
        for (DisplayRule displayRule : dspRules) {
            if (displayRule.getType().equals(BusinessConcept.APPINFO_X_DEFAULT_VALUE_RULE)) {
                String xpathInRule = displayRule.getXpath();
                if (XpathUtil.checkDefalutByXpath(sourcePath, xpathInRule))
                    return XmlUtil.escapeXml(displayRule.getValue());
            }
        }
        return null;
    }

    public String cloneNode(int siblingId, int newId, int docIndex, boolean ifdeep) throws Exception {

        WebContext ctx = WebContextFactory.get();
        HashMap<Integer, XSParticle> idToParticle = (HashMap<Integer, XSParticle>) ctx.getSession().getAttribute("idToParticle"); //$NON-NLS-1$
        HashMap<Integer, String> idToXpath = (HashMap<Integer, String>) ctx.getSession().getAttribute("idToXpath" + docIndex); //$NON-NLS-1$
        ArrayList<String> nodeAutorization = (ArrayList<String>) ctx.getSession().getAttribute("nodeAutorization"); //$NON-NLS-1$
        XSParticle xsp = idToParticle.get(siblingId);
        // associate the new id node to the particle of his sibling
        idToParticle.put(newId, xsp);
        Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
        Document doc = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex + "_backup"); //$NON-NLS-1$ //$NON-NLS-2$
        HashMap<String, TreeNode> xpathToTreeNode = (HashMap<String, TreeNode>) ctx.getSession().getAttribute("xpathToTreeNode"); //$NON-NLS-1$
        HashMap<String, String> xpathToPolymType = (HashMap<String, String>) ctx.getSession().getAttribute(
                "xpathToPolymType" + docIndex); //$NON-NLS-1$

        try {
            Node node = Util.getNodeList(d, idToXpath.get(siblingId)).item(0);
            String dspValue = null;
            if (node != null) {
                Node nodeClone = node.cloneNode(true);

                if (!ifdeep) {
                    clearChildrenValue(nodeClone);

                    // added by lzhang, DWR synchronize to update the DSPValue
                    dspValue = updateNodeDspValue(docIndex, siblingId);
                    if (dspValue != null && nodeClone.getFirstChild() != null)
                        nodeClone.getFirstChild().setNodeValue(dspValue);
                }

                // simulate an "insertAfter()" which actually doesn't exist
                insertAfter(nodeClone, node);
                ctx.getSession().setAttribute("itemDocument" + docIndex, node.getOwnerDocument()); //$NON-NLS-1$
            } else
                return "Error";//$NON-NLS-1$

            // added by lzhang, consistent with document nodes when edit status
            if (doc != null) {
                Node node2 = Util.getNodeList(doc, idToXpath.get(siblingId)).item(0);
                if (node2 != null) {
                    Node nodeClone2 = node2.cloneNode(true);
                    if (!ifdeep)
                        clearChildrenValue(nodeClone2);
                    insertAfter(nodeClone2, node2);
                }
            }

            String siblingXpath = XpathUtil.getFieldXpath(idToXpath.get(siblingId));

            int siblingIndex = getXpathIndex(idToXpath.get(siblingId));

            HashMap<String, UpdateReportItem> updatedPath;
            if (ctx.getSession().getAttribute("updatedPath" + docIndex) != null) { //$NON-NLS-1$
                updatedPath = (HashMap<String, UpdateReportItem>) ctx.getSession().getAttribute("updatedPath" + docIndex); //$NON-NLS-1$
            } else {
                updatedPath = new HashMap<String, UpdateReportItem>();
            }

            editXpathInidToXpathAdd(siblingId, idToXpath, updatedPath, xpathToPolymType);

            idToXpath.put(newId, siblingXpath + "[" + (siblingIndex + 1) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            ctx.getSession().setAttribute("idToXpath", idToXpath); //$NON-NLS-1$            
            ctx.getSession().setAttribute(
                    "cloneXpath" + docIndex, //$NON-NLS-1$
                    new String[] { idToXpath.get(siblingId),
                            siblingXpath + "[" + (siblingIndex + 1) + "]", String.valueOf(ifdeep) }); //$NON-NLS-1$ //$NON-NLS-2$

            XSParticle particle = idToParticle.get(newId);
            if (particle == null) {// add simple node
                String uritem = null;
                if (ifdeep)
                    uritem = node.getFirstChild().getNodeValue();
                else
                    uritem = dspValue != null ? dspValue : ""; //$NON-NLS-1$
                updatedPath.put(
                        siblingXpath + "[" + (siblingIndex + 1) + "]", new UpdateReportItem(idToXpath.get(newId), "", uritem)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
            } else if (particle.getTerm().isElementDecl()) {
                if (particle.getTerm().asElementDecl().getType().isSimpleType()) {
                    UpdateReportItem ri = new UpdateReportItem(idToXpath.get(newId), "", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    updatedPath.put(siblingXpath + "[" + (siblingIndex + 1) + "]", ri); //$NON-NLS-1$ //$NON-NLS-2$
                }
            } else {
                updatedPath.put(siblingXpath + "[" + (siblingIndex + 1) + "]", null); //$NON-NLS-1$ //$NON-NLS-2$
            }

            ctx.getSession().setAttribute("updatedPath" + docIndex, updatedPath); //$NON-NLS-1$

            nodeAutorization.add(siblingXpath + "[" + (siblingIndex + 1) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            ctx.getSession().setAttribute("nodeAutorization", nodeAutorization); //$NON-NLS-1$
            TreeNode siblingNode = xpathToTreeNode.get(siblingXpath + "[" + siblingIndex + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            TreeNode siblingNodeTmp = (TreeNode) siblingNode.clone();
            siblingNodeTmp.setNodeId(newId);
            xpathToTreeNode.put(siblingXpath + "[" + (siblingIndex + 1) + "]", siblingNodeTmp); //$NON-NLS-1$ //$NON-NLS-2$    

            if (siblingNode.isPolymiorphism()) {
                // update polym
                String nodeStr = xpathToPolymType.get(XpathUtil.unifyXPath(siblingXpath + "[" + siblingIndex + "]"));
                xpathToPolymType.put(XpathUtil.unifyXPath(siblingXpath + "[" + (siblingIndex + 1) + "]"),
                        nodeStr != null ? nodeStr : siblingNode.getTypeName());
            }
            return "Cloned";//$NON-NLS-1$

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return "Error";//$NON-NLS-1$
        }

    }

    public void updateKeyNodesToEmptyInItemDocument(int docIndex) {
        WebContext ctx = WebContextFactory.get();
        String[] keys = (String[]) ctx.getSession().getAttribute("foreignKeys"); //$NON-NLS-1$
        for (int i = 0; i < keys.length; i++) {
            try {
                Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
                String oldValue = Util.getFirstTextNode(d, keys[i]);
                if (oldValue == null)
                    Util.getNodeList(d, keys[i]).item(0).appendChild(d.createTextNode("")); //$NON-NLS-1$
                else
                    Util.getNodeList(d, keys[i]).item(0).getFirstChild().setNodeValue(""); //$NON-NLS-1$
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }

        }
    }

    public String validateItem(int docIndex) {
        try {
            WebContext ctx = WebContextFactory.get();
            Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
            String concept = d.getDocumentElement().getLocalName();
            String xmlCont = Util.nodeToString(d);
            Element root = (Element) Util.parse(xmlCont).getDocumentElement();
            Node node = root;
            Configuration config = Configuration.getInstance(true);
            String schema = Util.getPort().getDataModel(new WSGetDataModel(new WSDataModelPK(config.getModel()))).getXsdSchema();
            if (com.amalto.core.util.Util.getUUIDNodes(schema, concept).size() > 0) { // check uuid key exists
                String dataCluster = config.getCluster();
                node = com.amalto.core.util.Util.processUUID(root, schema, dataCluster, concept, true);
            }
            com.amalto.core.util.Util.validate((Element) node, schema);
        } catch (Exception e) {
            String prefix = "Unable to create/update the item " + ": ";
            String err = prefix + ": " + e.getLocalizedMessage(); //$NON-NLS-1$
            LOG.error(err, e);
            return err;
        }

        return ""; //$NON-NLS-1$

    }

    public boolean updateForeignKeyPolymMap(String xpath, String type, int docIndex) {
        WebContext ctx = WebContextFactory.get();
        HashMap<String, String> xpathToPolymFKType = (HashMap<String, String>) ctx.getSession().getAttribute(
                "xpathToPolymFKType" + docIndex); //$NON-NLS-1$
        xpathToPolymFKType.put(xpath, type);

        return true;
    }

    public String updateNode(int id, String content, int docIndex) {
        WebContext ctx = WebContextFactory.get();
        HashMap<Integer, String> idToXpath = (HashMap<Integer, String>) ctx.getSession().getAttribute("idToXpath" + docIndex); //$NON-NLS-1$
        String xpath = idToXpath.get(id);
        if (xpath == null)
            return "Nothing to update";//$NON-NLS-1$

        String updateStatus = updateNode2(xpath, content, docIndex);

        try {
            updateDspRules(docIndex, (Document) ctx.getSession().getAttribute("itemDocument" + docIndex),//$NON-NLS-1$
                    Util.getConceptFromPath(xpath));
        } catch (Exception e) {
            LOG.error("Error happened when update display rules!", e);//$NON-NLS-1$
        }

        return updateStatus;
    }

    /**
     * DOC HSHU Comment method "checkVisibilityRules".
     */
    public Map<String, String> checkVisibilityRules(int docIndex) {
        Map<String, String> checkMap = new HashMap<String, String>();
        List<DisplayRule> dspRules = (List<DisplayRule>) WebContextFactory.get().getSession()
                .getAttribute("displayRules" + docIndex); //$NON-NLS-1$

        if (dspRules != null) {
            for (DisplayRule displayRule : dspRules) {
                String xpath = displayRule.getXpath();
                if (displayRule.getType().equals(BusinessConcept.APPINFO_X_VISIBLE_RULE)) {
                    HashMap<String, TreeNode> xpathToTreeNode = (HashMap<String, TreeNode>) WebContextFactory.get().getSession()
                            .getAttribute("xpathToTreeNode");//$NON-NLS-1$
                    String nodeXpath;
                    for (Object key : xpathToTreeNode.keySet()) {
                        nodeXpath = key.toString();
                        if (XpathUtil.checkVisibleByXpath(nodeXpath, xpath)) {
                            int nodeId = xpathToTreeNode.get(key).getNodeId();
                            checkMap.put(String.valueOf(nodeId), displayRule.getValue());
                        }
                    }
                }
            }
        }
        return checkMap.isEmpty() ? null : checkMap;
    }

    public synchronized static String updateNode2(String xpath, String content, int docIndex) {
        WebContext ctx = WebContextFactory.get();
        Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$        
        try {
            String oldValue = Util.getFirstTextNode(d, xpath);
            if (content.equals(oldValue))
                return "Nothing to update";//$NON-NLS-1$

            if (oldValue == null) {
                Node checkNode = Util.getNodeList(d, xpath).item(0);
                if (checkNode != null) {
                    checkNode.appendChild(d.createTextNode(content));
                } else {
                    Util.createOrUpdateNode(xpath, content, d);
                }
            } else {
                Node curNode = Util.getNodeList(d, xpath).item(0);
                if (curNode != null)
                    curNode.getFirstChild().setNodeValue(content);
                else
                    Util.createOrUpdateNode(xpath, content, d);
            }
            // TODO add path to session
            HashMap<String, UpdateReportItem> updatedPath;
            if (ctx.getSession().getAttribute("updatedPath" + docIndex) != null) { //$NON-NLS-1$
                updatedPath = (HashMap<String, UpdateReportItem>) ctx.getSession().getAttribute("updatedPath" + docIndex); //$NON-NLS-1$
            } else {
                updatedPath = new HashMap<String, UpdateReportItem>();
            }
            if (updatedPath.get(xpath) != null) {
                oldValue = updatedPath.get(xpath).getOldValue();
            }
            UpdateReportItem item = new UpdateReportItem(xpath, oldValue, content);
            updatedPath.put(xpath, item);
            ctx.getSession().setAttribute("updatedPath" + docIndex, updatedPath); //$NON-NLS-1$
            // update the treeNode
            HashMap<String, TreeNode> xpathToTreeNode = (HashMap<String, TreeNode>) ctx.getSession().getAttribute(
                    "xpathToTreeNode");//$NON-NLS-1$
            TreeNode node = null;
            if (xpath != null)
                node = xpathToTreeNode.get(xpath);
            if (xpath.lastIndexOf("]") == xpath.length() - 1 && node == null) { //$NON-NLS-1$
                node = xpathToTreeNode.get(xpath.replaceAll("\\[\\d+\\]$", "[1]")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (node != null) {
                node.setValue(content);
                if (content == null || content.trim().length() == 0)
                    node.setDisplayDefalutValue(false);
            }
            return "Node updated"; //$NON-NLS-1$
        } catch (Exception e2) {
            LOG.error(e2.getMessage(), e2);
            return "Error"; //$NON-NLS-1$
        }
    }

    private static Document checkNode(String xpath, Document d) throws Exception {
        // try each element of the xpath and check if it exists in datamodel
        if (xpath.charAt(0) == '/') {
            xpath = xpath.substring(1);
        }
        String[] elements = xpath.split("/"); //$NON-NLS-1$
        String xpathParent = "/"; //$NON-NLS-1$
        for (int i = 0; i < elements.length; i++) {
            if (CommonDWR.getNodeList(d, xpathParent + "/" + elements[i]).getLength() == 0) { //$NON-NLS-1$
                d = createNode(xpathParent, elements[i], d);
            }
            if (i == 0)
                xpathParent = "/" + elements[i]; //$NON-NLS-1$
            else
                xpathParent += "/" + elements[i]; //$NON-NLS-1$
        }
        return d;
    }

    private static Document createNode(String xpathParent, String nodeToBeCreated, Document d) throws Exception {
        WebContext ctx = WebContextFactory.get();
        HashMap xpathToParticle = (HashMap) ctx.getSession().getAttribute("xpathToParticle"); //$NON-NLS-1$

        Element el = d.createElement(nodeToBeCreated);
        XSParticle[] xsp = null;

        xsp = ((XSParticle) xpathToParticle.get(xpathParent)).getTerm().asModelGroup().getChildren();

        String elementAfter;
        for (int i = 0; i < xsp.length; i++) {
            String element = xsp[i].getTerm().asElementDecl().getName();
            if (nodeToBeCreated.equals(element)) {
                if (i == xsp.length - 1) {
                    Node parent = Util.getNodeList(d, xpathParent).item(0);
                    parent.appendChild(el);
                    return d;
                }
                for (int j = 0; j < xsp.length - i - 1; j++) {
                    elementAfter = xpathParent + "/" + xsp[i + j + 1].getTerm().asElementDecl().getName(); //$NON-NLS-1$
                    Node node = Util.getNodeList(d, elementAfter).item(0);
                    if (node != null) {
                        node.getParentNode().insertBefore(el, node);
                        return d;
                    }
                }

                // TODO
                {
                    Node parent = Util.getNodeList(d, xpathParent).item(0);
                    parent.appendChild(el);
                }
            }
        }
        return d;
    }

    /**
     * add by ymli if ListItem with xpath like '/PurchaseOrder/ListItems/POItem[i] is deleted, the xpath in idToXpath
     * will be edited , eg. '/PurchaseOrder/ListItems/POItem[j]'(j>i), j--
     * 
     */
    public void editXpathInidToXpath(int id, HashMap<Integer, String> idToXpath, HashMap<String, UpdateReportItem> updatedPath,
            HashMap<String, String> xpathToPolymType) {
        String nodeXpath = idToXpath.get(id).replaceAll("\\[\\d+\\]$", ""); //$NON-NLS-1$ //$NON-NLS-2$
        String patternXpath = nodeXpath.replaceAll("\\[", "\\\\["); //$NON-NLS-1$ //$NON-NLS-2$
        patternXpath = patternXpath.replaceAll("\\]", "\\\\]"); //$NON-NLS-1$ //$NON-NLS-2$

        Pattern p = Pattern.compile("(.*?)(\\[)(\\d+)(\\](.*?))"); //$NON-NLS-1$
        Matcher m = p.matcher(idToXpath.get(id));
        int nodeIndex = -1;
        if (m.matches())
            nodeIndex = Integer.parseInt(m.group(3));
        Iterator<Integer> keys = idToXpath.keySet().iterator();
        while (keys.hasNext()) {
            int key = keys.next();
            String xpath = idToXpath.get(key);
            String xpath1 = xpath;
            String unifiedXpath = null;
            if (xpath.matches(patternXpath + "\\[\\d+\\].*")) { //$NON-NLS-1$
                int pathIndex = -1;
                Matcher m1 = p.matcher(xpath);
                if (m1.matches())
                    pathIndex = Integer.parseInt(m1.group(3));
                if (nodeIndex < pathIndex) {
                    pathIndex--;
                    xpath = nodeXpath + "[" + pathIndex + "]" + m1.group(m1.groupCount()); //$NON-NLS-1$ //$NON-NLS-2$
                    idToXpath.put(key, xpath);
                    // added by lzhang
                    unifiedXpath = XpathUtil.unifyXPath(xpath1);
                    if (xpathToPolymType.containsKey(unifiedXpath)) {
                        xpathToPolymType.put(XpathUtil.unifyXPath(xpath), xpathToPolymType.get(unifiedXpath));
                        xpathToPolymType.remove(unifiedXpath);
                    }

                    if (updatedPath.get(xpath1) != null) {
                        UpdateReportItem uri = updatedPath.get(xpath1);
                        updatedPath.remove(xpath1);
                        updatedPath.put(xpath, uri);
                    }
                }

            }
        }
    }

    /**
     * add by ymli if ListItem with xpath like '/PurchaseOrder/ListItems/POItem[i] is add, the xpath in idToXpath will
     * be edited , eg. '/PurchaseOrder/ListItems/POItem[j]'(j>i), j++
     * 
     */
    public void editXpathInidToXpathAdd(int id, HashMap<Integer, String> idToXpath,
            HashMap<String, UpdateReportItem> updatedPath, HashMap<String, String> xpathToPolymType) {
        int beginIndex = idToXpath.get(id).lastIndexOf("["); //$NON-NLS-1$
        int endIndex = idToXpath.get(id).lastIndexOf("]"); //$NON-NLS-1$

        String patternXpath = idToXpath.get(id).substring(0, beginIndex);
        int nodeIndex = Integer.parseInt(idToXpath.get(id).substring(beginIndex + 1, endIndex));

        Iterator<Integer> keys = idToXpath.keySet().iterator();
        while (keys.hasNext()) {
            int key = keys.next();
            String xpath = idToXpath.get(key);
            String xpath1 = xpath;
            String unifiedXpath = null;
            int xpathIndex = xpath.indexOf(patternXpath);
            if (xpathIndex >= 0) {
                int pathIndex = -1;
                String lastSubString = xpath.substring(xpathIndex + patternXpath.length());
                int beginIndex1 = lastSubString.indexOf("["); //$NON-NLS-1$
                int endIndex1 = lastSubString.indexOf("]"); //$NON-NLS-1$
                if (beginIndex1 < endIndex1 && beginIndex1 != -1 && endIndex1 != -1) {
                    pathIndex = Integer.parseInt(lastSubString.substring(beginIndex1 + 1, endIndex1));
                    String lastString = lastSubString.substring(endIndex1 + 1);
                    if (nodeIndex < pathIndex) {
                        pathIndex++;
                        xpath = patternXpath + "[" + pathIndex + "]" + lastString; //$NON-NLS-1$ //$NON-NLS-2$
                        idToXpath.put(key, xpath);
                        // added by lzhang
                        unifiedXpath = XpathUtil.unifyXPath(xpath1);
                        if (xpathToPolymType.containsKey(unifiedXpath)) {
                            xpathToPolymType.put(XpathUtil.unifyXPath(xpath), xpathToPolymType.get(unifiedXpath));
                            xpathToPolymType.remove(unifiedXpath);
                        }

                        if (updatedPath.get(xpath1) != null) {
                            UpdateReportItem uri = updatedPath.get(xpath1);
                            updatedPath.remove(xpath1);
                            updatedPath.put(xpath, uri);
                        }

                    }
                }

            }

        }
    }

    /**
     * add by ymli if ListItem with xpath like '/PurchaseOrder/ListItems/POItem[i] is deleted, the xpath in idToXpath
     * will be edited , eg. '/PurchaseOrder/ListItems/POItem[j]'(j>i), j--
     * 
     */
    public void editUpdatedPath(HashMap<String, UpdateReportItem> updatedPath, String xpath) {
        String subXpath = xpath.replaceAll("\\[\\d+\\]$", ""); //$NON-NLS-1$ //$NON-NLS-2$

        int b = xpath.indexOf("[") + 1; //$NON-NLS-1$
        int e = xpath.indexOf("]"); //$NON-NLS-1$
        int nodeIndex = Integer.parseInt((String) xpath.subSequence(b, e));
        Iterator<String> keys = updatedPath.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.matches(subXpath + "\\[\\d+\\]$")) { //$NON-NLS-1$
                int star = key.indexOf("[") + 1; //$NON-NLS-1$
                int end = key.indexOf("]"); //$NON-NLS-1$
                if (star < end && star != -1) {
                    int pathIndex = Integer.parseInt((String) key.subSequence(star, end));
                    if (nodeIndex < pathIndex) {
                        UpdateReportItem report = updatedPath.get(key);
                        keys.remove();
                        updatedPath.remove(key);
                        pathIndex--;
                        xpath = subXpath + "[" + pathIndex + "]"; //$NON-NLS-1$ //$NON-NLS-2$
                        updatedPath.put(xpath, report);
                    }
                }
            }
        }
    }

    /**
     * count the number of path which is >= index
     * 
     * @author ymli
     * @param updatedPath
     * @param index
     * @return
     */
    private int getCountOfsmaller(HashMap<String, UpdateReportItem> updatedPath, int index) {
        int count = 0;
        Set<String> keys = updatedPath.keySet();
        for (Iterator<String> it = keys.iterator(); it.hasNext();) {
            Pattern p = Pattern.compile("(.*?)(\\[)(\\d+)(\\]$)"); //$NON-NLS-1$
            Matcher m = p.matcher(it.next());
            if (m.matches()) {
                int pathIndex = -1;
                pathIndex = Integer.parseInt(m.group(3));
                if (pathIndex >= index)
                    count++;
            }
        }
        return count;
    }

    private int getXpathIndex(String xpath) {
        int pathIndex = -1;
        Pattern p = Pattern.compile("(.*?)(\\[)(\\d+)(\\]$)"); //$NON-NLS-1$
        Matcher m = p.matcher(xpath);
        if (m.matches()) {
            pathIndex = Integer.parseInt(m.group(3));
            return pathIndex;
        }
        return pathIndex;
    }

    public String removeNode(int id, int docIndex, String oldValue) {
        WebContext ctx = WebContextFactory.get();
        HashMap<Integer, String> idToXpath = (HashMap<Integer, String>) ctx.getSession().getAttribute("idToXpath" + docIndex); //$NON-NLS-1$
        Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
        Document doc = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex + "_backup"); //$NON-NLS-1$ //$NON-NLS-2$
        HashMap<Integer, XSParticle> idToParticle = (HashMap<Integer, XSParticle>) ctx.getSession().getAttribute("idToParticle"); //$NON-NLS-1$
        HashMap<String, String> xpathToPolymType = (HashMap<String, String>) ctx.getSession().getAttribute(
                "xpathToPolymType" + docIndex); //$NON-NLS-1$

        try {

            Util.getNodeList(d, idToXpath.get(id)).item(0).getParentNode()
                    .removeChild(Util.getNodeList(d, idToXpath.get(id)).item(0));

            if (doc != null) {
                Node node2 = Util.getNodeList(doc, idToXpath.get(id)).item(0);
                if (node2 != null)
                    node2.getParentNode().removeChild(node2);
            }

            HashMap<String, UpdateReportItem> updatedPath;
            if (ctx.getSession().getAttribute("updatedPath" + docIndex) != null) { //$NON-NLS-1$
                updatedPath = (HashMap<String, UpdateReportItem>) ctx.getSession().getAttribute("updatedPath" + docIndex); //$NON-NLS-1$
            } else {
                updatedPath = new HashMap<String, UpdateReportItem>();
            }
            UpdateReportItem ri = new UpdateReportItem(idToXpath.get(id), oldValue, ""); //$NON-NLS-1$      
            // add by ymli. fix the bug:0010576. edit the path
            String path = idToXpath.get(id);
            // modified by lzhang, be consistent with xpathToPolymType
            boolean ifPolym = false;
            if (xpathToPolymType.containsKey(XpathUtil.unifyXPath(path))) {
                xpathToPolymType.remove(XpathUtil.unifyXPath(path));
                ifPolym = true;
            }

            editXpathInidToXpath(id, idToXpath, updatedPath, xpathToPolymType);

            if (ifPolym)
                updateDspRules(docIndex, d, path.substring(1, path.indexOf("/", 1) > -1 ? path.indexOf("/", 1) : path.length())//$NON-NLS-1$//$NON-NLS-2$
                        .trim());

            if (updatedPath.get(idToXpath.get(id)) != null) {
                path = updatedPath.get(idToXpath.get(id)).getPath();
                Pattern p = Pattern.compile("(.*?)(\\[)(\\d+)(\\]$)"); //$NON-NLS-1$
                Matcher m = p.matcher(path);
                if (m.matches()) {
                    String nodeXpath = m.group(1);
                    int pathIndex = -1;
                    pathIndex = Integer.parseInt(m.group(3));
                    pathIndex += getCountOfsmaller(updatedPath, pathIndex);
                    path = nodeXpath + "[" + pathIndex + "]"; //$NON-NLS-1$ //$NON-NLS-2$
                    ri.setPath(path);
                }
            }
            XSParticle particle = idToParticle.get(id);
            if (particle == null)// remove simple node
                updatedPath.put(path, ri);
            else if (particle.getTerm().isElementDecl()) {
                if (particle.getTerm().asElementDecl().getType().isSimpleType()) {
                    updatedPath.put(path, ri);
                }
            } else
                updatedPath.put(path, null);

            idToXpath.remove(id);
            ctx.getSession().setAttribute("idToXpath" + docIndex, idToXpath); //$NON-NLS-1$
            ctx.getSession().setAttribute("updatedPath" + docIndex, updatedPath); //$NON-NLS-1$
            ctx.getSession().setAttribute("itemDocument" + docIndex, d); //$NON-NLS-1$
            return "Deleted";//$NON-NLS-1$
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return "Error";//$NON-NLS-1$
        }
    }

    public boolean isDataClusterExists(String dataClusterPK) throws Exception {
        WSExistsDataCluster wsExistsDataCluster = new WSExistsDataCluster();
        wsExistsDataCluster.setWsDataClusterPK(new WSDataClusterPK(dataClusterPK));
        WSBoolean wsBoolean = Util.getPort().existsDataCluster(wsExistsDataCluster);
        return wsBoolean.is_true();
    }

    public static boolean isItemModifiedByOther(boolean newItem, int docIndex) throws Exception {
        WebContext ctx = WebContextFactory.get();

        WSItem wsitem = (WSItem) ctx.getSession().getAttribute("itemDocument" + docIndex + "_wsItem"); //$NON-NLS-1$ //$NON-NLS-2$
        if (wsitem != null) {
            ItemPOJOPK itempk = new ItemPOJOPK(new DataClusterPOJOPK(wsitem.getWsDataClusterPK().getPk()),
                    wsitem.getConceptName(), wsitem.getIds());
            boolean isModified = com.amalto.core.util.Util.getItemCtrl2Local().isItemModifiedByOther(itempk,
                    wsitem.getInsertionTime());
            return isModified;
        }
        return false;
    }

    private static int getNodePositon(Node node) {
        if (node.getParentNode() == null)
            return 1;

        NodeList children = node.getParentNode().getChildNodes();
        int id = 0;
        for (int i = 0; i < children.getLength(); i++) {
            Node nodeToSpec = children.item(i);
            if (nodeToSpec instanceof Element && nodeToSpec.getNodeName().equals(node.getNodeName())) {
                id++;
                if (nodeToSpec.isSameNode(node))
                    return id;
            }
        }
        return -1;
    }

    public static ItemResult saveItem(String[] ids, String concept, boolean newItem, int docIndex, String language)
            throws Exception {
        WebContext ctx = WebContextFactory.get();
        Locale locale = new Locale(language);
        WSItemPK wsi = null;
        try {
            Configuration config = Configuration.getInstance();
            String dataModelPK = config.getModel();
            String dataClusterPK = config.getCluster();
            if (dataModelPK == null || dataModelPK.trim().length() == 0)
                throw new Exception("Data Model can't be empty!"); //$NON-NLS-1$
            if (dataClusterPK == null || dataClusterPK.trim().length() == 0)
                throw new Exception("Data Container can't be empty!"); //$NON-NLS-1$
            Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
            Document bk = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex + "_backup"); //$NON-NLS-1$ //$NON-NLS-2$

            reFillXsdByXML(d, dataModelPK, concept, docIndex);

            // filter item xml
            HashMap<String, String> xpathToPolymType = (HashMap<String, String>) ctx.getSession().getAttribute(
                    "xpathToPolymType" + docIndex); //$NON-NLS-1$

            if (bk != null) {
                // check if Schema instance namespace is bound before looking for xsi:type
                if (bk.getDocumentElement().getAttributeNS("http://www.w3.org/2000/xmlns/", "xsi").length() != 0) { //$NON-NLS-1$ //$NON-NLS-2$)
                    for (String xpath : new ArrayList<String>(xpathToPolymType.keySet())) {
                        String modifiedXpath = XpathUtil.unifyXPath(xpath);
                        if (!modifiedXpath.equals(xpath)) {
                            String value = xpathToPolymType.get(xpath);
                            xpathToPolymType.remove(xpath);
                            xpathToPolymType.put(modifiedXpath, value);
                        }
                    }
                    NodeList list = Util.getNodeList(bk, "//*[@xsi:type]"); //$NON-NLS-1$
                    for (int i = 0; i < list.getLength(); i++) {
                        Node nodeWithType = list.item(i);
                        Node parentNode = nodeWithType.getParentNode();
                        String type = nodeWithType.getAttributes().getNamedItem("xsi:type").getNodeValue();//$NON-NLS-1$
                        String trace = '/' + nodeWithType.getNodeName();
                        int pos = getNodePositon(nodeWithType);
                        if (pos != -1)
                            trace += "[" + pos + "]";//$NON-NLS-1$//$NON-NLS-2$
                        while (parentNode != null) {
                            pos = getNodePositon(parentNode);
                            String subTrace = '/' + parentNode.getNodeName();
                            if (pos != -1)
                                subTrace += "[" + pos + "]";//$NON-NLS-1$//$NON-NLS-2$
                            trace = subTrace + trace;
                            parentNode = parentNode.getParentNode();
                            if (parentNode instanceof Document) {
                                parentNode = parentNode.getParentNode();
                            }
                        }
                        if (!xpathToPolymType.containsKey(trace)) {
                            xpathToPolymType.put(trace, type);
                        }
                    }
                }
            }

            if (xpathToPolymType != null && xpathToPolymType.size() > 0) {
                d.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", //$NON-NLS-1$ //$NON-NLS-2$
                        "http://www.w3.org/2001/XMLSchema-instance"); //$NON-NLS-1$
                for (Iterator<String> iterator = xpathToPolymType.keySet().iterator(); iterator.hasNext();) {
                    String xpath = (String) iterator.next();
                    if (Util.getNodeList(d, xpath).getLength() > 0)
                        ((Element) Util.getNodeList(d, xpath).item(0)).setAttribute("xsi:type", xpathToPolymType.get(xpath)); //$NON-NLS-1$
                }
            }
            HashMap<String, String> xpathToPolymFKType = (HashMap<String, String>) ctx.getSession().getAttribute(
                    "xpathToPolymFKType" + docIndex); //$NON-NLS-1$
            if (xpathToPolymFKType != null && xpathToPolymFKType.size() > 0) {
                d.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tmdm", "http://www.talend.com/mdm"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                for (Iterator<String> iterator = xpathToPolymFKType.keySet().iterator(); iterator.hasNext();) {
                    String xpath = (String) iterator.next();
                    ((Element) Util.getNodeList(d, xpath).item(0)).setAttribute("tmdm:type", xpathToPolymFKType.get(xpath)); //$NON-NLS-1$
                }
            }

            String xml = CommonDWR.getXMLStringFromDocument(d);
            xml = xml.replaceAll("<\\?xml.*?\\?>", ""); //$NON-NLS-1$ //$NON-NLS-2$
            xml = xml.replaceAll("\\(Auto\\)", ""); //$NON-NLS-1$ //$NON-NLS-2$

            if (LOG.isDebugEnabled())
                LOG.debug("saveItem() " + xml); //$NON-NLS-1$

            ctx.getSession().setAttribute("viewNameItems", null); //$NON-NLS-1$
            String operationType;
            if (newItem == true)
                operationType = "CREATE"; //$NON-NLS-1$
            else
                operationType = "UPDATE"; //$NON-NLS-1$

            // check updatedPath
            HashMap<String, UpdateReportItem> updatedPath = new HashMap<String, UpdateReportItem>();
            updatedPath = (HashMap<String, UpdateReportItem>) ctx.getSession().getAttribute("updatedPath" + docIndex); //$NON-NLS-1$
            boolean hasAutoChangeField = false;
            if (ctx.getSession().getAttribute("hasAutoChangeField" + docIndex) != null //$NON-NLS-1$
                    && ctx.getSession().getAttribute("hasAutoChangeField" + docIndex).equals(new Boolean(true))) { //$NON-NLS-1$
                hasAutoChangeField = true;
            }
            if (!"DELETE".equals(operationType) && (updatedPath == null || updatedPath.isEmpty())) { //$NON-NLS-1$
                if (!hasAutoChangeField) {
                    return new ItemResult(ItemResult.UNCHANGED);
                }
            }

            // put item
            boolean isUpdateThisItem = true;
            if (newItem == true)
                isUpdateThisItem = false;
            // if update, check the item is modified by others?

            WSPutItemWithReport wsPutItemWithReport = new WSPutItemWithReport(new WSPutItem(new WSDataClusterPK(dataClusterPK),
                    xml, new WSDataModelPK(dataModelPK), isUpdateThisItem), "genericUI", true); //$NON-NLS-1$
            wsi = Util.getPort().putItemWithReport(wsPutItemWithReport);
            synchronizeUpdateState(ctx, docIndex);
            if (wsi != null) {
                // put update report
                ctx.getSession().setAttribute("treeIdxToIDS" + docIndex, wsi.getIds()); //$NON-NLS-1$
            }

            String message = null;
            int status;
            if (Util.isTransformerExist("beforeSaving_" + concept)) { //$NON-NLS-1$

                String outputErrorMessage = wsPutItemWithReport.getSource();

                String errorCode = null;
                if (outputErrorMessage != null) {
                    Document doc = Util.parse(outputErrorMessage);
                    // TODO what if multiple error nodes ?
                    String xpath = "//report/message"; //$NON-NLS-1$
                    Node errorNode = XPathAPI.selectSingleNode(doc, xpath);
                    if (errorNode instanceof Element) {
                        Element errorElement = (Element) errorNode;
                        errorCode = errorElement.getAttribute("type"); //$NON-NLS-1$
                        Node child = errorElement.getFirstChild();
                        if (child instanceof Text)
                            message = ((Text) child).getTextContent();
                    }
                }

                if ("info".equals(errorCode)) { //$NON-NLS-1$
                    if (message == null || message.length() == 0)
                        message = MESSAGES.getMessage(locale, "save.process.validation.success"); //$NON-NLS-1$
                    status = ItemResult.SUCCESS;
                } else {
                    // Anything but 0 is unsuccessful
                    if (message == null || message.length() == 0)
                        message = MESSAGES.getMessage(locale, "save.process.validation.failure"); //$NON-NLS-1$
                    status = ItemResult.FAILURE;
                }
            } else {
                message = MESSAGES.getMessage(locale, "save.success"); //$NON-NLS-1$
                status = ItemResult.SUCCESS;
            }

            return new ItemResult(status, message, Util.joinStrings(wsi == null ? ids : wsi.getIds(), ".")); //$NON-NLS-1$

        } catch (Exception e) {
            ItemResult result;
            if (Util.causeIs(e, RoutingException.class)) {
                String saveSUCCE = MESSAGES.getMessage(locale, "save.success.but.exist.exception", //$NON-NLS-1$
                        concept + "." + Util.joinStrings(ids, "."), e.getLocalizedMessage()); //$NON-NLS-1$//$NON-NLS-2$
                result = new ItemResult(ItemResult.FAILURE, saveSUCCE);
            } else if (Util.causeIs(e, CVCException.class)) {
                String err = MESSAGES.getMessage(locale, "save.fail.cvc.exception", concept); //$NON-NLS-1$
                result = new ItemResult(ItemResult.FAILURE, err);
            } else {
                String err = MESSAGES.getMessage(locale, "save.fail", concept + "." + Util.joinStrings(ids, ".")); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
                result = new ItemResult(ItemResult.FAILURE, err);
            }
            return result;
        }
    }

    private static Document filledByDspValue(String dataModelPK, String concept, Document d, int docIndex) throws Exception {
        WebContext ctx = WebContextFactory.get();
        String realSchemaStyle = null;

        HashMap<String, String> xpathToPolymType = (HashMap<String, String>) ctx.getSession().getAttribute(
                "xpathToPolymType" + docIndex); //$NON-NLS-1$
        if (xpathToPolymType != null && xpathToPolymType.size() > 0) {
            List polymList = new ArrayList();
            for (Iterator<String> iterator = xpathToPolymType.keySet().iterator(); iterator.hasNext();) {
                String subType = xpathToPolymType.get(iterator.next());
                polymList.add(subType);
            }
            HashMap<List, DisplayRulesUtil> polymToDisplayRulesUtil = (HashMap<List, DisplayRulesUtil>) ctx.getSession()
                    .getAttribute("polymToDisplayRulesUtil" + docIndex);//$NON-NLS-1$

            DisplayRulesUtil displayRulesUtil = polymToDisplayRulesUtil.get(polymList);
            if (displayRulesUtil != null)
                realSchemaStyle = displayRulesUtil.genDefaultValueStyle(XmlUtil.parseDocument(d));
        }
        if (realSchemaStyle == null) {
            DisplayRulesUtil displayRulesUtil = (DisplayRulesUtil) ctx.getSession().getAttribute(
                    "itemDocument_displayRulesUtil" + docIndex); //$NON-NLS-1$
            realSchemaStyle = displayRulesUtil.genDefaultValueStyle(XmlUtil.parseDocument(d));
        }

        // added by lzhang, make sure there is no empty node which has DSP value
        org.dom4j.Document transformedDocument = XmlUtil.styleDocument(d, realSchemaStyle);

        return Util.parse(transformedDocument.asXML());
    }

    public String deleteItem(String concept, String[] ids, int docIndex) {
        WebContext ctx = WebContextFactory.get();
        try {
            Configuration config = Configuration.getInstance();
            String dataClusterPK = config.getCluster();
            String outputErrorMessage = com.amalto.core.util.Util.beforeDeleting(dataClusterPK, concept, ids);

            String message = null;
            String errorCode = null;
            if (outputErrorMessage != null) {
                Document doc = Util.parse(outputErrorMessage);
                // TODO what if multiple error nodes ?
                String xpath = "//report/message"; //$NON-NLS-1$
                Node errorNode = XPathAPI.selectSingleNode(doc, xpath);
                if (errorNode instanceof Element) {
                    Element errorElement = (Element) errorNode;
                    errorCode = errorElement.getAttribute("type"); //$NON-NLS-1$
                    Node child = errorElement.getFirstChild();
                    if (child instanceof Text)
                        message = ((Text) child).getTextContent();
                }
            }

            if (outputErrorMessage == null || "info".equals(errorCode)) { //$NON-NLS-1$
                TreeNode rootNode = getRootNode(concept, "en"); //$NON-NLS-1$
                if (ids != null && !rootNode.isReadOnly()) {
                    WSItemPK wsItem = Util.getPort().deleteItem(
                            new WSDeleteItem(new WSItemPK(new WSDataClusterPK(dataClusterPK), concept, ids), false));
                    if (wsItem != null)
                        pushUpdateReport(ids, concept, "PHYSICAL_DELETE", docIndex); //$NON-NLS-1$ // If docIndex is -1, it means the item is
                    // deleted from the list.
                    else
                        message = "ERROR - Unable to delete item";
                    ctx.getSession().setAttribute("viewNameItems", null); //$NON-NLS-1$

                    if (outputErrorMessage == null)
                        message = message == null ? "" : message; //$NON-NLS-1$
                    else if (message == null || message.length() == 0)
                        message = MESSAGES.getMessage("delete.process.validation.success"); //$NON-NLS-1$
                } else {
                    if (outputErrorMessage == null)
                        message = message == null ? "" : message; //$NON-NLS-1$
                    else if (message == null || message.length() == 0)
                        message = MESSAGES.getMessage("delete.process.validation.success"); //$NON-NLS-1$
                    return message + " - No update report was produced";
                }
            } else {
                // Anything but 0 is unsuccessful
                if (message == null || message.length() == 0)
                    message = MESSAGES.getMessage("delete.process.validation.failure"); //$NON-NLS-1$
                message = "ERROR_3" + message; //$NON-NLS-1$
            }
            return message;

        } catch (Exception e) {
            return "ERROR -" + e.getLocalizedMessage(); //$NON-NLS-1$
        }
    }

    public String[] getUriArray(String concept, String[] ids) {
        Configuration config;
        List<String> uriList = new ArrayList<String>();
        try {
            config = Configuration.getInstance();
            String dataClusterPK = config.getCluster();
            String content;
            WSItemPK wsItem = new WSItemPK(new WSDataClusterPK(dataClusterPK), concept, ids);
            content = Util.getPort().getItem(new WSGetItem(wsItem)).getContent();

            for (Iterator iterator = parsXMLString(content).getRootElement().nodeIterator(); iterator.hasNext();) {
                org.jboss.dom4j.Node node = (org.jboss.dom4j.Node) iterator.next();
                if (node.getStringValue().startsWith("/imageserver")) { //$NON-NLS-1$
                    uriList.add(node.getStringValue());
                }
            }
            if (LOG.isDebugEnabled())
                LOG.debug(uriList.toArray());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        String[] uriArray = new String[uriList.size()];
        for (int i = 0; i < uriList.size(); i++) {
            uriArray[i] = uriList.get(i);
        }
        return uriArray;
    }

    public String logicalDeleteItem(String concept, String[] ids, String path, int docIndex) {
        WebContext ctx = WebContextFactory.get();
        try {
            Configuration config = Configuration.getInstance();
            String dataClusterPK = config.getCluster();
            TreeNode rootNode = getRootNode(concept, "en"); //$NON-NLS-1$
            if (ids != null && !rootNode.isReadOnly()) {
                Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$
                String xml = null;
                if (d == null) {// get item from db
                    WSItem item = Util.getPort().getItem(
                            new WSGetItem(new WSItemPK(new WSDataClusterPK(dataClusterPK), concept, ids)));
                    xml = item.getContent();
                } else {
                    xml = CommonDWR.getXMLStringFromDocument(d);
                }
                WSDroppedItemPK wsItem = Util.getPort().dropItem(
                        new WSDropItem(new WSItemPK(new WSDataClusterPK(dataClusterPK), concept, ids), path, false));
                if (wsItem != null && xml != null)
                    if ("/".equalsIgnoreCase(path)) { //$NON-NLS-1$
                        pushUpdateReport(ids, concept, "LOGIC_DELETE", docIndex); //$NON-NLS-1$
                    } else {// part delete consider as 'UPDATE'
                        xml = xml.replaceAll("<\\?xml.*?\\?>", ""); //$NON-NLS-1$ //$NON-NLS-2$
                        String xpath = path.replaceAll("/" + concept, ""); //$NON-NLS-1$ //$NON-NLS-2$
                        JXPathContext jxpContext = JXPathContext.newContext(Util.parse(xml).getDocumentElement());
                        Object oldValue = jxpContext.getValue(xpath);
                        if (oldValue != null && !oldValue.equals("")) { //$NON-NLS-1$
                            UpdateReportItem item = new UpdateReportItem(path, oldValue.toString(), ""); //$NON-NLS-1$
                            HashMap<String, UpdateReportItem> updatedPath = (HashMap<String, UpdateReportItem>) ctx.getSession()
                                    .getAttribute("updatedPath" + docIndex); //$NON-NLS-1$
                            if (updatedPath == null)
                                updatedPath = new HashMap<String, UpdateReportItem>();
                            updatedPath.put(path, item);
                            ctx.getSession().setAttribute("updatedPath" + docIndex, updatedPath); //$NON-NLS-1$
                            pushUpdateReport(ids, concept, "UPDATE", docIndex); //$NON-NLS-1$
                        }
                    }
                else
                    return "ERROR - dropItem is NULL";//$NON-NLS-1$
                ctx.getSession().setAttribute("viewNameItems", null); //$NON-NLS-1$
                return "OK";//$NON-NLS-1$
            } else {
                return "OK - But no update report";//$NON-NLS-1$
            }
        } catch (Exception e) {
            return "ERROR -" + e.getLocalizedMessage();
        }
    }

    /**
     * create an "empty" item from scratch, set every text node to empty
     * 
     * @param viewPK
     * @throws RemoteException
     * @throws Exception
     */
    private void createItem(String concept, int docIndex) throws RemoteException, Exception {
        WebContext ctx = WebContextFactory.get();
        Configuration config = Configuration.getInstance();
        String xml1 = "<" + concept + "></" + concept + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        Document d = Util.parse(xml1);
        ctx.getSession().setAttribute("itemDocument" + docIndex, d); //$NON-NLS-1$
        Map<String, XSElementDecl> map = CommonDWR.getConceptMap(config.getModel());
        XSComplexType xsct = (XSComplexType) (map.get(concept).getType());
        XSParticle[] xsp = xsct.getContentType().asParticle().getTerm().asModelGroup().getChildren();
        for (int j = 0; j < xsp.length; j++) {
            // why don't set up children element? FIXME

            setChildren(xsp[j], "/" + concept, docIndex); //$NON-NLS-1$
        }
    }

    private void setChildren(XSParticle xsp, String xpathParent, int docIndex) throws Exception {
        setChildrenWithValue(xsp, xpathParent, docIndex, false);
    }

    private void setChildrenWithValue(XSParticle xsp, String xpathParent, int docIndex, boolean withValue) throws Exception {
        // aiming added see 0009563
        if (xsp.getTerm().asModelGroup() != null) { // is complex type
            XSParticle[] xsps = xsp.getTerm().asModelGroup().getChildren();
            for (int i = 0; i < xsps.length; i++) {
                setChildrenWithValue(xsps[i], xpathParent, docIndex, withValue);
            }
        }
        if (xsp.getTerm().asElementDecl() == null)
            return;
        // end

        WebContext ctx = WebContextFactory.get();
        Document d = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex); //$NON-NLS-1$ _backup
        Document doc = (Document) ctx.getSession().getAttribute("itemDocument" + docIndex + "_backup"); //$NON-NLS-1$ //$NON-NLS-2$

        String[] cloneXpath = (String[]) ctx.getSession().getAttribute("cloneXpath" + docIndex); //$NON-NLS-1$ 
        String xPath = xpathParent + '/' + xsp.getTerm().asElementDecl().getName(); //$NON-NLS-1$ 
        Node parentNode = Util.getNodeList(d, xpathParent).item(0);
        if (doc != null) {
            NodeList nodes = Util.getNodeList(doc, xPath);
            if (nodes != null && nodes.getLength() > 0) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element el = d.createElement(xsp.getTerm().asElementDecl().getName());
                    Node node = nodes.item(i);
                    String textContent = node.getTextContent();
                    if (cloneXpath != null && cloneXpath[2].equals("true") && xPath.startsWith(cloneXpath[1]))// cloneXpath[0]
                    // is
                    // 'clone
                    // from'
                    // xpath,
                    // cloneXpath[1]
                    // is
                    // 'clone
                    // to'
                    // xpath
                    {
                        NodeList cloneList = Util.getNodeList(d, (xPath + "[" + (i + 1) + "]").replace(cloneXpath[1], //$NON-NLS-1$  //$NON-NLS-2$ 
                                cloneXpath[0]));
                        if (cloneList != null && cloneList.getLength() > 0)
                            textContent = cloneList.item(0).getTextContent();
                    }
                    if (xsp.getTerm().asElementDecl().getType().isSimpleType() && textContent != null && withValue)
                        el.setTextContent(textContent);

                    parentNode.appendChild(el);
                    if (xsp.getTerm().asElementDecl().getType().isComplexType()) {
                        XSParticle[] children = null;
                        Node typeNode = node.getAttributes().getNamedItem("xsi:type");//$NON-NLS-1$
                        if (typeNode != null) {
                            String xsiType = typeNode.getNodeValue();
                            if (xsiType != null) {
                                ReusableType resuType = SchemaWebAgent.getInstance().getReusableType(xsiType);
                                List<XSParticle> pt = resuType.getAllChildren(null);
                                children = pt.toArray(new XSParticle[] {});
                            }
                        } else {
                            XSComplexType type = (XSComplexType) xsp.getTerm().asElementDecl().getType();
                            children = type.getContentType().asParticle().getTerm().asModelGroup().getChildren();
                        }

                        for (XSParticle child : children) {
                            setChildrenWithValue(child, xPath + "[position()=" + (i + 1) + "]", docIndex, withValue); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    }
                }
            } else {
                setChildrenWithValueByClone(cloneXpath, xPath, d, xsp, parentNode, xpathParent, docIndex, withValue);
            }
        } else {
            setChildrenWithValueByClone(cloneXpath, xPath, d, xsp, parentNode, xpathParent, docIndex, withValue);
        }
    }

    private void setChildrenWithValueByClone(String[] cloneXpath, String xPath, Document d, XSParticle xsp, Node parentNode,
            String xpathParent, int docIndex, boolean withValue) throws Exception {
        if (cloneXpath != null && xPath.startsWith(cloneXpath[1])) {
            NodeList cloneList = Util.getNodeList(d, xPath.replace(cloneXpath[1], cloneXpath[0]));
            if (cloneList != null && cloneList.getLength() > 0)
                for (int i = 0; i < cloneList.getLength(); i++) {// multi-occur
                    Element el2 = d.createElement(xsp.getTerm().asElementDecl().getName());
                    if (cloneXpath[2].equals("true") && xsp.getTerm().asElementDecl().getType().isSimpleType() && withValue) //$NON-NLS-1$
                        el2.setTextContent(cloneList.item(i).getTextContent());
                    parentNode.appendChild(el2);

                    if (xsp.getTerm().asElementDecl().getType().isComplexType()) {
                        XSParticle[] children = null;
                        Node typeNode = cloneList.item(i).getAttributes().getNamedItem("xsi:type");//$NON-NLS-1$
                        if (typeNode != null) {
                            String xsiType = typeNode.getNodeValue();
                            if (xsiType != null) {
                                ReusableType resuType = SchemaWebAgent.getInstance().getReusableType(xsiType);
                                List<XSParticle> pt = resuType.getAllChildren(null);
                                children = pt.toArray(new XSParticle[] {});
                            }
                        } else {
                            XSComplexType type = (XSComplexType) xsp.getTerm().asElementDecl().getType();
                            children = type.getContentType().asParticle().getTerm().asModelGroup().getChildren();
                        }
                        for (XSParticle child : children) {
                            setChildrenWithValue(child, xPath + "[position()=" + (i + 1) + "]", docIndex, withValue); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    }
                }
        } else {
            Element el = d.createElement(xsp.getTerm().asElementDecl().getName());
            parentNode.appendChild(el);

            if (xsp.getTerm().asElementDecl().getType().isComplexType()) {
                XSComplexType type = (XSComplexType) xsp.getTerm().asElementDecl().getType();
                XSParticle particle = type.getContentType().asParticle();
                if (particle != null) {
                    XSParticle[] children = type.getContentType().asParticle().getTerm().asModelGroup().getChildren();
                    String xpth = xpathParent + '/' + xsp.getTerm().asElementDecl().getName(); //$NON-NLS-1$ 
                    for (XSParticle child : children) {
                        setChildrenWithValue(child, xpth, docIndex, withValue);
                    }
                }
            }
        }
    }

    public String countForeignKey(String xpathForeignKey) throws Exception {
        Configuration config = Configuration.getInstance();
        String conceptName = Util.getConceptFromPath(xpathForeignKey);
        return Util.getPort().count(new WSCount(new WSDataClusterPK(config.getCluster()), conceptName, null, -1)).getValue();
    }

    public String parseForeignKeyFilter(String dataObject, String fkFilter, int docIndex, int nodeId) throws Exception {

        String parsedFkfilter = fkFilter;

        // get xpath value map
        WebContext ctx = WebContextFactory.get();
        HashMap<String, TreeNode> xpathToTreeNode = (HashMap<String, TreeNode>) ctx.getSession().getAttribute("xpathToTreeNode");//$NON-NLS-1$
        HashMap<String, UpdateReportItem> updatedPath = (HashMap<String, UpdateReportItem>) ctx.getSession().getAttribute(
                "updatedPath" + docIndex);//$NON-NLS-1$
        HashMap<Integer, String> idToXpath = (HashMap<Integer, String>) ctx.getSession().getAttribute("idToXpath" + docIndex);//$NON-NLS-1$
        String currentXpath = null;
        if (idToXpath != null && idToXpath.get(nodeId) != null)
            currentXpath = idToXpath.get(nodeId);

        if (fkFilter != null) {

            if (Util.isCustomFilter(fkFilter)) {

                fkFilter = StringEscapeUtils.unescapeXml(fkFilter);
                parsedFkfilter = parseRightValueOrPath(xpathToTreeNode, updatedPath, dataObject, fkFilter, currentXpath);
                return parsedFkfilter;

            }

            // parse
            String[] criterias = fkFilter.split("#");//$NON-NLS-1$
            List conditions = new ArrayList<String>();
            for (String cria : criterias) {
                Map<String, String> conditionMap = new HashMap<String, String>();
                String[] values = cria.split("\\$\\$");//$NON-NLS-1$
                for (int i = 0; i < values.length; i++) {

                    switch (i) {
                    case 0:
                        conditionMap.put("Xpath", values[0]);//$NON-NLS-1$
                        break;
                    case 1:
                        conditionMap.put("Operator", values[1]);//$NON-NLS-1$
                        break;
                    case 2:
                        String rightValueOrPath = values[2];

                        rightValueOrPath = StringEscapeUtils.unescapeXml(rightValueOrPath);

                        rightValueOrPath = parseRightValueOrPath(xpathToTreeNode, updatedPath, dataObject, rightValueOrPath,
                                currentXpath);

                        conditionMap.put("Value", rightValueOrPath);//$NON-NLS-1$
                        break;
                    case 3:
                        conditionMap.put("Predicate", values[3]);//$NON-NLS-1$
                        break;
                    default:
                        break;
                    }
                }
                conditions.add(conditionMap);
            }
            // build
            if (conditions.size() > 0) {
                StringBuffer sb = new StringBuffer();
                for (Iterator iterator = conditions.iterator(); iterator.hasNext();) {
                    Map<String, String> conditionMap = (Map<String, String>) iterator.next();
                    if (conditionMap.size() > 0) {
                        String xpath = conditionMap.get("Xpath") == null ? "" : conditionMap.get("Xpath");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                        String operator = conditionMap.get("Operator") == null ? "" : conditionMap.get("Operator");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                        String value = conditionMap.get("Value") == null ? "" : conditionMap.get("Value");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                        String predicate = conditionMap.get("Predicate") == null ? "" : conditionMap.get("Predicate");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                        sb.append(xpath + "$$" + operator + "$$" + value + "$$" + predicate + "#");//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
                    }
                }
                if (sb.length() > 0)
                    parsedFkfilter = sb.toString();
            }

        }// end if

        return parsedFkfilter;

    }

    public String parseRightValueOrPath(HashMap<String, TreeNode> xpathToTreeNode, HashMap<String, UpdateReportItem> updatedPath,
            String dataObject, String rightValueOrPath, String currentXpath) {

        String origiRightValueOrPath = rightValueOrPath;
        String patternString = dataObject + "(/[A-Za-z0-9_\\[\\]]*)+";//$NON-NLS-1$
        Pattern pattern = Pattern.compile(patternString);// FIXME support simple xpath
        Matcher matcher = pattern.matcher(rightValueOrPath);
        while (matcher.find()) {
            for (int j = 0; j < matcher.groupCount(); j++) {
                String gettedXpath = matcher.group(j);

                if (gettedXpath != null) {

                    // check literal
                    int startPos = matcher.start(j);
                    int endPos = matcher.end(j);
                    if (startPos > 0 && endPos < origiRightValueOrPath.length() - 1) {
                        String checkValue = origiRightValueOrPath.substring(startPos - 1, endPos + 1).trim();
                        if (checkValue.startsWith("\"") && checkValue.endsWith("\""))//$NON-NLS-1$//$NON-NLS-2$
                            return rightValueOrPath;
                    }

                    // handle multi occurrences

                    // clean start char
                    if (currentXpath.startsWith("//"))//$NON-NLS-1$
                        currentXpath = currentXpath.substring(2);
                    else if (currentXpath.startsWith("/"))//$NON-NLS-1$
                        currentXpath = currentXpath.substring(1);

                    if (gettedXpath.startsWith("//"))//$NON-NLS-1$
                        gettedXpath = currentXpath.substring(2);
                    else if (gettedXpath.startsWith("/"))//$NON-NLS-1$
                        gettedXpath = currentXpath.substring(1);

                    if (currentXpath.matches(".*\\[(\\d+)\\].*") && !gettedXpath.matches(".*\\[(\\d+)\\].*")) {//$NON-NLS-1$//$NON-NLS-2$
                        // get ..
                        String currentXpathParent = currentXpath;
                        String gettedXpathParent = gettedXpath;
                        if (currentXpath.lastIndexOf("/") != -1)//$NON-NLS-1$
                            currentXpathParent = currentXpath.substring(0, currentXpath.lastIndexOf("/"));//$NON-NLS-1$
                        if (gettedXpath.lastIndexOf("/") != -1)//$NON-NLS-1$
                            gettedXpathParent = gettedXpath.substring(0, gettedXpath.lastIndexOf("/"));//$NON-NLS-1$
                        // clean
                        String currentXpathParentReplaced = currentXpathParent.replaceAll("\\[(\\d+)\\]", "");//$NON-NLS-1$//$NON-NLS-2$
                        // compare
                        if (currentXpathParentReplaced.equals(gettedXpathParent)) {
                            if (gettedXpath.lastIndexOf("/") != -1)//$NON-NLS-1$
                                gettedXpath = currentXpathParent + gettedXpath.substring(gettedXpath.lastIndexOf("/"));//$NON-NLS-1$
                        }
                    }

                    // get replaced value
                    String replacedValue = null;
                    boolean matchedAValue = false;

                    // How to handle multi-nodes?
                    if (updatedPath != null && updatedPath.get("/" + gettedXpath) != null) {//$NON-NLS-1$
                        UpdateReportItem updateReportItem = updatedPath.get("/" + gettedXpath);//$NON-NLS-1$
                        replacedValue = updateReportItem.getNewValue();
                        if (replacedValue != null)
                            matchedAValue = true;
                    }

                    if (!matchedAValue && xpathToTreeNode != null && xpathToTreeNode.get("/" + gettedXpath) != null) {//$NON-NLS-1$
                        replacedValue = xpathToTreeNode.get("/" + gettedXpath).getValue();//$NON-NLS-1$
                        if (replacedValue != null)
                            matchedAValue = true;
                    }

                    replacedValue = replacedValue == null ? "null" : replacedValue;//$NON-NLS-1$
                    if (matchedAValue)
                        replacedValue = "\"" + replacedValue + "\"";//$NON-NLS-1$//$NON-NLS-2$
                    rightValueOrPath = rightValueOrPath.replaceFirst(patternString, replacedValue);
                }

            }// end for
        }// end while

        return rightValueOrPath;
    }

    /**
     * lym
     */
    public String countForeignKey_filter(String dataObject, String xpathForeignKey, String xpathForeignKeyInfo, String fkFilter,
            int docIndex, int nodeId) throws Exception {
        return Util.countForeignKey_filter(xpathForeignKey, xpathForeignKeyInfo,
                parseForeignKeyFilter(dataObject, fkFilter, docIndex, nodeId));
    }

    public String getForeignKeyListWithCount(int start, int limit, String value, String dataObject, String xpathForeignKey,
            String xpathInfoForeignKey, String fkFilter, int docIndex, int nodeId) throws RemoteException, Exception {
        return Util.getForeignKeyList(start, limit, value, xpathForeignKey, xpathInfoForeignKey,
                parseForeignKeyFilter(dataObject, fkFilter, docIndex, nodeId), true);
    }

    /**
     * DOC HSHU Comment method "isPolymForeignKey".
     * 
     * @throws Exception
     */
    public boolean isPolymForeignKey(String xpathForeignKey) throws Exception {

        boolean isPolymForeignKey = false;

        if (xpathForeignKey != null && xpathForeignKey.length() > 0) {

            if (xpathForeignKey.startsWith("/"))//$NON-NLS-1$
                xpathForeignKey = xpathForeignKey.substring(1);
            String fkEntity = "";//$NON-NLS-1$
            if (xpathForeignKey.indexOf("/") != -1) {//$NON-NLS-1$
                fkEntity = xpathForeignKey.substring(0, xpathForeignKey.indexOf("/"));//$NON-NLS-1$
            } else {
                fkEntity = xpathForeignKey;
            }
            String fkEntityType = SchemaWebAgent.getInstance().getBusinessConcept(fkEntity).getCorrespondTypeName();
            List<ReusableType> subtypes = SchemaWebAgent.getInstance().getMySubtypes(fkEntityType, true);

            if (subtypes != null && subtypes.size() > 0)
                isPolymForeignKey = true;

        }

        return isPolymForeignKey;

    }

    /**
     * DOC HSHU Comment method "getForeignKeyPolymTypeList".
     * 
     * @param value
     * @param xpathForeignKey
     * @param docIndex
     * @param nodeId
     * @return
     * @throws Exception
     */
    public String getForeignKeyPolymTypeList(String value, String xpathForeignKey, int docIndex, int nodeId, String language)
            throws Exception {

        String fkEntityType = null;
        ReusableType entityReusableType = null;
        List<SubTypeBean> derivedTypes = new ArrayList<SubTypeBean>();

        if (xpathForeignKey != null && xpathForeignKey.length() > 0) {
            if (xpathForeignKey.startsWith("/"))//$NON-NLS-1$
                xpathForeignKey = xpathForeignKey.substring(1);
            String fkEntity = "";//$NON-NLS-1$
            if (xpathForeignKey.indexOf("/") != -1) {//$NON-NLS-1$
                fkEntity = xpathForeignKey.substring(0, xpathForeignKey.indexOf("/"));//$NON-NLS-1$
            } else {
                fkEntity = xpathForeignKey;
            }

            fkEntityType = SchemaWebAgent.getInstance().getBusinessConcept(fkEntity).getCorrespondTypeName();
            entityReusableType = SchemaWebAgent.getInstance().getReusableType(fkEntityType);
            entityReusableType.load();

            List<ReusableType> subtypes = SchemaWebAgent.getInstance().getMySubtypes(fkEntityType, true);
            for (ReusableType reusableType : subtypes) {
                reusableType.load();
                SubTypeBean subTypeBean = new SubTypeBean();
                subTypeBean.setName(reusableType.getName());
                subTypeBean.setLabel(reusableType.getLabelMap().get(language) == null ? reusableType.getName() : reusableType
                        .getLabelMap().get(language));
                subTypeBean.setOrderValue(reusableType.getOrderValue());
                if (reusableType.isAbstract()) {
                    continue;
                }
                derivedTypes.add(subTypeBean);
            }

        }

        Collections.sort(derivedTypes);

        JSONObject json = new JSONObject();
        int counter = 0;
        JSONArray rows = new JSONArray();
        json.put("rows", rows);//$NON-NLS-1$

        if (fkEntityType != null && !entityReusableType.isAbstract()) {
            JSONObject row = new JSONObject();
            row.put("value", entityReusableType.getName());//$NON-NLS-1$
            row.put("text", entityReusableType.getLabelMap().get(language) == null ? entityReusableType.getName()//$NON-NLS-1$
                    : entityReusableType.getLabelMap().get(language));
            rows.put(row);

            counter++;
        }

        for (SubTypeBean type : derivedTypes) {
            JSONObject row = new JSONObject();
            row.put("value", type.getName());//$NON-NLS-1$
            row.put("text", type.getLabel());//$NON-NLS-1$
            rows.put(row);

            counter++;
        }

        json.put("total", counter);//$NON-NLS-1$

        return json.toString();
    }

    /**
     * DOC HSHU Comment method "switchForeignKeyType".
     * 
     * @param targetEntity
     * @param xpathForeignKey
     * @param xpathInfoForeignKey
     * @param fkFilter
     * @return
     * @throws Exception
     */
    public ForeignKeyDrawer switchForeignKeyType(String targetEntityType, String xpathForeignKey, String xpathInfoForeignKey,
            String fkFilter) throws Exception {
        ForeignKeyDrawer fkDrawer = new ForeignKeyDrawer();

        BusinessConcept businessConcept = SchemaWebAgent.getInstance().getFirstBusinessConceptFromRootType(targetEntityType);
        if (businessConcept == null)
            return null;
        String targetEntity = businessConcept.getName();

        if (xpathForeignKey != null && xpathForeignKey.length() > 0) {
            xpathForeignKey = replaceXpathRoot(targetEntity, xpathForeignKey);
        }

        if (xpathInfoForeignKey != null && xpathInfoForeignKey.length() > 0) {
            String[] fkInfoPaths = xpathInfoForeignKey.split(",");//$NON-NLS-1$
            xpathInfoForeignKey = "";//$NON-NLS-1$
            for (int i = 0; i < fkInfoPaths.length; i++) {
                String fkInfoPath = fkInfoPaths[i];
                String relacedFkInfoPath = replaceXpathRoot(targetEntity, fkInfoPath);
                if (relacedFkInfoPath != null && relacedFkInfoPath.length() > 0) {
                    if (xpathInfoForeignKey.length() > 0)
                        xpathInfoForeignKey += ",";//$NON-NLS-1$
                    xpathInfoForeignKey += relacedFkInfoPath;
                }
            }
        }
        fkDrawer.setXpathForeignKey(xpathForeignKey);
        fkDrawer.setXpathInfoForeignKey(xpathInfoForeignKey);
        return fkDrawer;
    }

    /**
     * DOC HSHU Comment method "replaceXpathRoot".
     * 
     * @param targetEntity
     * @param xpathForeignKey
     * @return
     */
    private String replaceXpathRoot(String targetEntity, String xpath) {
        if (xpath.indexOf("/") != -1)//$NON-NLS-1$
            xpath = targetEntity + xpath.substring(xpath.indexOf("/"));//$NON-NLS-1$
        else
            xpath = targetEntity;
        return xpath;
    }

    private static String pushUpdateReport(String[] ids, String concept, String operationType, int docIndex) throws Exception {
        if (LOG.isTraceEnabled())
            LOG.trace("pushUpdateReport() concept " + concept + " operation " + operationType);

        // check updatedPath
        if (docIndex != -1) {
            WebContext ctx = WebContextFactory.get();
            HashMap<String, UpdateReportItem> updatedPath = new HashMap<String, UpdateReportItem>();
            updatedPath = (HashMap<String, UpdateReportItem>) ctx.getSession().getAttribute("updatedPath" + docIndex);//$NON-NLS-1$
            if (!("PHYSICAL_DELETE".equals(operationType) || "LOGIC_DELETE".equals(operationType)) && (updatedPath == null || updatedPath.isEmpty())) { //$NON-NLS-1$//$NON-NLS-2$
                return "ERROR_2";//$NON-NLS-1$
            }

            String xml2 = Util.createUpdateReport(ids, concept, operationType, updatedPath);

            synchronizeUpdateState(ctx, docIndex);

            if (LOG.isDebugEnabled())
                LOG.debug("pushUpdateReport() " + xml2);

            return Util.persistentUpdateReport(xml2, true);

        } else
            return "OK";//$NON-NLS-1$
    }

    public static void synchronizeUpdateState(int docIndex) {
        WebContext ctx = WebContextFactory.get();
        synchronizeUpdateState(ctx, docIndex);
    }

    private static void synchronizeUpdateState(WebContext ctx, int docIndex) {
        if (docIndex != -1)
            ctx.getSession().setAttribute("updatedPath" + docIndex, null);//$NON-NLS-1$
        ctx.getSession().setAttribute("viewNameItems", null);//$NON-NLS-1$
    }

    private void insertAfter(Node newNode, Node node) {
        if (node.getNextSibling() != null)
            node.getParentNode().insertBefore(newNode, node.getNextSibling());
        else
            node.getParentNode().appendChild(newNode);
    }

    public static boolean checkSmartViewExistsByOpt(String concept, String language) {
        try {
            SmartViewDescriptions smDescs = new SmartViewDescriptions();
            smDescs.build(concept, language);

            Set<SmartViewDescriptions.SmartViewDescription> smDescSet = smDescs.get(language);

            // Add the no language Smart Views too
            smDescSet.addAll(smDescs.get(null));

            if (!smDescSet.isEmpty())
                return true;
            else
                return false;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    public static boolean checkSmartViewExistsByLang(String concept, String language, boolean useNoLang) {
        return checkSmartViewExistsByLangAndOptName(concept, language, null, useNoLang);
    }

    public static boolean checkSmartViewExistsByLangAndOptName(String concept, String language, String optname, boolean useNoLang) {
        try {
            SmartViewDescriptions smDescs = new SmartViewDescriptions();
            smDescs.build(concept, language);

            Set<SmartViewDescriptions.SmartViewDescription> smDescSet = smDescs.get(language);
            if (useNoLang) {
                // Add the no language Smart Views too
                smDescSet.addAll(smDescs.get(null));
            }
            for (SmartViewDescriptions.SmartViewDescription smDesc : smDescSet) {
                if (optname != null) {
                    if (optname.equals(smDesc.getOptName()))
                        return true;
                } else {
                    if (smDesc.getOptName() == null)
                        return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    public static boolean checkSmartViewExists(String concept, String language) {
        boolean ret = checkSmartViewExistsByLang(concept, language, true);
        return ret;
    }

    public boolean checkIfDocumentExists(String[] ids, String concept) throws Exception {
        Configuration config = Configuration.getInstance();
        boolean flag = Util.getPort()
                .existsItem(new WSExistsItem(new WSItemPK(new WSDataClusterPK(config.getCluster()), concept, ids))).is_true();
        return flag;
    }

    public int countItems(String criteria, String dataObjet) throws Exception {
        Configuration config = Configuration.getInstance();
        String[] criterias = criteria.split("[\\s]+OR[\\s]+");//$NON-NLS-1$
        ArrayList<WSWhereItem> conditions = new ArrayList<WSWhereItem>();

        for (String cria : criterias) {
            ArrayList<WSWhereItem> condition = new ArrayList<WSWhereItem>();
            String[] subCriterias = cria.split("[\\s]+AND[\\s]+");//$NON-NLS-1$
            for (String subCria : subCriterias) {
                if (subCria.startsWith("(")) {//$NON-NLS-1$
                    subCria = subCria.substring(1);
                }
                if (subCria.endsWith(")")) {//$NON-NLS-1$
                    subCria = subCria.substring(0, subCria.length() - 1);
                }

                WSWhereItem whereItem = countItem(subCria, dataObjet);
                condition.add(whereItem);
            }
            if (condition.size() > 0) {
                WSWhereAnd and = new WSWhereAnd(condition.toArray(new WSWhereItem[condition.size()]));
                WSWhereItem whand = new WSWhereItem(null, and, null);
                conditions.add(whand);
            }
        }
        WSWhereOr or = new WSWhereOr(conditions.toArray(new WSWhereItem[conditions.size()]));
        WSWhereItem wi = new WSWhereItem(null, null, or);

        // count items
        int count = Integer.parseInt(Util.getPort()
                .count(new WSCount(new WSDataClusterPK(config.getCluster()), dataObjet, wi, 0)).getValue());

        WebContext ctx = WebContextFactory.get();
        ctx.getSession().setAttribute("totalCountItems", count);//$NON-NLS-1$

        return count;
    }

    public WSWhereItem countItem(String criteria, String dataObjet) throws Exception {
        WSWhereItem wi;
        String[] filters = criteria.split(" ");//$NON-NLS-1$
        String filterXpaths, filterOperators, filterValues;

        filterXpaths = filters[0];
        filterOperators = filters[1];
        if (filters.length <= 2)
            filterValues = " ";//$NON-NLS-1$
        else
            filterValues = filters[2];

        WSWhereCondition wc = new WSWhereCondition(filterXpaths, getOperator(filterOperators), filterValues,
                WSStringPredicate.NONE, false);
        ArrayList<WSWhereItem> conditions = new ArrayList<WSWhereItem>();
        WSWhereItem item = new WSWhereItem(wc, null, null);
        conditions.add(item);

        if (conditions.size() == 0) {
            wi = null;
        } else {
            WSWhereAnd and = new WSWhereAnd(conditions.toArray(new WSWhereItem[conditions.size()]));
            wi = new WSWhereItem(null, and, null);
        }

        return wi;

    }

    public boolean prepareSessionForItemDetails(String concept, String language) {

        try {

            WebContext ctx = WebContextFactory.get();
            Configuration config = Configuration.getInstance();
            String model = config.getModel();

            CommonDWR.getFieldsByDataModel(model, concept, language, true);

            WSConceptKey key = Util.getPort().getBusinessConceptKey(
                    new WSGetBusinessConceptKey(new WSDataModelPK(model), concept));
            String[] keys = new String[key.getFields().length];
            keys = Arrays.copyOf(key.getFields(), key.getFields().length);
            for (int i = 0; i < keys.length; i++) {
                if (".".equals(key.getSelector()))//$NON-NLS-1$
                    keys[i] = "/" + concept + "/" + keys[i];//$NON-NLS-1$//$NON-NLS-2$
                else
                    keys[i] = key.getSelector() + keys[i];
            }
            ctx.getSession().setAttribute("foreignKeys", keys); //$NON-NLS-1$

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
        return true;

    }

    private Map<String, ArrayList<String>> getMetaDataTypes(View view) throws Exception {
        HashMap<String, ArrayList<String>> metaDataTypes = new HashMap<String, ArrayList<String>>();
        Configuration config = Configuration.getInstance(true);
        Map<String, XSElementDecl> xsdMap = CommonDWR.getConceptMap(config.getModel());

        String concept = view.getViewPK();
        if (concept.contains("Browse_items_"))//$NON-NLS-1$
            concept = CommonDWR.getConceptFromBrowseItemView(view.getViewPK());

        XSElementDecl el = xsdMap.get(concept);

        for (String viewItem : view.getViewables()) {
            ArrayList<String> dataTypesHolder = new ArrayList<String>();
            String[] pathSlices = viewItem.split("/");//$NON-NLS-1$
            XSElementDecl node = parseMetaDataTypes(el, pathSlices[0], dataTypesHolder);
            if (pathSlices.length > 1) {
                for (int i = 1; i < pathSlices.length; i++) {
                    node = parseMetaDataTypes(node, pathSlices[i], dataTypesHolder);
                }
            }
            metaDataTypes.put(viewItem, dataTypesHolder);
        }
        // fix the bug:0015278 by ymli
        Set<String> searchablesKeys = view.getSearchables().keySet();
        for (String key : searchablesKeys) {
            if (!metaDataTypes.containsKey(key)) {
                ArrayList<String> dataTypesHolder = new ArrayList<String>();
                String[] pathSlices = key.split("/");//$NON-NLS-1$
                XSElementDecl node = parseMetaDataTypes(el, pathSlices[0], dataTypesHolder);
                if (pathSlices.length > 1) {
                    for (int i = 1; i < pathSlices.length; i++) {
                        node = parseMetaDataTypes(node, pathSlices[i], dataTypesHolder);
                    }
                }
                metaDataTypes.put(key, dataTypesHolder);
            }

        }

        return metaDataTypes;
    }

    public Map<String, List<String>> getFKvalueInfoFromXSDElem(String concept, String path) {
        Map<String, List<String>> fkHandler = new HashMap<String, List<String>>();

        try {
            Configuration config = Configuration.getInstance();
            String dataModelPK = config.getModel();
            Map<String, XSElementDecl> map = CommonDWR.getConceptMap(dataModelPK);
            XSElementDecl decl = map.get(concept);
            String[] pathSlices = path.split("/");//$NON-NLS-1$

            if (pathSlices.length > 1) {
                for (int i = 1; i < pathSlices.length; i++) {
                    fkHandler = getForeignKeyInfoForXSDElem1(decl, pathSlices[i]);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return fkHandler;
        }

        return fkHandler;
    }

    /**
     * get foreignKey, foreignKeyInfo, foreignKeyFilter from specify node by path.
     * 
     * @param elemDecl
     * @param path
     * @return
     */
    private Map<String, List<String>> getForeignKeyInfoForXSDElem1(XSElementDecl elemDecl, String path) {
        Map<String, List<String>> foreignKeyContents = new HashMap<String, List<String>>();
        XSType type = elemDecl.getType();

        if (type instanceof XSComplexType) {
            XSComplexType cmpxType = (XSComplexType) type;
            XSContentType conType = cmpxType.getContentType();
            XSParticle[] children = conType.asParticle().getTerm().asModelGroup().getChildren();

            for (XSParticle child : children) {
                XSTerm term = child.getTerm();

                if (term instanceof XSElementDecl && ((XSElementDecl) term).getName().equals(path)) {
                    XSElementDecl childElem = (XSElementDecl) child.getTerm();

                    if (childElem.getAnnotation() instanceof AnnotationImpl) {
                        AnnotationImpl antnImp = (AnnotationImpl) childElem.getAnnotation();
                        ElementNSImpl ensImpl = (ElementNSImpl) antnImp.getAnnotation();
                        NodeList list = ensImpl.getChildNodes();
                        List<String> fkInfoHandler = new ArrayList<String>();
                        List<String> fkHandler = new ArrayList<String>();
                        List<String> fkFilterHandler = new ArrayList<String>();
                        List<String> fkRetrieveHandler = new ArrayList<String>();

                        for (int i = 0; i < list.getLength(); i++) {
                            Node node = list.item(i);

                            if (node instanceof TextImpl) {
                                TextImpl txtImpl = (TextImpl) node;

                                if (txtImpl.getNextSibling() instanceof ElementNSImpl) {
                                    ElementNSImpl ens = (ElementNSImpl) txtImpl.getNextSibling();

                                    if (ens.getAttributes().getNamedItem("source").getNodeValue().equals("X_ForeignKey")) {//$NON-NLS-1$//$NON-NLS-2$
                                        String value = ens.getTextContent();
                                        Pattern ptn = Pattern.compile("(.*?)\\[(.*?)\\]");//$NON-NLS-1$
                                        Matcher match = ptn.matcher(value);

                                        if (match.matches()) {
                                            value = match.group(1);
                                        }

                                        fkHandler.add(value);
                                    } else if (ens.getAttributes().getNamedItem("source").getNodeValue()//$NON-NLS-1$
                                            .equals("X_ForeignKeyInfo")) {//$NON-NLS-1$
                                        // @temp multiply fkinfo
                                        fkInfoHandler.add(ens.getFirstChild().getNodeValue());
                                    } else if (ens.getAttributes().getNamedItem("source").getNodeValue()//$NON-NLS-1$
                                            .equals("X_ForeignKey_Filter")) {//$NON-NLS-1$
                                        fkFilterHandler.add(ens.getFirstChild().getNodeValue());
                                    } else if (ens.getAttributes().getNamedItem("source").getNodeValue()//$NON-NLS-1$
                                            .equals("X_Retrieve_FKinfos")) {//$NON-NLS-1$
                                        fkRetrieveHandler.add(ens.getFirstChild().getNodeValue());
                                    }
                                }
                            }
                        }

                        foreignKeyContents.put("foreignKey", fkHandler);//$NON-NLS-1$
                        foreignKeyContents.put("foreignKeyInfo", fkInfoHandler);//$NON-NLS-1$
                        foreignKeyContents.put("foreignKeyFilter", fkFilterHandler);//$NON-NLS-1$
                        foreignKeyContents.put("foreignKeyRetrieve", fkRetrieveHandler);//$NON-NLS-1$
                    }
                }
            }
        }

        return foreignKeyContents;
    }

    private XSElementDecl parseMetaDataTypes(XSElementDecl elem, String pathSlice, ArrayList<String> valuesHolder) {
        valuesHolder.clear();
        XSContentType conType;
        if (elem == null)
            return null;
        XSType type = elem.getType();
        if (elem.getName().equals(pathSlice)) {
            if (elem.getType() instanceof XSComplexType) {
                valuesHolder.add("complex type");//$NON-NLS-1$
            } else {
                XSSimpleType simpType = (XSSimpleType) elem.getType();
                valuesHolder.add(simpType.getName());
            }
            return elem;
        }
        if (type instanceof XSComplexType) {
            XSComplexType cmpxType = (XSComplexType) type;
            conType = cmpxType.getContentType();
            XSParticle[] children = conType.asParticle().getTerm().asModelGroup().getChildren();
            for (XSParticle child : children) {
                if (child.getTerm() instanceof XSElementDecl) {
                    XSElementDecl childElem = (XSElementDecl) child.getTerm();
                    if (childElem.getName().equals(pathSlice)) {
                        ArrayList<String> fkContents = getForeignKeyInfoForXSDElem(childElem);
                        if (fkContents.size() > 0) {

                            valuesHolder.add("foreign key");//$NON-NLS-1$
                            valuesHolder.addAll(fkContents);
                            return childElem;
                        }

                        if (childElem.getType() instanceof XSSimpleType) {
                            XSSimpleType simpType = (XSSimpleType) childElem.getType();
                            Collection<FacetImpl> facets = (Collection<FacetImpl>) simpType.asRestriction().getDeclaredFacets();
                            for (XSFacet facet : facets) {
                                if (facet.getName().equals("enumeration")) {//$NON-NLS-1$
                                    valuesHolder.add("enumeration");//$NON-NLS-1$
                                    break;
                                }
                            }
                            if (!valuesHolder.contains("enumeration")) {//$NON-NLS-1$

                                String basicName = simpType.getBaseType().getName();
                                String simpTypeName = simpType.getName();
                                if (simpType.getTargetNamespace().equals("http://www.w3.org/2001/XMLSchema")) {//$NON-NLS-1$
                                    simpTypeName = "xsd:" + simpTypeName;//$NON-NLS-1$
                                } else
                                    simpTypeName = "xsd:" + basicName;//$NON-NLS-1$
                                valuesHolder.add(simpTypeName);
                            } else if (simpType.asRestriction() != null && valuesHolder.contains("enumeration")) {//$NON-NLS-1$
                                Iterator<XSFacet> facetIter = simpType.asRestriction().iterateDeclaredFacets();
                                while (facetIter.hasNext()) {
                                    XSFacet facet = facetIter.next();
                                    valuesHolder.add(facet.getValue().value);
                                }
                            }
                        } else {
                            valuesHolder.add("complex type");//$NON-NLS-1$
                        }
                        return childElem;
                    }
                }
            }
        } else {
            XSSimpleType simpType = (XSSimpleType) type;
        }

        return null;

    }

    private ArrayList<String> getForeignKeyInfoForXSDElem(XSElementDecl elemDecl) {
        ArrayList<String> foreignKeyContents = new ArrayList<String>();
        if (elemDecl.getAnnotation() instanceof AnnotationImpl) {
            AnnotationImpl antnImp = (AnnotationImpl) elemDecl.getAnnotation();

            ElementNSImpl ensImpl = (ElementNSImpl) antnImp.getAnnotation();
            NodeList list = ensImpl.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node instanceof TextImpl) {
                    TextImpl txtImpl = (TextImpl) node;
                    if (txtImpl.getNextSibling() instanceof ElementNSImpl) {
                        ElementNSImpl ens = (ElementNSImpl) txtImpl.getNextSibling();
                        if (ens.getAttributes().getNamedItem("source").getNodeValue().equals("X_ForeignKey")) {//$NON-NLS-1$//$NON-NLS-2$
                            String value = ens.getTextContent();
                            Pattern ptn = Pattern.compile("(.*?)\\[(.*?)\\]");//$NON-NLS-1$
                            Matcher match = ptn.matcher(value);
                            if (match.matches()) {
                                value = match.group(1);
                            }
                            foreignKeyContents.add(0, value);
                        } else if (ens.getAttributes().getNamedItem("source").getNodeValue().equals("X_ForeignKeyInfo")) {//$NON-NLS-1$//$NON-NLS-2$
                            String fkInfo = null;
                            if (foreignKeyContents.size() > 1) {
                                fkInfo = foreignKeyContents.get(1);
                            }
                            if (fkInfo == null) {
                                fkInfo = ens.getFirstChild().getNodeValue();
                            } else {
                                fkInfo += "," + ens.getFirstChild().getNodeValue();//$NON-NLS-1$
                            }
                            foreignKeyContents.add(0, fkInfo);
                        }
                    }
                }
            }
        }
        if (foreignKeyContents.size() == 1) {
            foreignKeyContents.add(1, "");//$NON-NLS-1$
        }
        return foreignKeyContents;
    }

    private WSWhereOperator getOperator(String option) {
        WSWhereOperator res = null;
        if (option.equalsIgnoreCase("CONTAINS"))//$NON-NLS-1$
            res = WSWhereOperator.CONTAINS;
        else if (option.equalsIgnoreCase("EQUALS"))//$NON-NLS-1$
            res = WSWhereOperator.EQUALS;
        else if (option.equalsIgnoreCase("GREATER_THAN"))//$NON-NLS-1$
            res = WSWhereOperator.GREATER_THAN;
        else if (option.equalsIgnoreCase("GREATER_THAN_OR_EQUAL"))//$NON-NLS-1$
            res = WSWhereOperator.GREATER_THAN_OR_EQUAL;
        else if (option.equalsIgnoreCase("JOIN"))//$NON-NLS-1$
            res = WSWhereOperator.JOIN;
        else if (option.equalsIgnoreCase("LOWER_THAN"))//$NON-NLS-1$
            res = WSWhereOperator.LOWER_THAN;
        else if (option.equalsIgnoreCase("LOWER_THAN_OR_EQUAL"))//$NON-NLS-1$
            res = WSWhereOperator.LOWER_THAN_OR_EQUAL;
        else if (option.equalsIgnoreCase("NOT_EQUALS"))//$NON-NLS-1$
            res = WSWhereOperator.NOT_EQUALS;
        else if (option.equalsIgnoreCase("STARTSWITH"))//$NON-NLS-1$
            res = WSWhereOperator.STARTSWITH;
        else if (option.equalsIgnoreCase("STRICTCONTAINS"))//$NON-NLS-1$
            res = WSWhereOperator.STRICTCONTAINS;
        return res;
    }

    private org.jboss.dom4j.Document parsXMLString(String xmlString) {
        SAXReader saxReader = new SAXReader();
        org.jboss.dom4j.Document document = null;
        try {
            document = saxReader.read(new StringReader(xmlString));
        } catch (DocumentException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
        return document;

    }

    public ListRange getSmartViewList(int start, int limit, String sort, String dir, String regex) throws Exception {
        ListRange listRange = new ListRange();
        try {
            if (regex == null || regex.length() == 0)
                return listRange;

            String[] inputParams = regex.split("&");//$NON-NLS-1$
            String concept = inputParams[0];
            String language = inputParams[1];

            // Get SmartViews from processes
            SmartViewDescriptions smDescs = new SmartViewDescriptions();
            smDescs.build(concept, language);

            List<ComboItemBean> comboItems = new ArrayList<ComboItemBean>();

            // Get the lang Smart Views first : Smart_view_<entity>_<ISO> and Smart_view_<entity>_<ISO>#<option>
            Set<SmartViewDescriptions.SmartViewDescription> smDescSet = smDescs.get(language);
            // Add the fallback noLang Smart Views too : Smart_view_<entity> and Smart_view_<entity>#<option>
            smDescSet.addAll(smDescs.get(null));

            for (SmartViewDescriptions.SmartViewDescription smDesc : smDescSet) {
                String value = URLEncoder.encode(smDesc.getName(), "UTF-8"); //$NON-NLS-1$
                comboItems.add(new ComboItemBean(value, smDesc.getDisplayName()));
            }

            listRange.setData(comboItems.toArray());
            listRange.setTotalSize(comboItems.size());

        } catch (Exception e) {
            String err = "Unable to get Smart view List! ";
            LOG.error(e.getMessage(), e);
            throw new Exception(err);
        }
        return listRange;
    }

    public ListRange getRunnableProcessList(int start, int limit, String sort, String dir, String regex) throws Exception {
        ListRange listRange = new ListRange();
        try {

            if (regex == null || regex.length() == 0)
                return listRange;
            String[] inputParams = regex.split("&");//$NON-NLS-1$
            String businessConcept = inputParams[0];
            String language = inputParams[1];

            // get Runnable process
            List<ComboItemBean> comboItem = new ArrayList<ComboItemBean>();

            // FIXME we can cache these concepts
            Configuration config = Configuration.getInstance(true);
            String model = config.getModel();
            String[] businessConcepts = Util.getPort().getBusinessConcepts(new WSGetBusinessConcepts(new WSDataModelPK(model)))
                    .getStrings();

            WSTransformerPK[] wst = Util.getPort().getTransformerPKs(new WSGetTransformerPKs("*")).getWsTransformerPK();//$NON-NLS-1$
            for (int i = 0; i < wst.length; i++) {
                if (isMyRunableProcess(wst[i].getPk(), businessConcept, businessConcepts)) {
                    // edit by ymli;fix the bug:0012025
                    // Use the Process description instead of the '#' suffix in the run process drop-down list.
                    // and if the description is null, use the default value.
                    WSTransformer trans = Util.getPort().getTransformer(new WSGetTransformer(wst[i]));
                    String description = trans.getDescription();
                    Pattern p = Pattern.compile(".*\\[" + language.toUpperCase() + ":(.*?)\\].*", Pattern.DOTALL);//$NON-NLS-1$//$NON-NLS-2$
                    String name = p.matcher(description).replaceAll("$1");//$NON-NLS-1$
                    if (name.equals(""))//$NON-NLS-1$
                        if (language.equalsIgnoreCase("fr"))//$NON-NLS-1$
                            name = "Action par d茅faut";//$NON-NLS-1$
                        else if (language.equalsIgnoreCase("en"))//$NON-NLS-1$
                            name = "Default Action";//$NON-NLS-1$
                        else
                            name = description;
                    comboItem.add(new ComboItemBean(wst[i].getPk(), name));
                }
            }

            listRange.setData(comboItem.toArray());
            listRange.setTotalSize(comboItem.size());

        } catch (Exception e) {
            String err = "Unable to get Runnable Process List! ";
            LOG.error(e.getMessage(), e);
            throw new Exception(e.getLocalizedMessage());
        }
        return listRange;
    }

    private boolean isMyRunableProcess(String transformerName, String ownerConcept, String[] businessConcepts) {

        String possibleConcept = "";//$NON-NLS-1$
        if (businessConcepts != null) {
            for (int i = 0; i < businessConcepts.length; i++) {
                String businessConcept = businessConcepts[i];
                if (transformerName.startsWith("Runnable_" + businessConcept)) {//$NON-NLS-1$
                    if (businessConcept.length() > possibleConcept.length())
                        possibleConcept = businessConcept;
                }
            }
        }

        if (ownerConcept != null && ownerConcept.equals(possibleConcept))
            return true;

        return false;
    }

    public String processItem(String concept, String[] ids, int docIndex, String transformerPK) throws Exception {
        try {
            if (ids.length == 0) {
                WebContext ctx = WebContextFactory.get();
                ids = (String[]) ctx.getSession().getAttribute("treeIdxToIDS" + docIndex);//$NON-NLS-1$
            }
            String itemAlias = concept + "." + Util.joinStrings(ids, ".");//$NON-NLS-1$//$NON-NLS-2$
            // create updateReport
            LOG.info("Creating update-report for " + itemAlias + "'s action. ");
            String updateReport = Util.createUpdateReport(ids, concept, "ACTION", null);//$NON-NLS-1$

            WSTransformerContext wsTransformerContext = new WSTransformerContext(new WSTransformerV2PK(transformerPK), null, null);
            WSTypedContent wsTypedContent = new WSTypedContent(null, new WSByteArray(updateReport.getBytes("UTF-8")),//$NON-NLS-1$
                    "text/xml; charset=utf-8");//$NON-NLS-1$//$NON-NLS-2$
            WSExecuteTransformerV2 wsExecuteTransformerV2 = new WSExecuteTransformerV2(wsTransformerContext, wsTypedContent);
            // check runnable transformer
            // we can leverage the exception mechanism also
            boolean isRunnableTransformerExist = false;
            WSTransformerPK[] wst = Util.getPort().getTransformerPKs(new WSGetTransformerPKs("*")).getWsTransformerPK();//$NON-NLS-1$
            for (int i = 0; i < wst.length; i++) {
                if (wst[i].getPk().equals(transformerPK)) {
                    isRunnableTransformerExist = true;
                    break;
                }
            }
            // execute

            WSTransformer wsTransformer = Util.getPort().getTransformer(new WSGetTransformer(new WSTransformerPK(transformerPK)));
            if (wsTransformer.getPluginSpecs() == null || wsTransformer.getPluginSpecs().length == 0)
                throw new Exception("The Plugin Specs of this process is undefined! ");

            boolean outputReport = false;
            String downloadUrl = "";//$NON-NLS-1$
            if (isRunnableTransformerExist) {
                LOG.info("Executing transformer for " + itemAlias + "'s action. ");
                WSTransformerContextPipelinePipelineItem[] entries = Util.getPort().executeTransformerV2(wsExecuteTransformerV2)
                        .getPipeline().getPipelineItem();
                if (entries.length > 0) {
                    WSTransformerContextPipelinePipelineItem item = entries[entries.length - 1];
                    if (item.getVariable().equals("output_url")) {//$NON-NLS-1$
                        byte[] bytes = item.getWsTypedContent().getWsBytes().getBytes();
                        String content = new String(bytes);
                        try {
                            Document resultDoc = Util.parse(content);
                            NodeList attrList = Util.getNodeList(resultDoc, "//attr");//$NON-NLS-1$
                            if (attrList != null && attrList.getLength() > 0) {
                                downloadUrl = attrList.item(0).getTextContent();
                                outputReport = true;
                            }
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
            } else {
                // return false;
                throw new Exception("The target process is not existed! ");
            }
            // store
            LOG.info("Saving update-report for " + itemAlias + "'s action. ");

            if (!Util.persistentUpdateReport(updateReport, true).equals("OK")) {//$NON-NLS-1$
                // return false;
                throw new Exception("Store Update-Report failed! ");//$NON-NLS-1$
            }
            if (outputReport)
                return "Ok" + downloadUrl; //$NON-NLS-1$

        } catch (Exception e) {
            String err = "Unable to launch Runnable Process! ";
            LOG.error(e.getMessage(), e);
            String output = e.getLocalizedMessage();
            if (e.getLocalizedMessage() == null || e.getLocalizedMessage().equals(""))
                output = err;
            throw new Exception(output);
        }
        return "Ok"; //$NON-NLS-1$

    }

    /**
     * @author ymli
     * @param concept
     * @return
     * @throws RemoteException
     * @throws XtentisWebappException
     * @throws Exception
     */
    public boolean isReadOnlyinItem(String concept, String[] ids) throws RemoteException, XtentisWebappException, Exception {

        Configuration config = Configuration.getInstance();
        String dataClusterPK = config.getCluster();
        boolean ret = false;
        if (ids != null) {
            ret = LocalUser.getLocalUser().userItemCanWrite(new ItemPOJOPK(new DataClusterPOJOPK(dataClusterPK), concept, ids),
                    dataClusterPK, concept);
            if (ret)
                return false;
            ret = LocalUser.getLocalUser().userItemCanRead(new ItemPOJOPK(new DataClusterPOJOPK(dataClusterPK), concept, ids));
        }
        return ret;
    }

    /**
     * @author ymli; fix the bug:0013463
     * @param lang
     * @param format
     * @param value
     * @return
     * @throws ParseException
     * 
     */
    public String printFormat(String lang, String format, String value, String typeName) {
        try {
            if (typeName == null || typeName.equals("null") || format.equals("null") || "".equals(value))//$NON-NLS-1$//$NON-NLS-2$
                return value;
            Object object = Util.getTypeValue(lang, typeName, value, format);
            if (object instanceof Calendar || object instanceof Time || object == null) {
                return Util.outputValidateDate(value, format);
            }
            return object.toString();
        } catch (ParseException parseException) {
            LOG.error(parseException.getMessage());
            return "";
        }
    }

    /***
     * @author ymli get the format value of date
     * @param lang
     * @param format
     * @param value
     * @param typeName
     * @return
     * @throws ParseException
     */
    public String printFormatDate(String lang, String format, String value, String typeName) throws ParseException {

        Calendar object = null;
        if (typeName.equalsIgnoreCase("date"))//$NON-NLS-1$
            object = Date.parseDate(value.trim()).toCalendar();
        else if (typeName.equalsIgnoreCase("dateTime")) {//$NON-NLS-1$
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");//$NON-NLS-1$
            object = Calendar.getInstance();
            object.setTime(sdf.parse(value.trim()));
        }

        if (format == null || format.equals("null") || object == null)//$NON-NLS-1$
            return value;

        return Util.formatDate(format, object);
    }

    /**
     * @author ymli; fix the bug:0013463. validate the value from server
     * @param nodeId
     * @param value
     * @return
     */
    public String validateNode(String language, int nodeId, String value, int docIndex) {
        String errorMessage = null;
        WebContext ctx = WebContextFactory.get();
        HashMap<String, TreeNode> xpathToTreeNode = (HashMap<String, TreeNode>) ctx.getSession().getAttribute("xpathToTreeNode");//$NON-NLS-1$
        HashMap<Integer, String> idToXpath = (HashMap<Integer, String>) ctx.getSession().getAttribute("idToXpath" + docIndex);//$NON-NLS-1$
        Locale locale = new Locale(language);

        if (idToXpath == null)
            return "null";//$NON-NLS-1$

        String xpath = idToXpath.get(nodeId);
        TreeNode node = null;
        ArrayList<Restriction> restrictions = null;
        if (xpath != null) {
            if (xpathToTreeNode == null)
                return "null";//$NON-NLS-1$
            node = xpathToTreeNode.get(xpath);

            if (xpath.lastIndexOf("]") == xpath.length() - 1 && node == null) {//$NON-NLS-1$
                node = xpathToTreeNode.get(xpath.replaceAll("\\[\\d+\\]$", "[1]"));//$NON-NLS-1$//$NON-NLS-2$
            }
        }
        if (node == null)
            return "null";//$NON-NLS-1$
        boolean isValidation = true;// if true, return null,else return errorMessage

        if (node.getTypeName().equals("double") || node.getTypeName().equals("float") || node.getTypeName().equals("decimal")) {//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            if (node.getMinOccurs() > 0) {
                if (!isNumeric(value)) {
                    return MESSAGES.getMessage(locale, "field.invalid", node.getTypeName()); //$NON-NLS-1$
                }
            } else {
                if (!"".equals(value) && !isNumeric(value)) {//$NON-NLS-1$
                    return MESSAGES.getMessage(locale, "field.invalid", node.getTypeName()); //$NON-NLS-1$
                }
            }
        } else if (node.getTypeName().equals("int") || node.getTypeName().equals("integer") || node.getTypeName().equals("long")//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                || node.getTypeName().equals("short")) {//$NON-NLS-1$
            if (node.getMinOccurs() > 0) {
                if (!isInteger(value)) {
                    return MESSAGES.getMessage(locale, "field.invalid", node.getTypeName()); //$NON-NLS-1$
                }
            } else {
                if (!"".equals(value) && !isInteger(value)) {//$NON-NLS-1$ 
                    return MESSAGES.getMessage(locale, "field.invalid", node.getTypeName()); //$NON-NLS-1$
                }
            }
        } else if (node.getTypeName().equals("date")) {//$NON-NLS-1$
            String targetValue = (node.getValue() != null && !"".equals(node.getValue()) && node.getRealValue() != null && !""
                    .equals(node.getRealValue())) ? node.getRealValue() : value;
            // System.out.println("targetValue = " + targetValue);
            if (node.getMinOccurs() > 0) {
                if (!isDate(targetValue)) {
                    return MESSAGES.getMessage(locale, "datefield.invalid", node.getTypeName());//$NON-NLS-1$
                }
            } else {
                if (!"".equals(targetValue) && !isDate(targetValue)) {//$NON-NLS-1$ 
                    return MESSAGES.getMessage(locale, "datefield.invalid", node.getTypeName());//$NON-NLS-1$
                }
            }
        } else if (node.getTypeName().equals("dateTime")) {//$NON-NLS-1$       
            String targetValue = (node.getValue() != null && !"".equals(node.getValue()) && node.getRealValue() != null && !"" //$NON-NLS-1$  //$NON-NLS-2$ 
                    .equals(node.getRealValue())) ? node.getRealValue() : value;
            if (node.getMinOccurs() > 0) {
                if (!isDateTime(targetValue)) {
                    return MESSAGES.getMessage(locale, "datetimefield.invalid", node.getTypeName());//$NON-NLS-1$
                }
            } else {
                if (!"".equals(value) && !isDateTime(targetValue)) {//$NON-NLS-1$ 
                    return MESSAGES.getMessage(locale, "datetimefield.invalid", node.getTypeName());//$NON-NLS-1$
                }
            }
        }

        if (value.length() == 0 && node != null && (node.getMinOccurs() >= 1)) {
            // by yguo, fix 0016045: Facet messages not taken into account
            if (node.getRestrictions() != null && node.getFacetErrorMsg() != null && node.getFacetErrorMsg().size() != 0) {
                restrictions = node.getRestrictions();
                errorMessage = (String) node.getFacetErrorMsg().get(language);
            }
            boolean ancestor = checkAncestorMinOCcurs(node);
            if (ancestor || (!ancestor && !isSiblingNodeEmpty(xpathToTreeNode, node))) {
                if (node.getMinOccurs() >= 1)
                    errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "minOccurs.invalid", node.getMinOccurs())//$NON-NLS-1$
                            : errorMessage;
                else
                    errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "mandatory.invalid") : errorMessage;//$NON-NLS-1$
            }
            // isValidation = false;
            if (node.isKey()) {
                errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "key.invalid") : errorMessage;//$NON-NLS-1$
            }
            return errorMessage == null ? "null" : errorMessage;
        }

        if (node != null) {
            restrictions = node.getRestrictions();
        }
        if (restrictions == null)
            return "null";//$NON-NLS-1$

        for (Restriction re : restrictions) {
            if (node.getFacetErrorMsg() != null)
                errorMessage = (String) node.getFacetErrorMsg().get(language);
            if (value.length() == 0 && node.isKey()) {
                errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "key.invalid") : errorMessage;//$NON-NLS-1$
                isValidation = false;
                break;
            }

            if (node.getMinOccurs() >= 1 || (node.getMinOccurs() == 0 && value.trim().length() != 0)) {
                if (re.getName().equals("pattern")) {//$NON-NLS-1$
                    if (!AUTO_INCREMENT.equals(value) && !Pattern.compile(re.getValue()).matcher(value).matches()) {
                        errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "match.invalid", value, re.getValue())//$NON-NLS-1$
                                : errorMessage;
                        isValidation = false;
                        break;
                    }
                }
                if (re.getName().equals("minLength") && value.length() < Integer.parseInt(re.getValue())) {//$NON-NLS-1$
                    errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "minlength.invalid", re.getValue()) : errorMessage;//$NON-NLS-1$
                    isValidation = false;
                    break;
                }
                if (re.getName().equals("maxLength") && value.length() > Integer.parseInt(re.getValue())) {//$NON-NLS-1$
                    errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "maxlength.invalid", re.getValue()) : errorMessage;//$NON-NLS-1$
                    isValidation = false;
                    break;
                }
                if (re.getName().equals("length") && value.length() != Integer.parseInt(re.getValue())) {//$NON-NLS-1$
                    errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "length.invalid", re.getValue()) : errorMessage;//$NON-NLS-1$
                    isValidation = false;
                    break;
                }
                if (re.getName().equals("minExclusive"))//$NON-NLS-1$
                    if (!isNumeric(value)) {
                        errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "number.invalid", node.getName())//$NON-NLS-1$
                                : errorMessage;
                        isValidation = false;
                        break;
                    } else if (Float.parseFloat(value) <= Float.parseFloat(re.getValue())) {
                        errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "minExclusive.invalid", re.getValue())//$NON-NLS-1$
                                : errorMessage;
                        isValidation = false;
                        break;
                    }

                if (re.getName().equals("minInclusive")) {//$NON-NLS-1$
                    if (!isNumeric(value)) {
                        errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "number.invalid", node.getName())//$NON-NLS-1$
                                : errorMessage;
                        isValidation = false;
                        break;
                    } else if (Float.parseFloat(value) < Float.parseFloat(re.getValue())) {
                        errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "minInclusive.invalid", re.getValue())//$NON-NLS-1$
                                : errorMessage;
                        isValidation = false;
                        break;
                    }
                }

                if (re.getName().equals("maxInclusive")) {//$NON-NLS-1$
                    if (!isNumeric(value)) {
                        errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "number.invalid", node.getName())//$NON-NLS-1$
                                : errorMessage;
                        isValidation = false;
                        break;
                    } else if (Float.parseFloat(value) > Float.parseFloat(re.getValue())) {
                        errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "maxInclusive.invalid", re.getValue())//$NON-NLS-1$
                                : errorMessage;
                        isValidation = false;
                        break;
                    }
                }

                if (re.getName().equals("maxExclusive")) {//$NON-NLS-1$
                    if (!isNumeric(value)) {
                        errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "number.invalid", node.getName())//$NON-NLS-1$
                                : errorMessage;
                        isValidation = false;
                        break;
                    } else if (Float.parseFloat(value) >= Float.parseFloat(re.getValue())) {
                        errorMessage = errorMessage == null ? MESSAGES.getMessage(locale, "maxExclusive.invalid", re.getValue())//$NON-NLS-1$
                                : errorMessage;
                        isValidation = false;
                        break;
                    }
                }
            }

        }

        return isValidation ? "null" : errorMessage;//$NON-NLS-1$
        // return null;
    }

    public static boolean validatedComplexMandatory(String language, int nodeId, int docIndex) {
        WebContext ctx = WebContextFactory.get();
        HashMap<Integer, XSParticle> idToParticle = (HashMap<Integer, XSParticle>) ctx.getSession().getAttribute("idToParticle"); //$NON-NLS-1$
        XSParticle selXsp = idToParticle.get(nodeId);
        boolean approved = true;
        if (selXsp != null) {
            XSParticle[] xsps = selXsp.getTerm().asModelGroup().getChildren();
            if (xsps != null) {
                for (XSParticle xsp : xsps) {
                    if (xsp.getMinOccurs() >= 1) {
                        return false;
                    }
                }
            }
        }
    	return approved;
    }

    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[\\-+]?[0-9]+\\.?[0-9]*");//$NON-NLS-1$
        return pattern.matcher(str).matches();
    }

    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("[\\-+]?[0-9]+");//$NON-NLS-1$
        return pattern.matcher(str).matches();
    }
    
    public static boolean isDate(String str) {
        Pattern pattern = Pattern.compile("^(?:(?!0000)[0-9]{4}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1[0-9]|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:0[48]|[2468][048]|[13579][26])00)-02-29)$");//$NON-NLS-1$
        return pattern.matcher(str).matches();
    }
    
    public static boolean isDateTime(String str) {
        Pattern pattern = Pattern.compile("^(?:(?!0000)[0-9]{4}-(?:(?:0[1-9]|1[0-2])-(?:0[1-9]|1[0-9]|2[0-8])|(?:0[13-9]|1[0-2])-(?:29|30)|(?:0[13578]|1[02])-31)|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:0[48]|[2468][048]|[13579][26])00)-02-29)T(20|21|22|23|[0-1]?\\d):[0-5]?\\d:[0-5]?\\d$");//$NON-NLS-1$
        return pattern.matcher(str).matches();
    }

    /**
     * @author ymli; check if this node is mandatory
     * @param node
     * @return
     */
    private boolean checkAncestorMinOCcurs(TreeNode node) {
        if (node.getParent() != null && node.getMinOccurs() >= 1 && node.getParent().getMinOccurs() >= 1)
            return true;
        else
            return false;
    }

    /**
     * check node's sibling node is empty or not
     * 
     * @param xpathToTreeNode
     * @param node
     * @return
     */
    private boolean isSiblingNodeEmpty(HashMap<String, TreeNode> xpathToTreeNode, TreeNode node) {

        String xpath = node.getBindingPath();
        int pos = xpath.lastIndexOf("/");//$NON-NLS-1$
        String parentPath = xpath.substring(0, pos);
        for (Entry<String, TreeNode> entry : xpathToTreeNode.entrySet()) {
            pos = entry.getKey().lastIndexOf("/");//$NON-NLS-1$
            String pPath = entry.getKey().substring(0, pos);
            if (pPath.equals(parentPath) && !entry.getKey().equals(xpath)) {
                if (entry.getValue() == null)
                    continue;
                String v = entry.getValue().getValue();
                if (v != null && v.trim().length() > 0)
                    return false;
            }
        }
        return true;
    }

    /**
     * @author ymli get the Conditions' name available
     * @return
     */
    public String getviewItemsCriterias(String view, boolean isShared) {
        String viewItemsCriteria = getSearchTemplateNames(0, 0, view, isShared);// "condion1##condition2##condion3##condition4";
        return viewItemsCriteria;
    }

    /**
     * @author ymli get the whereItems available base on Criteria
     * @return
     */
    public String getWhereItemsByCriteria(String viewName) {

        String whereItem = "";// "Country/isoCode#EQUALS#33# ###Country/label#CONTAINS#a#OR###Country/Continent#CONTAINS#6#AND";//$NON-NLS-1$

        try {
            String result = Util
                    .getPort()
                    .getItem(
                            new WSGetItem(new WSItemPK(new WSDataClusterPK(XSystemObjects.DC_SEARCHTEMPLATE.getName()),
                                    "BrowseItem", new String[] { viewName }))).getContent().trim();//$NON-NLS-1$
            if (result != null) {                
                String criterias = result.substring(result.indexOf("<WhereCriteria>") + 15, result.indexOf("</WhereCriteria>"));//$NON-NLS-1$//$NON-NLS-2$
                String[] criteria = criterias.split("</Criteria>");//$NON-NLS-1$
                String Field = "";//$NON-NLS-1$
                String Operator = "";//$NON-NLS-1$
                String Value = "";//$NON-NLS-1$
                String Join = " ";//$NON-NLS-1$
                int criteriaLenth = criteria[criteria.length - 1].trim().isEmpty() ? criteria.length - 1 : criteria.length;
                // String item="";
                for (int i = 0; i < criteriaLenth; i++) {
                    Field = criteria[i].substring(criteria[i].indexOf("<Field>") + 7, criteria[i].indexOf("</Field>"));//$NON-NLS-1$//$NON-NLS-2$
                    Operator = criteria[i].substring(criteria[i].indexOf("<Operator>") + 10, criteria[i].indexOf("</Operator>"));//$NON-NLS-1$//$NON-NLS-2$
                    Value = criteria[i].substring(criteria[i].indexOf("<Value>") + 7, criteria[i].indexOf("</Value>"));//$NON-NLS-1$//$NON-NLS-2$

                    whereItem += Field + "#" + Operator + "#" + Value + "#" + Join + "###";//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
                    if (criteria[i].indexOf("<Join>") + 6 < criteria[i].indexOf("</Join>"))//$NON-NLS-1$//$NON-NLS-2$
                        Join = criteria[i].substring(criteria[i].indexOf("<Join>") + 6, criteria[i].indexOf("</Join>"));//$NON-NLS-1$//$NON-NLS-2$

                }
            } else {
                return null;
            }
        } catch (RemoteException e) {
            LOG.error(e.getMessage(), e);
        } catch (XtentisWebappException e) {
            LOG.error(e.getMessage(), e);
        }
        if (!whereItem.isEmpty()) {
            whereItem = whereItem.substring(0, whereItem.length() - 3);
        }
        return whereItem;
    }

    /**
     * @author ymli save the Search Template
     * @param viewPK
     * @param templateName
     * @param owner
     * @param isShared
     * @param criterias
     * @return //[Country/isoCode EQUALS 33, , OR, ] //[Country/isoCode CONTAINS *]
     */
    public String saveCriteria(String viewPK, String templateName, boolean isShared, String[][] criteriasString) {
        String returnString = "OK";//$NON-NLS-1$
        try {
            String owner = Util.getLoginUserName();
            WhereCriteria whereCriteria = new WhereCriteria();
            BrowseItem searchTemplate = new BrowseItem();
            searchTemplate.setViewPK(viewPK);
            searchTemplate.setCriteriaName(templateName);
            searchTemplate.setShared(isShared);
            searchTemplate.setOwner(owner);
            // searchTemplate.setWhereCriteria(whereCriteria);

            Criteria[] criterias = whereCriteria.getCriterias();
            if (criterias == null || criterias.equals(null))
                criterias = new Criteria[criteriasString.length];
            setwhereCriteria(criterias, criteriasString);
            whereCriteria.setCriterias(criterias);
            searchTemplate.setWhereCriteria(whereCriteria);

            WSItemPK pk = Util.getPort().putItem(
                    new WSPutItem(new WSDataClusterPK(XSystemObjects.DC_SEARCHTEMPLATE.getName()), searchTemplate
                            .marshal2String(), new WSDataModelPK(XSystemObjects.DM_SEARCHTEMPLATE.getName()), false));

            if (pk != null)
                returnString = "OK";//$NON-NLS-1$
            else
                returnString = null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            returnString = e.getMessage();
        } finally {
            return returnString;
        }
    }

    private void setwhereCriteria(Criteria[] criterias, String[][] criteriasString) {

        for (int i = 0; i < criterias.length; i++) {
            Criteria criteria = new Criteria();// set to criterias
            String[] criteriaString = criteriasString[i];// get from criteriasString

            String[] paths = criteriaString[0].split(" ");//$NON-NLS-1$
            criteria.setField(paths[0]);
            criteria.setOperator(paths[1]);
            if (paths.length < 3)
                criteria.setValue("*");//$NON-NLS-1$
            else
                criteria.setValue(paths[2]);

            criteriaString[0] = criteriaString[0].replaceAll(" ", "#");//$NON-NLS-1$//$NON-NLS-2$
            for (int j = 1; j < criteriaString.length; j++) {
                if (criteriaString[j] != null && criteriaString[j].trim().length() > 0
                        && (criteriaString[j].trim().equals("AND") || criteriaString[j].trim().equals("OR")))//$NON-NLS-1$//$NON-NLS-2$
                    criteria.setJoin(criteriaString[j].trim());
                
            }

            criterias[i] = criteria;
        }

    }

    public ListRange getSearchTemplates(int start, int limit, String sort, String dir, String regex) throws Exception {

        ListRange listRange = new ListRange();
        ArrayList<SearchTempalteName> list = new ArrayList<SearchTempalteName>();
        String templates = getSearchTemplateNames(start, limit, regex, false);
        if (templates.length() > 0) {

            String[] searchTemplates = templates.split("##");//$NON-NLS-1$
            for (int i = 0; i < searchTemplates.length; i++) {
                SearchTempalteName name = new SearchTempalteName(searchTemplates[i]);
                list.add(name);
            }
        }
        listRange.setData(list.toArray());
        String countItem = countSearchTemplate(regex);
        listRange.setTotalSize(Integer.parseInt(countItem));
        return listRange;
    }

    public String getSearchTemplateNames(int start, int limit, String view, boolean isShared) {
        String templateNames = "";//$NON-NLS-1$
        try {
            int localStart = 0;
            int localLimit = 0;
            if (start == limit && limit == 0) {
                localStart = 0;
                localLimit = Integer.MAX_VALUE;
            } else {
                localStart = start;
                localLimit = limit;

            }
            WSWhereItem wi = new WSWhereItem();

            WSWhereCondition wc1 = new WSWhereCondition("BrowseItem/ViewPK", WSWhereOperator.EQUALS, view,//$NON-NLS-1$
                    WSStringPredicate.NONE, false);

            WSWhereCondition wc3 = new WSWhereCondition("BrowseItem/Owner", WSWhereOperator.EQUALS, Util.getAjaxSubject()//$NON-NLS-1$
                    .getUsername(), WSStringPredicate.OR, false);
            WSWhereCondition wc4;
            WSWhereOr or = new WSWhereOr();
            if (isShared) {
                wc4 = new WSWhereCondition("BrowseItem/Shared", WSWhereOperator.EQUALS, "true", WSStringPredicate.OR, false);//$NON-NLS-1$//$NON-NLS-2$

                or = new WSWhereOr(new WSWhereItem[] { new WSWhereItem(wc3, null, null), new WSWhereItem(wc4, null, null) });
            } else {
                or = new WSWhereOr(new WSWhereItem[] { new WSWhereItem(wc3, null, null) });
            }

            WSWhereAnd and = new WSWhereAnd(new WSWhereItem[] { new WSWhereItem(wc1, null, null),
            /* new WSWhereItem(wc2, null, null), */
            new WSWhereItem(null, null, or) });

            wi = new WSWhereItem(null, and, null);

            String[] results = Util.getPort()
                    .xPathsSearch(new WSXPathsSearch(new WSDataClusterPK(XSystemObjects.DC_SEARCHTEMPLATE.getName()), null,// pivot
                            new WSStringArray(new String[] { "BrowseItem/CriteriaName" }), wi, -1, localStart, localLimit, null, // order//$NON-NLS-1$
                            // by
                            null, // direction
                            false)).getStrings();

            // Map<String, String> map = new HashMap<String, String>();

            for (int i = 0; i < results.length; i++) {
                results[i] = results[i].replaceAll("<CriteriaName>(.*)</CriteriaName>", "$1");//$NON-NLS-1$
                templateNames += results[i] + "##";//$NON-NLS-1$
                // map.put(results[i], results[i]);
            }

        } catch (RemoteException e) {
            LOG.error(e.getMessage(), e);
        } catch (XtentisWebappException e) {
            LOG.error(e.getMessage(), e);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return templateNames;
    }

    public String deleteTemplate(String id) {
        try {
            String[] ids = { id };
            String concept = "BrowseItem";//$NON-NLS-1$
            String dataClusterPK = XSystemObjects.DC_SEARCHTEMPLATE.getName();
            if (ids != null) {
                WSItemPK wsItem = Util.getPort().deleteItem(
                        new WSDeleteItem(new WSItemPK(new WSDataClusterPK(dataClusterPK), concept, ids), false));

                if (wsItem == null)
                    return "ERROR - deleteTemplate is NULL";//$NON-NLS-1$
                return "OK";//$NON-NLS-1$
            } else {
                return "OK";//$NON-NLS-1$
            }
        } catch (Exception e) {
            return "ERROR -" + e.getLocalizedMessage();//$NON-NLS-1$
        }
    }

    public String countSearchTemplate(String view) throws Exception {

        WSWhereItem wi = new WSWhereItem();

        WSWhereCondition wc1 = new WSWhereCondition("BrowseItem/ViewPK", WSWhereOperator.EQUALS, view, WSStringPredicate.NONE,//$NON-NLS-1$
                false);
        WSWhereCondition wc3 = new WSWhereCondition("BrowseItem/Owner", WSWhereOperator.EQUALS, Util.getAjaxSubject()//$NON-NLS-1$
                .getUsername(), WSStringPredicate.NONE, false);

        WSWhereOr or = new WSWhereOr(new WSWhereItem[] { new WSWhereItem(wc3, null, null) });

        WSWhereAnd and = new WSWhereAnd(new WSWhereItem[] { new WSWhereItem(wc1, null, null),
        new WSWhereItem(null, null, or) });

        wi = new WSWhereItem(null, and, null);
        return Util.getPort()
                .count(new WSCount(new WSDataClusterPK(XSystemObjects.DC_SEARCHTEMPLATE.getName()), "BrowseItem", wi, -1))//$NON-NLS-1$
                .getValue();
    }

    public boolean isExistCriteria(String dataObjectLabel, String id) throws RemoteException, XtentisWebappException {

        WSItemPK wsItemPK = new WSItemPK();
        wsItemPK.setConceptName("BrowseItem");//$NON-NLS-1$

        WSDataClusterPK wsDataClusterPK = new WSDataClusterPK();
        wsDataClusterPK.setPk(XSystemObjects.DC_SEARCHTEMPLATE.getName());
        wsItemPK.setWsDataClusterPK(wsDataClusterPK);

        String[] ids = new String[1];
        ids[0] = id;
        wsItemPK.setIds(ids);

        WSExistsItem wsExistsItem = new WSExistsItem(wsItemPK);
        WSBoolean wsBoolean = Util.getPort().existsItem(wsExistsItem);
        return wsBoolean.is_true();
    }

    /**
     * get properties by specify key.
     * 
     * @param key
     * @return
     * @throws IOException
     */
    public String getProperty(String key) throws IOException {
        return PropsUtils.getProperties().getProperty(key);
    }

    /**
     * @author ymli
     * @param start
     * @param limit
     * @param sort
     * @param dir
     * @param regex
     * @return
     * @throws Exception
     */
    public ListRange getBookMarks(int start, int limit, String sort, String dir, String regex) throws Exception {
        ListRange listRange = new ListRange();
        String templates = getSearchTemplateNames(start, limit, regex, false);
        List<ComboItemBean> comboItem = new ArrayList<ComboItemBean>();
        /*
         * ComboItemBean save = new ComboItemBean("Bookmark this Search","Bookmark this Search"); ComboItemBean manage =
         * new ComboItemBean("Manage Search Bookmarks","Manage Search Bookmarks"); comboItem.add(save);
         * comboItem.add(manage);
         */
        if (templates.length() > 0) {
            String[] searchTemplates = templates.split("##");//$NON-NLS-1$
            for (int i = 0; i < searchTemplates.length; i++) {
                comboItem.add(new ComboItemBean(searchTemplates[i], searchTemplates[i]));
            }
        }
        listRange.setData(comboItem.toArray());
        String countItem = countSearchTemplate(regex);
        listRange.setTotalSize(Integer.parseInt(countItem));
        return listRange;
    }

    public List<String> getLineageEntity(String concept) throws Exception {
        List<String> refs = SchemaWebAgent.getInstance().getReferenceEntities(concept);

        return refs;
    }

    public ListRange getItems(int start, int limit, String sort, String dir, String regex) throws Exception {
        ListRange listRange = new ListRange();
        WSDataClusterPK wsDataClusterPK = new WSDataClusterPK();
        String entity = null;
        String contentWords = null;
        String keys = null;
        Long fromDate = new Long(-1);
        Long toDate = new Long(-1);
        String fkvalue = null;
        String dataObject = null;

        if (regex != null && regex.length() > 0) {
            JSONObject criteria = new JSONObject(regex);

            Configuration configuration = Configuration.getInstance();
            wsDataClusterPK.setPk(configuration.getCluster());
            entity = !criteria.isNull("entity") ? (String) criteria.get("entity") : "";//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            keys = !criteria.isNull("key") && !"*".equals(criteria.get("key")) ? (String) criteria.get("key") : "";//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$
            fkvalue = !criteria.isNull("fkvalue") && !"*".equals(criteria.get("fkvalue")) ? (String) criteria.get("fkvalue") : "";//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$
            dataObject = !criteria.isNull("dataObject") && !"*".equals(criteria.get("dataObject")) ? (String) criteria//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                    .get("dataObject") : "";//$NON-NLS-1$//$NON-NLS-2$
            contentWords = !criteria.isNull("keyWords") && !"*".equals(criteria.get("keyWords")) ? (String) criteria//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                    .get("keyWords") : "";//$NON-NLS-1$//$NON-NLS-2$

            if (!criteria.isNull("fromDate")) {//$NON-NLS-1$
                String startDate = (String) criteria.get("fromDate");//$NON-NLS-1$
                SimpleDateFormat dataFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//$NON-NLS-1$
                java.util.Date date = dataFmt.parse(startDate);
                fromDate = date.getTime();
            }

            if (!criteria.isNull("toDate")) {//$NON-NLS-1$
                String endDate = (String) criteria.get("toDate");//$NON-NLS-1$
                SimpleDateFormat dataFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//$NON-NLS-1$
                java.util.Date date = dataFmt.parse(endDate);
                toDate = date.getTime();
            }
        }

        // @temp yguo , xpath and value
        BusinessConcept businessConcept = SchemaWebAgent.getInstance().getBusinessConcept(entity);
        Map<String, String> foreignKeyMap = businessConcept.getForeignKeyMap();
        Set<String> foreignKeyXpath = foreignKeyMap.keySet();
        List<String> xpathes = new ArrayList<String>();

        for (String path : foreignKeyXpath) {
            String dataObjectPath = foreignKeyMap.get(path);
            if (dataObjectPath.indexOf(dataObject) != -1) {
                xpathes.add(path.substring(1));
            }
        }

        if (xpathes.size() == 0) {
            List<String> types = SchemaWebAgent.getInstance().getBindingType(businessConcept.getE());
            for (String type : types) {
                List<ReusableType> subTypes = SchemaWebAgent.getInstance().getMySubtypes(type);
                for (ReusableType reusableType : subTypes) {
                    Map<String, String> fks = SchemaWebAgent.getInstance().getReferenceEntities(reusableType, dataObject);
                    Collection<String> fkPaths = fks != null ? fks.keySet() : null;
                    for (String fkpath : fkPaths) {
                        if (fks.get(fkpath).indexOf(dataObject) != -1) {
                            xpathes.add(fkpath);
                        }
                    }
                }
            }
        }
        
        if (xpathes.size() == 0) {
            Map<String, String> inheritanceForeignKeyMap = businessConcept.getInheritanceForeignKeyMap();
            if(inheritanceForeignKeyMap.size() > 0){
                Set<String> keySet = inheritanceForeignKeyMap.keySet();
                String dataObjectPath  = null;
                for (String path : keySet) {
                    dataObjectPath = inheritanceForeignKeyMap.get(path);
                    if (dataObjectPath.indexOf(dataObject) != -1) {
                        xpathes.add(path.substring(1));
                    }
                }
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(keys);
        sb.append("$");//$NON-NLS-1$
        sb.append(joinList(xpathes, ","));//$NON-NLS-1$
        sb.append("$");//$NON-NLS-1$
        sb.append(fkvalue);

        WSItemPKsByCriteriaResponse results = Util.getPort().getItemPKsByFullCriteria(
                new WSGetItemPKsByFullCriteria(new WSGetItemPKsByCriteria(wsDataClusterPK, entity, contentWords, sb.toString(),
                        fromDate, toDate, start, limit), false));

        Map[] data = new Map[results.getResults().length - 1];
        int totalSize = 0;
        for (int i = 0; i < results.getResults().length; i++) {
            if (i == 0) {
                totalSize = Integer.parseInt(Util.parse(results.getResults()[i].getWsItemPK().getConceptName())
                        .getDocumentElement().getTextContent());
                continue;
            }

            Map record = new HashMap();
            record.put("date", new java.util.Date(results.getResults()[i].getDate()).toString());//$NON-NLS-1$
            record.put("entity", results.getResults()[i].getWsItemPK().getConceptName());//$NON-NLS-1$
            record.put("key", results.getResults()[i].getWsItemPK().getIds());//$NON-NLS-1$
            data[i - 1] = record;
        }

        listRange.setTotalSize(totalSize);
        listRange.setData(data);

        return listRange;
    }

    private String joinList(List<String> list, String decollator) {
        if (list == null)
            return "";
        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;
        for (String str : list) {
            if (isFirst) {
                sb.append(str);
                isFirst = false;
                continue;
            }
            sb.append(decollator + str);
        }
        return sb.toString();
    }

    public boolean isEntityCreatable(String conceptName) throws Exception {
        return !SchemaWebAgent.getInstance().isEntityDenyCreatable(conceptName);
    }

    public static void recursiveParseXML(Node node, Map<String, Integer> pathTimes, String path) {
        String name = node.getNodeName();
        NodeList nodeList = node.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            NodeList childList = childNode.getChildNodes();
            String childName = childNode.getNodeName();

            if (childList.getLength() == 1 && childList.item(0).hasChildNodes()) {
                String entryPath = path + name + "/" + childName; //$NON-NLS-1$
                Integer entry = pathTimes.get(entryPath);

                if (entry == null) {
                    pathTimes.put(entryPath, 1);
                } else {
                    pathTimes.put(entryPath, entry + 1);
                }
            } else if (childList.getLength() == 1) {
                continue;
            } else {
                String subPath = path + "/" + name + "/" + childName; //$NON-NLS-1$ //$NON-NLS-2$
                Integer entryValue = pathTimes.get(subPath);

                if (entryValue == null) {
                    pathTimes.put(subPath, 1);
                } else {
                    pathTimes.put(subPath, entryValue + 1);
                }
                recursiveParseXML(childNode, pathTimes, "/" + name + "/");//$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    public static void reFillXsdByXML(Document d, String dataModelPK, String concept, int docIndex) throws Exception {
        Map<String, Integer> multiOccurrence = new HashMap<String, Integer>();

        recursiveParseXML(d.getFirstChild(), multiOccurrence, "");
        if (multiOccurrence != null && multiOccurrence.size() > 0) {
            String xsd = Util.getPort().getDataModel(new WSGetDataModel(new WSDataModelPK(dataModelPK))).getXsdSchema();
            Document xsdDoc = Util.parse(xsd);
            // @yguo, refill the xsd by specify multiOccurrence
            if (multiOccurrence != null && multiOccurrence.size() > 0) {
                for (Iterator<String> iterator = multiOccurrence.keySet().iterator(); iterator.hasNext();) {
                    String xpath = iterator.next();
                    Integer num = multiOccurrence.get(xpath);
                    String xsdPath = "//xsd:element[@name=\"" + xpath.substring(xpath.lastIndexOf("/") + 1) + "\"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    NodeList nodeList = Util.getNodeList(xsdDoc, xsdPath);
                    int length = nodeList.getLength();
                    if (length < num) {
                        Node node = nodeList.item(0);
                        if (node != null) {
                            for (int j = 0; j < num - length; j++) {
                                Node cloneNode = node.cloneNode(true);
                                node.getParentNode().insertBefore(cloneNode, node);
                            }
                        }
                    }
                }
            }

            Map<String, XSElementDecl> map = com.amalto.core.util.Util.getConceptMap(CommonDWR.getXMLStringFromDocument(xsdDoc));
            XSElementDecl xsed = map.get(concept);
            DisplayRulesUtil displayRulesUtil = new DisplayRulesUtil(xsed);
            WebContext ctx = WebContextFactory.get();
            ctx.getSession().setAttribute("itemDocument_displayRulesUtil" + docIndex, displayRulesUtil); //$NON-NLS-1$
        }
    }
}
