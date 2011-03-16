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
package org.talend.mdm.webapp.itemsbrowser2.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.talend.mdm.webapp.itemsbrowser2.client.boundary.GetService;
import org.talend.mdm.webapp.itemsbrowser2.client.creator.CellEditorCreator;
import org.talend.mdm.webapp.itemsbrowser2.client.creator.CellRendererCreator;
import org.talend.mdm.webapp.itemsbrowser2.client.model.ItemBean;
import org.talend.mdm.webapp.itemsbrowser2.client.util.Locale;
import org.talend.mdm.webapp.itemsbrowser2.client.widget.ItemsFormPanel;
import org.talend.mdm.webapp.itemsbrowser2.client.widget.ItemsSearchContainer;
import org.talend.mdm.webapp.itemsbrowser2.client.widget.inputfield.creator.FieldCreator;
import org.talend.mdm.webapp.itemsbrowser2.shared.EntityModel;
import org.talend.mdm.webapp.itemsbrowser2.shared.SimpleTypeModel;
import org.talend.mdm.webapp.itemsbrowser2.shared.TypeModel;
import org.talend.mdm.webapp.itemsbrowser2.shared.ViewBean;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.mvc.AppEvent;
import com.extjs.gxt.ui.client.mvc.Controller;
import com.extjs.gxt.ui.client.mvc.Dispatcher;
import com.extjs.gxt.ui.client.mvc.View;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.TabPanel.TabPosition;
import com.extjs.gxt.ui.client.widget.Viewport;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.grid.CellEditor;
import com.extjs.gxt.ui.client.widget.grid.CheckBoxSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class ItemsView extends View {

    private TabPanel tabFrame = null;

    public static Window window = null;

    private ItemsSearchContainer itemsSearchContainer = null;

    public static final String ROOT_DIV = "talend_itemsbrowser2_ItemsBrowser2";//$NON-NLS-1$

    public static final String TAB_FRAME = "tab_frame";//$NON-NLS-1$

    public static final String ITEMS_SEARCH_CONTAINER = "itemsSearchContainer";//$NON-NLS-1$

    public static final String ITEMS_FORM_TARGET = "items_form_target";

    public static final String TARGET_IN_NEW_WINDOW = "target_in_new_window";

    public static final String TARGET_IN_NEW_TAB = "target_in_new_tab";

    public static final String TARGET_IN_SEARCH_TAB = "target_in_search_tab";

    public ItemsView(Controller controller) {
        super(controller);
    }

    /*
     * (non-Jsdoc)
     * 
     * @see com.extjs.gxt.ui.client.mvc.Viewinitialize()
     */
    @Override
    protected void initialize() {
        super.initialize();
    }

    protected void handleEvent(AppEvent event) {

        if (event.getType() == ItemsEvents.InitFrame) {
            onInitFrame(event);
        } else if (event.getType() == ItemsEvents.InitSearchContainer) {
            onInitSearchContainer(event);
        } else if (event.getType() == ItemsEvents.GetView) {
            onGetView(event);
        } else if (event.getType() == ItemsEvents.SearchView) {
            onSearchView(event);
        } else if (event.getType() == ItemsEvents.ViewItemForm) {
            onViewItemForm(event);
        }
    }

    protected void onGetView(final AppEvent event) {
        ViewBean viewBean = event.getData();

        itemsSearchContainer = Registry.get(ITEMS_SEARCH_CONTAINER);
        itemsSearchContainer.getItemsListPanel().getToolBar().updateToolBar(viewBean);
    }

    protected void onSearchView(final AppEvent event) {
        ViewBean viewBean = event.getData();
        EntityModel entityModel = viewBean.getBindingEntityModel();

        // TODO update columns
        itemsSearchContainer = Registry.get(ITEMS_SEARCH_CONTAINER);
        List<ColumnConfig> ccList = new ArrayList<ColumnConfig>();
        CheckBoxSelectionModel<ItemBean> sm = new CheckBoxSelectionModel<ItemBean>();
        sm.setSelectionMode(SelectionMode.MULTI);
        ccList.add(sm.getColumn());
        List<String> viewableXpaths = viewBean.getViewableXpaths();
        Map<String, TypeModel> dataTypes = entityModel.getMetaDataTypes();
        for (String xpath : viewableXpaths) {
            TypeModel typeModel = dataTypes.get(xpath);
            ColumnConfig cc = new ColumnConfig(xpath, typeModel.getLabel(Locale.getLanguage(Itemsbrowser2.getSession()
                    .getAppHeader())), 200);
            if (typeModel instanceof SimpleTypeModel) {
                Field field = FieldCreator.createField((SimpleTypeModel) typeModel, null, false,
                        Locale.getLanguage(Itemsbrowser2.getSession().getAppHeader()));
                CellEditor cellEditor = CellEditorCreator.createCellEditor(field);
                if (cellEditor != null) {
                    cc.setEditor(cellEditor);
                }
            }

            GridCellRenderer<ModelData> renderer = CellRendererCreator.createRenderer(typeModel);
            if (renderer != null) {
                cc.setRenderer(renderer);
            }

            ccList.add(cc);
        }

        itemsSearchContainer.getItemsListPanel().updateGrid(sm, ccList);
        // TODO in the view of ViewItemForm binding
    }

    protected void onViewItemForm(AppEvent event) {
        ItemBean itemBean = event.getData();
        String itemsFormTarget = event.getData(ItemsView.ITEMS_FORM_TARGET);
        EntityModel entityModel = (EntityModel) Itemsbrowser2.getSession().getCurrentEntityModel();
        if (itemBean != null) {
            String tabTitle = itemBean.getConcept() + itemBean.getIds();
            if (itemsFormTarget.equals(ItemsView.TARGET_IN_SEARCH_TAB)) {
                itemsSearchContainer = Registry.get(ITEMS_SEARCH_CONTAINER);
                // itemsSearchContainer.getItemsFormPanel().paint(entityModel);
                itemsSearchContainer.getItemsFormPanel().bind(itemBean);
                // itemsSearchContainer.getItemsFormPanel().setReadOnly(itemBean, entityModel.getKeys());
                // TODO handle legacy form
            } else if (itemsFormTarget.equals(ItemsView.TARGET_IN_NEW_TAB)) {
                if (Itemsbrowser2.getSession().getAppHeader().isUsingDefaultForm()) {
                    ItemsFormPanel itemsFormPanel = new ItemsFormPanel();
                    addTab(itemsFormPanel, tabTitle, tabTitle, true);
                    // itemsFormPanel.paint(entityModel);
                    // itemsFormPanel.bind(itemBean);
                    // itemsFormPanel.setReadOnly(itemBean, entityModel.getKeys());
                } else {
                    GetService.openItemBrowser(itemBean.getIds(), itemBean.getConcept());
                }
            } else if (itemsFormTarget.equals(ItemsView.TARGET_IN_NEW_WINDOW)) {
                ItemsFormPanel itemsFormPanel = new ItemsFormPanel();
                addWin(itemsFormPanel, tabTitle, itemBean.getIds(), itemBean.getConcept());
                // itemsFormPanel.paint(entityModel);
                // itemsFormPanel.bind(itemBean);
                // itemsFormPanel.setReadOnly(itemBean, entityModel.getKeys());
                // TODO handle legacy form
            }
        }
    }

    protected void onInitFrame(AppEvent ae) {

        // create search panel
        // build frame
        Log.info("Init tab-frame... ");

        Viewport container = new Viewport() {

            public void onAttach() {
                super.onAttach();
                Widget w = this.getParent();
                setSize(w.getOffsetWidth(), w.getOffsetHeight());
            }

            protected void onWindowResize(int width, int height) {
                Widget w = this.getParent();
                setSize(w.getOffsetWidth(), w.getOffsetHeight());
            }
        };
        container.setLayout(new FitLayout());
        container.setAutoWidth(true);

        // build tab
        tabFrame = new TabPanel();
        tabFrame.setMinTabWidth(115);
        tabFrame.setResizeTabs(true);
        tabFrame.setAnimScroll(true);
        tabFrame.setTabScroll(true);
        tabFrame.setCloseContextMenu(true);
        tabFrame.setTabPosition(TabPosition.BOTTOM);
        tabFrame.setBodyBorder(false);

        container.add(tabFrame);

        // registry serves as a global context
        Registry.register(TAB_FRAME, tabFrame);

        // FIXME can not auto-fill
        container.setStyleAttribute("height", "100%");
        RootPanel.get(ROOT_DIV).getElement().getStyle().setHeight(100, Unit.PCT);
        RootPanel.get(ROOT_DIV).add(container);
        tabFrame.setHeight(container.getOffsetHeight());

        Dispatcher.forwardEvent(ItemsEvents.InitSearchContainer);
    }

    protected void onInitSearchContainer(AppEvent ae) {

        // create search panel
        Log.info("Init items-search-container... ");
        itemsSearchContainer = new ItemsSearchContainer();

        addTab(itemsSearchContainer, "Search Tab", "Search Tab", false);

        Registry.register(ITEMS_SEARCH_CONTAINER, itemsSearchContainer);

    }

    private void addTab(Component c, String tabId, String tabName, boolean closable) {
        TabItem item = tabFrame.getItemByItemId(tabId);

        if (item == null) {
            item = new TabItem();
            item.setItemId(tabId);
            item.setLayout(new FitLayout());
            item.setText(tabName);
            item.setClosable(closable);
            item.add(c);
            item.addStyleName("pad-text");
            tabFrame.add(item);
        }

        tabFrame.setSelection(item);

    }

    /**
     * DOC HSHU Comment method "addWin".
     */
    private void addWin(Component c, String title, String itemPK2, String dataObject) {

        // FIXME Do we need one window for one item?

        window = new Window();
        window.setSize(500, 500);
        window.setPlain(true);
        window.setModal(false);
        window.setHeading(title);
        window.setLayout(new FitLayout());

        window.add(c);

        window.setClosable(true);
        window.setResizable(true);
        window.setMaximizable(true);

        window.show();

        // random start point
        window.center();
        int left = window.getAbsoluteLeft();
        int top = window.getAbsoluteTop();
        int offset = Random.nextInt(35);
        window.setPosition(left + offset, top + offset);
        window.setId("formWindowContainer");
        GetService.regCallback();
        GetService.renderFormWindow(itemPK2, dataObject, false, "", window.getBody().dom);
    }

}
