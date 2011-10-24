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
package org.talend.mdm.webapp.browserecords.client.widget;

import java.util.List;

import org.talend.mdm.webapp.browserecords.client.model.ItemBean;
import org.talend.mdm.webapp.browserecords.client.widget.treedetail.TreeDetail;
import org.talend.mdm.webapp.browserecords.shared.ComplexTypeModel;
import org.talend.mdm.webapp.browserecords.shared.ViewBean;
import org.talend.mdm.webapp.browserecords.shared.VisibleRuleResult;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.TreeItem;

public class ItemPanel extends ContentPanel {

    private final TreeDetail tree = new TreeDetail();

    private ContentPanel smartPanel = new ContentPanel();

    private ItemDetailToolBar toolBar;

    private ViewBean viewBean;

    private ItemBean item;

    private String operation;
    
    private boolean isForeignKeyPanel;

    private ContentPanel contenPanel;

    public ItemPanel() {

    }

    public ItemPanel(ViewBean viewBean, ItemBean item, String operation) {
        this.viewBean = viewBean;
        this.item = item;
        this.toolBar = new ItemDetailToolBar(item, operation, viewBean);
        this.operation = operation;
        this.initUI(null);
    }

    public ItemPanel(ViewBean viewBean, ItemBean item, String operation, ContentPanel contenPanel, TreeItem root) {
        this.viewBean = viewBean;
        this.item = item;
        this.toolBar = new ItemDetailToolBar(item, operation, viewBean);
        this.operation = operation;
        this.isForeignKeyPanel = true;
        this.contenPanel = contenPanel;
        this.initUI(root);
    }
    
    private void initUI(TreeItem root) {
        this.setBodyBorder(false);
        this.setHeaderVisible(false);
        this.setTopComponent(toolBar);
        this.setLayout(new RowLayout(Orientation.VERTICAL));
        if(!isForeignKeyPanel){
            tree.setToolBar(toolBar);
            if (ItemDetailToolBar.CREATE_OPERATION.equals(operation)) {
                tree.initTree(viewBean, null);
            } else if (ItemDetailToolBar.VIEW_OPERATION.equals(operation)) {
                tree.initTree(viewBean, item);
            } else if (ItemDetailToolBar.DUPLICATE_OPERATION.equals(operation)) {
                tree.initTree(viewBean, item, operation);
            } else if (ItemDetailToolBar.PERSONALEVIEW_OPERATION.equals(operation)
                    || ItemDetailToolBar.SMARTVIEW_OPERATION.equals(operation)) {
                tree.initTree(viewBean, item);
            } else {
                tree.initTree(viewBean, null);
            }
            tree.expand();
            this.add(tree, new RowData(1, 1));
        }else{
            tree.setRoot(root);
            tree.setViewBean(viewBean);
            this.add(contenPanel, new RowData(1, 1));
        }
        
        smartPanel.setVisible(false);
        smartPanel.setHeaderVisible(false);
        this.add(smartPanel, new RowData(1, 1));
    }

    public void onUpdatePolymorphism(ComplexTypeModel typeModel) {
        tree.onUpdatePolymorphism(typeModel);
    }

    public void onExecuteVisibleRule(List<VisibleRuleResult> visibleResults) {
        tree.onExecuteVisibleRule(visibleResults);
    }

    public ItemBean getItem() {
        return item;
    }

    public TreeDetail getTree() {
        return tree;
    }

    public String getOperation() {
        return operation;
    }

    public void refreshTree() {
        tree.refreshTree(item);
        if (smartPanel.getWidget(0) != null && smartPanel.getWidget(0) instanceof Frame) {
            String url = ((Frame) smartPanel.getWidget(0)).getUrl();
            ((Frame) smartPanel.getWidget(0)).setUrl(url + "&" + Math.random()); //$NON-NLS-1$
            smartPanel.layout(true);
        }
    }

    public ContentPanel getSmartPanel() {
        return smartPanel;
    }

    public ItemDetailToolBar getToolBar() {
        return toolBar;
    }
}
