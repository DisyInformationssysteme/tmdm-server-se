// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.mdm.webapp.recyclebin.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.talend.mdm.webapp.base.client.SessionAwareAsyncCallback;
import org.talend.mdm.webapp.base.client.i18n.BaseMessagesFactory;
import org.talend.mdm.webapp.base.client.model.BasePagingLoadConfigImpl;
import org.talend.mdm.webapp.base.client.model.ItemBasePageLoadResult;
import org.talend.mdm.webapp.base.client.util.MultilanguageMessageParser;
import org.talend.mdm.webapp.base.client.util.UrlUtil;
import org.talend.mdm.webapp.base.client.widget.ColumnAlignGrid;
import org.talend.mdm.webapp.base.client.widget.PagingToolBarEx;
import org.talend.mdm.webapp.recyclebin.client.i18n.MessagesFactory;
import org.talend.mdm.webapp.recyclebin.client.resources.icon.Icons;
import org.talend.mdm.webapp.recyclebin.shared.ItemsTrashItem;
import org.talend.mdm.webapp.recyclebin.shared.NoPermissionException;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.Registry;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.BasePagingLoadConfig;
import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.PagingLoader;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.state.StateManager;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.grid.CheckBoxSelectionModel;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;

/**
 * DOC Administrator class global comment. Detailled comment
 */
public class MainFramePanel extends ContentPanel {

    private static MainFramePanel instance;

    private RecycleBinServiceAsync service = (RecycleBinServiceAsync) Registry.get(RecycleBin.RECYCLEBIN_SERVICE);

    private TextField<String> text = new TextField<String>();

    private final static int PAGE_SIZE = 20;

    private PagingToolBarEx pagetoolBar = null;

    private Grid<ItemsTrashItem> grid;

    private final Map<String, String> CONCEPT_MODEL_MAP = new HashMap<String, String>();

    private int outstandingRestoreCallCount = 0;

    private int outstandingRestoreCallFailCount = 0;

    private List<ItemsTrashItem> outstandingRestoreCallFailRecords = new LinkedList<ItemsTrashItem>();

    private int outstandingDeleteCallCount = 0;

    private int outstandingDeleteCallFailCount = 0;

    private List<ItemsTrashItem> outstandingDeleteCallFailRecords = new LinkedList<ItemsTrashItem>();

    private static final int COLUMN_WIDTH = 100;

    private MainFramePanel() {
        setLayout(new FitLayout());
        setBodyBorder(false);
        setHeaderVisible(false);
        initGrid();
        initTopBar();
    }

