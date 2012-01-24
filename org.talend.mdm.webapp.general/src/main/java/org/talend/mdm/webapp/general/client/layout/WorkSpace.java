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
package org.talend.mdm.webapp.general.client.layout;

import java.util.HashMap;
import java.util.Map;

import org.talend.mdm.webapp.general.client.mvc.view.GeneralView;

import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ContainerEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;

public class WorkSpace extends LayoutContainer {

    private static WorkSpace instance;

    private TabPanel workTabPanel = new TabPanel();

    private Map<String, JavaScriptObject> uiMap = new HashMap<String, JavaScriptObject>();

    private WorkSpace() {
        super();
        this.setLayout(new FitLayout());
        workTabPanel.setMinTabWidth(115);
        workTabPanel.setAnimScroll(true);
        workTabPanel.setTabScroll(true);
        workTabPanel.setCloseContextMenu(true);
        this.add(workTabPanel);
        initEvent();
    }

    public static WorkSpace getInstance() {
        if (instance == null) {
            instance = new WorkSpace();
        }
        return instance;
    }

    private void initEvent() {
        workTabPanel.addListener(Events.Resize, new Listener<BaseEvent>() {

            public void handleEvent(BaseEvent be) {
                resizeUIObjects();
            }
        });
        workTabPanel.addListener(Events.Remove, new Listener<ContainerEvent>() {

            public void handleEvent(ContainerEvent be) {
                uiMap.remove(((TabItem) be.getItem()).getItemId());
            }

        });
    }

    public JavaScriptObject getItem(String itemId) {
        return uiMap.get(itemId);
    }

    public void remove(String itemId) {
        TabItem item = workTabPanel.getItemByItemId(itemId);
        if (item != null)
            item.removeFromParent();
    }

    Map<JavaScriptObject, Listener<ContainerEvent<TabPanel, TabItem>>> eventHandler = new HashMap<JavaScriptObject, Listener<ContainerEvent<TabPanel, TabItem>>>();

    public void on(String eventName, final JavaScriptObject handler) {

        if ("beforeremove".equals(eventName)) { //$NON-NLS-1$
            eventHandler.put(handler, new Listener<ContainerEvent<TabPanel, TabItem>>() {

                public void handleEvent(ContainerEvent<TabPanel, TabItem> be) {
                    String id = be.getItem().getItemId();
                    be.setCancelled(!callJs(handler, id));
                }
            });
            workTabPanel.addListener(Events.BeforeRemove, eventHandler.get(handler));
        }
    }

    native boolean callJs(JavaScriptObject handler, String id)/*-{
		var tabPanel = $wnd.amalto.core.getTabPanel();
		var tabItem = {
			getId : function() {
				return id;
			}
		};
		return handler(tabPanel, tabItem);
    }-*/;

    public void un(String eventName, JavaScriptObject handler) {
        if ("beforeremove".equals(eventName)) { //$NON-NLS-1$
            workTabPanel.removeListener(Events.BeforeRemove, eventHandler.get(handler));
            eventHandler.remove(handler);
        }
    }

    public void addWorkTab(final String itemId, final JavaScriptObject uiObject) {

        TabItem item = workTabPanel.getItemByItemId(itemId);

        if (item == null) {
            String text = getTitleUIObject(uiObject);
            if (text == null || text.trim().length() == 0) {
                text = itemId;
            }
            item = new TabItem(text);

            item.addListener(Events.Select, new Listener<BaseEvent>() {

                public void handleEvent(BaseEvent be) {
                    resizeUIObjects();
                }
            });
            item.setItemId(itemId);
            item.setClosable(true);
            item.setLayout(new FitLayout());
            SimplePanel content = new SimplePanel() {

                @Override
                protected void onLoad() {
                    renderUIObject(this.getElement(), uiObject);
                }
            };
            content.getElement().setId("General_" + DOM.createUniqueId()); //$NON-NLS-1$
            item.add(content);
            workTabPanel.add(item);
            uiMap.put(itemId, uiObject);
        }
        workTabPanel.setSelection(item);
    }

    public void updateCurrentTabText(String tabText) {
        TabItem tabItem = workTabPanel.getSelectedItem();
        if (tabItem != null) {
            tabItem.setText(tabText);
        }
    }

    public void closeCurrentTab() {
        TabItem tabItem = workTabPanel.getSelectedItem();
        if (tabItem != null) {
            workTabPanel.remove(tabItem);
        }
    }

    public native String getTitleUIObject(JavaScriptObject uiObject)/*-{
		if (uiObject.title) {
			if (typeof uiObject.title == "string") {
				return uiObject.title;
			} else if (typeof uiObject.title == "function") {
				return uiObject.title();
			}
		}
		return null;
    }-*/;

    public void setSelection(String itemId) {
        TabItem item = workTabPanel.getItemByItemId(itemId);
        if (item != null) {
            workTabPanel.setSelection(item);
        }
    }

    private void resizeUIObjects() {
        TabItem item = workTabPanel.getSelectedItem();
        if (item != null) {
            JavaScriptObject uiObject = uiMap.get(item.getItemId());
            if (uiObject != null) {
                item.layout(true);
                resizeUIObject(uiObject, workTabPanel.getWidth(), item.getHeight());
            }
        }
    }

    native void resizeUIObject(JavaScriptObject uiObject, int width, int height)/*-{
		uiObject.setSize(width, height);
    }-*/;

    private native void renderUIObject(Element el, JavaScriptObject uiObject)/*-{
        if (!!uiObject.getXType){
        el.className = "extpj";
        if (!!uiObject.show){
        var instance = this;
        var _show = uiObject.show;
        uiObject.show = function(){
        instance.@org.talend.mdm.webapp.general.client.layout.WorkSpace::setSelection(Ljava/lang/String;)(uiObject.getItemId());
        _show.call(this);
        };
        }
        }

        uiObject.render(el);
        uiObject.setSize(el.offsetWidth, el.offsetHeight);
        uiObject.doLayout();
    }-*/;

    public void clearTabs() {
        String itemId1 = GeneralView.DSCID;
        String itemId2 = GeneralView.WELCOMEID;
        for (String id : uiMap.keySet()) {
            if (!(itemId1.equals(id) || itemId2.equals(id)))
                workTabPanel.getItemByItemId(id).removeFromParent();
        }
    }

    public native void loadApp(String context, String application)/*-{
		if ($wnd.amalto[context]) {
			if ($wnd.amalto[context][application]) {
				$wnd.amalto[context][application].init();
			}
		}
    }-*/;
}