    private void initGrid() {
        List<ColumnConfig> ccList = new ArrayList<ColumnConfig>();
        CheckBoxSelectionModel<ItemsTrashItem> sm = new CheckBoxSelectionModel<ItemsTrashItem>();
        sm.setSelectionMode(SelectionMode.MULTI);
        ccList.add(sm.getColumn());
        ColumnConfig colPK = new ColumnConfig();
        colPK.setId("itemPK");//$NON-NLS-1$
        colPK.setWidth(COLUMN_WIDTH);
        colPK.setHeader(MessagesFactory.getMessages().dataClusterName());
        ccList.add(colPK);
        ColumnConfig colDataModelName = new ColumnConfig();
        colDataModelName.setId("dataModelName");//$NON-NLS-1$
        colDataModelName.setWidth(COLUMN_WIDTH);
        colDataModelName.setHeader(MessagesFactory.getMessages().dataModelName());
        ccList.add(colDataModelName);
        ColumnConfig colRevisionID = new ColumnConfig();
        colRevisionID.setId("revisionID");//$NON-NLS-1$
        colRevisionID.setWidth(COLUMN_WIDTH);
        colRevisionID.setHeader(MessagesFactory.getMessages().revisionID());
        ccList.add(colRevisionID);
        ColumnConfig colConceptName = new ColumnConfig();
        colConceptName.setId("conceptName");//$NON-NLS-1$
        colConceptName.setWidth(COLUMN_WIDTH);
        colConceptName.setHeader(MessagesFactory.getMessages().conceptName());
        ccList.add(colConceptName);
        ColumnConfig colIds = new ColumnConfig();
        colIds.setId("ids");//$NON-NLS-1$
        colIds.setWidth(COLUMN_WIDTH);
        colIds.setHeader(MessagesFactory.getMessages().Ids());
        ccList.add(colIds);
        ColumnConfig colItemName = new ColumnConfig();
        colItemName.setId("itemName");//$NON-NLS-1$
        colItemName.setWidth(COLUMN_WIDTH);
        colItemName.setHeader(MessagesFactory.getMessages().name());
        ccList.add(colItemName);
        ColumnConfig colPartPath = new ColumnConfig();
        colPartPath.setId("partPath");//$NON-NLS-1$
        colPartPath.setWidth(COLUMN_WIDTH);
        colPartPath.setHeader(MessagesFactory.getMessages().partPath());
        ccList.add(colPartPath);
        ColumnConfig colUserName = new ColumnConfig();
        colUserName.setId("insertionUserName");//$NON-NLS-1$
        colUserName.setWidth(COLUMN_WIDTH);
        colUserName.setHeader(MessagesFactory.getMessages().UserName());
        ccList.add(colUserName);
        ColumnConfig colDate = new ColumnConfig();
        colDate.setId("insertionTime");//$NON-NLS-1$
        colDate.setWidth(COLUMN_WIDTH);
        colDate.setHeader(MessagesFactory.getMessages().Date());
        ccList.add(colDate);
        ColumnConfig colProjection = new ColumnConfig("projection", "projection", 0);//$NON-NLS-1$ //$NON-NLS-2$
        colProjection.setHidden(true);
        ccList.add(colProjection);
        ColumnConfig colDelete = new ColumnConfig();
        colDelete.setId("delete");//$NON-NLS-1$
        colDelete.setWidth(50);
        colDelete.setHeader(MessagesFactory.getMessages().delete());
        colDelete.setRenderer(new GridCellRenderer<BaseModelData>() {

            public Object render(final BaseModelData model, String property, ColumnData config, int rowIndex, int colIndex,
                    ListStore<BaseModelData> store, final Grid<BaseModelData> grid) {
                Image image = new Image();
                image.setResource(Icons.INSTANCE.delete());
                image.addStyleName("clickable"); //$NON-NLS-1$
                image.addClickHandler(new ClickHandler() {

                    public void onClick(ClickEvent event) {
                        service.isEntityPhysicalDeletable(
                                model.get("conceptName").toString(), new SessionAwareAsyncCallback<Boolean>() {//$NON-NLS-1$

                                    protected void doOnFailure(Throwable caught) {
                                        String errorMsg = caught.getLocalizedMessage();
                                        if (errorMsg == null || "".equals(errorMsg)) { //$NON-NLS-1$
                                            if (Log.isDebugEnabled())
                                                errorMsg = caught.toString(); // for debugging purpose
                                            else
                                                errorMsg = BaseMessagesFactory.getMessages().unknown_error();
                                        }

                                        if (caught instanceof NoPermissionException) {
                                            MessageBox.alert(BaseMessagesFactory.getMessages().error_title(), MessagesFactory
                                                    .getMessages().restore_no_permissions(), null);
                                            // GWT.log(errorMsg, caught);
                                        } else {
                                            MessageBox.alert(BaseMessagesFactory.getMessages().error_title(), MessagesFactory
                                                    .getMessages().delete_no_permissions(), null);
                                            // GWT.log(errorMsg, caught);
                                        }
                                    }

                                    public void onSuccess(Boolean result) {

                                        MessageBox.confirm(BaseMessagesFactory.getMessages().confirm_title(), MessagesFactory
                                                .getMessages().delete_confirm(), new Listener<MessageBoxEvent>() {

                                            public void handleEvent(MessageBoxEvent be) {
                                                if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {

                                                    if (model.get("projection") != null && !model.get("projection").equals("")) {//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                                                        String[] picArray = model.get("projection").toString().split(//$NON-NLS-1$
                                                                "/imageserver/");//$NON-NLS-1$
                                                        for (int i = 1; i < picArray.length; i++) {
                                                            String array = picArray[i];
                                                            if (!array.isEmpty()) {
                                                                String uri = array.substring(0, array.indexOf("?"));//$NON-NLS-1$

                                                                RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
                                                                        "/imageserver/secure/ImageDeleteServlet?uri=" + uri);//$NON-NLS-1$
                                                                builder.setCallback(new RequestCallback() {

                                                                    public void onResponseReceived(Request request,
                                                                            Response response) {
                                                                        // TODO result
                                                                    }

                                                                    public void onError(Request request, Throwable e) {
                                                                        MessageBox.alert(BaseMessagesFactory.getMessages()
                                                                                .error_title(), e.getMessage(), null);
                                                                    }
                                                                });

                                                                try {
                                                                    builder.send();
                                                                } catch (RequestException e) {
                                                                    MessageBox.alert(BaseMessagesFactory.getMessages()
                                                                            .error_title(), e.getMessage(), null);
                                                                }
                                                            }
                                                        }
                                                    }
                                                    deleteItem(model);
                                                }
                                            }

                                        });
                                    }

                                });

                    }

                });
                return image;
            }

        });
        ccList.add(colDelete);
        ColumnConfig colRestore = new ColumnConfig();
        colRestore.setId("restore");//$NON-NLS-1$
        colRestore.setWidth(50);
        colRestore.setHeader(MessagesFactory.getMessages().restore());
        colRestore.setRenderer(new GridCellRenderer<BaseModelData>() {

            public Object render(final BaseModelData model, String property, ColumnData config, int rowIndex, int colIndex,
                    ListStore<BaseModelData> store, final Grid<BaseModelData> grid) {
                Image image = new Image();
                image.setResource(Icons.INSTANCE.restore());
                image.addStyleName("clickable"); //$NON-NLS-1$
                image.addClickHandler(new ClickHandler() {

                    public void onClick(ClickEvent event) {
                        MessageBox.confirm(BaseMessagesFactory.getMessages().confirm_title(), MessagesFactory.getMessages()
                                .restore_confirm(), new Listener<MessageBoxEvent>() {

                            public void handleEvent(MessageBoxEvent be) {
                                if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {
                                    service.checkConflict(model.get("itemPK").toString(), model //$NON-NLS-1$
                                            .get("conceptName").toString(), model.get("ids").toString(), //$NON-NLS-1$ //$NON-NLS-2$
                                            new SessionAwareAsyncCallback<Boolean>() {

                                                public void onSuccess(Boolean result) {
                                                    if (result) {
                                                        MessageBox.confirm(BaseMessagesFactory.getMessages().confirm_title(),
                                                                MessagesFactory.getMessages().overwrite_confirm(),
                                                                new Listener<MessageBoxEvent>() {

                                                                    public void handleEvent(MessageBoxEvent be) {
                                                                        if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {
                                                                            recoverDroppedItem(model, grid);
                                                                        }
                                                                    }
                                                                });
                                                    } else {
                                                        recoverDroppedItem(model, grid);
                                                    }
                                                }
                                            });
                                }
                            }

                        });

                    }

                });
                return image;
            }

        });
        ccList.add(colRestore);
        ColumnModel cm = new ColumnModel(ccList);

        RpcProxy<PagingLoadResult<ItemsTrashItem>> proxy = new RpcProxy<PagingLoadResult<ItemsTrashItem>>() {

            @Override
            protected void load(Object loadConfig, final AsyncCallback<PagingLoadResult<ItemsTrashItem>> callback) {
                BasePagingLoadConfigImpl baseConfig = BasePagingLoadConfigImpl.copyPagingLoad((PagingLoadConfig) loadConfig);
                service.getTrashItems(text.getValue() == null ? "*" : text.getValue(), baseConfig,//$NON-NLS-1$
                        new SessionAwareAsyncCallback<ItemBasePageLoadResult<ItemsTrashItem>>() {

                            public void onSuccess(ItemBasePageLoadResult<ItemsTrashItem> result) {
                                for (ItemsTrashItem trashItem : result.getData()) {
                                    CONCEPT_MODEL_MAP.put(trashItem.getConceptName(), trashItem.getDataModelName());
                                }
                                callback.onSuccess(new BasePagingLoadResult<ItemsTrashItem>(result.getData(), result.getOffset(),
                                        result.getTotalLength()));
                            }

                            @Override
                            protected void doOnFailure(Throwable caught) {
                                super.doOnFailure(caught);
                                callback.onSuccess(new BasePagingLoadResult<ItemsTrashItem>(new ArrayList<ItemsTrashItem>(), 0, 0));
                            }
                        });
            }

        };

        // loader
        final PagingLoader<PagingLoadResult<ItemsTrashItem>> loader = new BasePagingLoader<PagingLoadResult<ItemsTrashItem>>(
                proxy);

        final ListStore<ItemsTrashItem> store = new ListStore<ItemsTrashItem>(loader);
        grid = new ColumnAlignGrid<ItemsTrashItem>(store, cm);
        grid.getView().setAutoFill(true);
        grid.getView().setForceFit(true);
        // grid.setSize(350, 600);
        int usePageSize = PAGE_SIZE;
        if (StateManager.get().get("trashgrid") != null) //$NON-NLS-1$
            usePageSize = Integer.valueOf(((Map<?, ?>) StateManager.get().get("trashgrid")).get("limit").toString()); //$NON-NLS-1$ //$NON-NLS-2$
        pagetoolBar = new PagingToolBarEx(usePageSize);
        pagetoolBar.bind(loader);
        grid.setLoadMask(true);
        grid.setStateId("trashgrid");//$NON-NLS-1$
        grid.setStateful(true);
        grid.addListener(Events.Attach, new Listener<GridEvent<ItemsTrashItem>>() {

            public void handleEvent(GridEvent<ItemsTrashItem> be) {
                PagingLoadConfig config = new BasePagingLoadConfig();
                config.setOffset(0);
                int pageSize = (Integer) pagetoolBar.getPageSize();
                config.setLimit(pageSize);
                loader.load(config);
            }
        });
        grid.setSelectionModel(sm);
        grid.addPlugin(sm);
        this.setBottomComponent(pagetoolBar);
        add(grid);
    }

    private void initTopBar() {
        ToolBar bar = new ToolBar();
        text.setId("trash-criteria");//$NON-NLS-1$
        text.setEmptyText("*");//$NON-NLS-1$        
        bar.add(text);
        final Button btn = new Button(MessagesFactory.getMessages().search());
        btn.setId("search");//$NON-NLS-1$
        btn.setToolTip(MessagesFactory.getMessages().serarch_tooltip());
        btn.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                PagingLoadConfig config = new BasePagingLoadConfig();
                config.setOffset(0);
                int pageSize = (Integer) pagetoolBar.getPageSize();
                config.setLimit(pageSize);
                grid.getStore().getLoader().load(config);
            }

        });
        text.addListener(Events.KeyDown, new Listener<FieldEvent>() {

            public void handleEvent(FieldEvent e) {
                if (e.getKeyCode() == KeyCodes.KEY_ENTER) {
                    if (btn != null) {
                        btn.fireEvent(Events.Select);
                    }
                }
            }
        });
        bar.add(btn);

        bar.add(new SeparatorToolItem());
        final Button restoreSelectedBtn = new Button(MessagesFactory.getMessages().restoreSelected());
        restoreSelectedBtn.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.restore()));
        restoreSelectedBtn.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                MessageBox.confirm(BaseMessagesFactory.getMessages().confirm_title(), MessagesFactory.getMessages()
                        .restoreSelectedConfirm(), new Listener<MessageBoxEvent>() {

                    public void handleEvent(MessageBoxEvent be) {

                        if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {
                            restoreSelected();
                        }
                    }
                });
            }

        });
        bar.add(restoreSelectedBtn);

        bar.add(new SeparatorToolItem());
        final Button deleteSelectedBtn = new Button(MessagesFactory.getMessages().deleteSelected());
        deleteSelectedBtn.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.delete()));
        deleteSelectedBtn.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                MessageBox.confirm(BaseMessagesFactory.getMessages().confirm_title(), MessagesFactory.getMessages()
                        .deleteSelectedConfirm(), new Listener<MessageBoxEvent>() {

                    public void handleEvent(MessageBoxEvent be) {

                        if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {
                            deleteSelected();
                        }
                    }
                });
            }

        });
        bar.add(deleteSelectedBtn);

        this.setTopComponent(bar);
    }

    public void restoreSelected() {
        if (grid != null) {
            GridSelectionModel<ItemsTrashItem> sm = grid.getSelectionModel();

            if (sm != null) {
                List<ItemsTrashItem> selectedRecords = sm.getSelectedItems();

                if (selectedRecords != null && selectedRecords.size() > 0) {

                    for (final ItemsTrashItem r : selectedRecords) {

                        ++outstandingRestoreCallCount;

                        service.checkConflict(r.get("itemPK").toString(), r //$NON-NLS-1$
                                .get("conceptName").toString(), r.get("ids").toString(), //$NON-NLS-1$ //$NON-NLS-2$
                                new AsyncCallback<Boolean>() {

                                    public void onSuccess(Boolean result) {
                                        if (result) {
                                            MessageBox.confirm(BaseMessagesFactory.getMessages().confirm_title(), MessagesFactory
                                                    .getMessages().restoreSelectedOverwriteConfirm(r.get("ids").toString()), //$NON-NLS-1$
                                                    new Listener<MessageBoxEvent>() {

                                                        public void handleEvent(MessageBoxEvent be) {
                                                            if (be.getButtonClicked().getItemId().equals(Dialog.YES)) {
                                                                restoreSelectedItem(r);
                                                            } else {
                                                                restoreSelectedCheckFinished(r, false);
                                                            }
                                                        }
                                                    });
                                        } else {
                                            restoreSelectedItem(r);
                                        }
                                    }

                                    public void onFailure(Throwable arg0) {
                                        restoreSelectedCheckFinished(r, false);
                                    }
                                });
                    }
                }
            }
        }
    }

    public void restoreSelectedItem(final ItemsTrashItem r) {
        String conceptName = r.get("conceptName").toString(); //$NON-NLS-1$
        String modelName = CONCEPT_MODEL_MAP.get(conceptName);
        service.recoverDroppedItem(r.get("itemPK").toString(), r.get("partPath").toString(),//$NON-NLS-1$//$NON-NLS-2$
                r.get("revisionId") == null ? null : r.get("revisionId").toString(), //$NON-NLS-1$ //$NON-NLS-2$
                conceptName, modelName, r.get("ids").toString(),//$NON-NLS-1$
                new AsyncCallback<Void>() {

                    public void onSuccess(Void arg0) {
                        restoreSelectedCheckFinished(r, true);
                    }

                    public void onFailure(Throwable caught) {
                        restoreSelectedCheckFinished(r, false);
                    }
                });
    }

    public void restoreSelectedCheckFinished(ItemsTrashItem r, boolean success) {
        --outstandingRestoreCallCount;

        if (!success) {
            ++outstandingRestoreCallFailCount;
            outstandingRestoreCallFailRecords.add(r);
        }

        if (success && grid != null && grid.getStore() != null) {
            grid.getStore().remove(r);
        }

        if (outstandingRestoreCallCount == 0) {
            pagetoolBar.refresh();

            if (outstandingRestoreCallFailCount > 0) {
                StringBuffer buf = new StringBuffer();
                boolean loopBegin = true;
                for (ItemsTrashItem item : outstandingRestoreCallFailRecords) {
                    buf.append((loopBegin ? " " : ", ") + item.get("ids")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    loopBegin = false;
                }
                MessageBox.alert(BaseMessagesFactory.getMessages().error_title(), MessagesFactory.getMessages()
                        .restoreSelectedError(outstandingRestoreCallFailCount, buf.toString()), null);

                outstandingRestoreCallFailCount = 0;
                outstandingRestoreCallFailRecords.clear();
            }

            refreshBrowseRecordsGrid();
        }
    }

    public void deleteSelected() {
        if (grid != null) {
            GridSelectionModel<ItemsTrashItem> sm = grid.getSelectionModel();

            if (sm != null) {
                List<ItemsTrashItem> selectedRecords = sm.getSelectedItems();

                if (selectedRecords != null && selectedRecords.size() > 0) {

                    for (final ItemsTrashItem r : selectedRecords) {

                        ++outstandingDeleteCallCount;

                        service.isEntityPhysicalDeletable(
                                r.get("conceptName").toString(), new SessionAwareAsyncCallback<Boolean>() {//$NON-NLS-1$

                                    protected void doOnFailure(Throwable caught) {
                                        deleteSelectedCheckFinished(r, false, caught.getMessage());
                                    }

                                    public void onSuccess(Boolean result) {
                                        if (r.get("projection") != null && !r.get("projection").equals("")) {//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
                                            String[] picArray = r.get("projection").toString().split(//$NON-NLS-1$
                                                    "/imageserver/");//$NON-NLS-1$
                                            for (int i = 1; i < picArray.length; i++) {
                                                String array = picArray[i];
                                                if (!array.isEmpty()) {
                                                    String uri = array.substring(0, array.indexOf("?"));//$NON-NLS-1$

                                                    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
                                                            "/imageserver/secure/ImageDeleteServlet?uri=" + uri);//$NON-NLS-1$
                                                    builder.setCallback(new RequestCallback() {

                                                        public void onResponseReceived(Request request, Response response) {
                                                        }

                                                        public void onError(Request request, Throwable e) {
                                                        }
                                                    });

                                                    try {
                                                        builder.send();
                                                    } catch (RequestException e) {
                                                    }
                                                }
                                            }
                                        }

                                        service.removeDroppedItem(
                                                r.get("itemPK").toString(), r.get("partPath").toString(),//$NON-NLS-1$//$NON-NLS-2$
                                                r.get("revisionId") == null ? null : r.get("revisionId").toString(), r //$NON-NLS-1$//$NON-NLS-2$
                                                        .get("conceptName").toString(), r.get("ids").toString(), UrlUtil.getLanguage(), //$NON-NLS-1$//$NON-NLS-2$
                                                new SessionAwareAsyncCallback<String>() {

                                                    public void onSuccess(String msg) {
                                                        deleteSelectedCheckFinished(r, true, msg);
                                                    }

                                                    @Override
                                                    protected void doOnFailure(Throwable caught) {
                                                        String errorMsg = caught.getLocalizedMessage();
                                                        if (errorMsg == null) {
                                                            if (Log.isDebugEnabled())
                                                                errorMsg = caught.toString(); // for debugging
                                                            // purpose
                                                            else
                                                                errorMsg = BaseMessagesFactory.getMessages().unknown_error();
                                                        }
                                                        deleteSelectedCheckFinished(r, false, errorMsg);
                                                    }
                                                });
                                    }
                                });
                    }
                }
            }
        }
    }

    public void deleteSelectedCheckFinished(ItemsTrashItem r, boolean success, String msg) {
        --outstandingDeleteCallCount;

        if (!success) {
            ++outstandingDeleteCallFailCount;
            outstandingDeleteCallFailRecords.add(r);
        }

        if (success && grid != null && grid.getStore() != null) {
            grid.getStore().remove(r);
        }

        if (outstandingDeleteCallCount == 0) {
            pagetoolBar.refresh();

            if (outstandingDeleteCallFailCount > 0) {
                outstandingDeleteCallFailCount = 0;
                outstandingDeleteCallFailRecords.clear();
            }

            if (msg != null) {
                MessageBox.info(BaseMessagesFactory.getMessages().info_title(),
                        MultilanguageMessageParser.pickOutISOMessage(msg), null);
            }
        }
    }

    private void deleteItem(final BaseModelData model) {

        service.removeDroppedItem(model.get("itemPK").toString(), model.get("partPath").toString(),//$NON-NLS-1$//$NON-NLS-2$
                model.get("revisionId") == null ? null : model.get("revisionId").toString(), model //$NON-NLS-1$//$NON-NLS-2$
                        .get("conceptName").toString(), model.get("ids").toString(), UrlUtil.getLanguage(),//$NON-NLS-1$//$NON-NLS-2$
                new SessionAwareAsyncCallback<String>() {

                    public void onSuccess(String msg) {
                        pagetoolBar.refresh();
                        grid.getStore().remove((ItemsTrashItem) model);
                        if (msg != null) {
                            MessageBox.info(BaseMessagesFactory.getMessages().info_title(),
                                    MultilanguageMessageParser.pickOutISOMessage(msg), null);
                        }
                    }

                    @Override
                    protected void doOnFailure(Throwable caught) {
                        String errorMsg = caught.getLocalizedMessage();
                        if (errorMsg == null) {
                            if (Log.isDebugEnabled())
                                errorMsg = caught.toString(); // for debugging purpose
                            else
                                errorMsg = BaseMessagesFactory.getMessages().unknown_error();
                        }
                        MessageBox.alert(BaseMessagesFactory.getMessages().error_title(),
                                MultilanguageMessageParser.pickOutISOMessage(errorMsg), null);
                    }
                });
    }

    public static MainFramePanel getInstance() {
        if (instance == null)
            instance = new MainFramePanel();
        return instance;
    }

    public void recoverDroppedItem(final BaseModelData model, final Grid<BaseModelData> grid) {
        String conceptName = model.get("conceptName").toString(); //$NON-NLS-1$
        String modelName = CONCEPT_MODEL_MAP.get(conceptName);
        service.recoverDroppedItem(model.get("itemPK").toString(), model.get("partPath").toString(),//$NON-NLS-1$//$NON-NLS-2$
                model.get("revisionId") == null ? null : model.get("revisionId").toString(), //$NON-NLS-1$ //$NON-NLS-2$
                conceptName, modelName, model.get("ids").toString(),//$NON-NLS-1$
                new SessionAwareAsyncCallback<Void>() {

                    public void onSuccess(Void arg0) {
                        pagetoolBar.refresh();
                        grid.getStore().remove(model);
                        refreshBrowseRecordsGrid();
                    }

                    protected void doOnFailure(Throwable caught) {
                        String errorMsg = caught.getLocalizedMessage();
                        if (errorMsg == null || "".equals(errorMsg)) { //$NON-NLS-1$
                            if (Log.isDebugEnabled())
                                errorMsg = caught.toString(); // for debugging purpose
                            else
                                errorMsg = BaseMessagesFactory.getMessages().unknown_error();
                        }

                        if (caught instanceof NoPermissionException) {
                            MessageBox.alert(BaseMessagesFactory.getMessages().error_title(), MessagesFactory.getMessages()
                                    .restore_no_permissions(), null);
                            // GWT.log(errorMsg, caught);
                        } else {
                            MessageBox.alert(BaseMessagesFactory.getMessages().error_title(), errorMsg, null);
                            // GWT.log(errorMsg, caught);
                        }
                    }
                });
    }

    private native void refreshBrowseRecordsGrid()/*-{
        $wnd.amalto.browserecords.BrowseRecords.refreshGrid();
    }-*/;
}
