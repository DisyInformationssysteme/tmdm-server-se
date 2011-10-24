package org.talend.mdm.webapp.browserecords.client.widget.treedetail;

import java.util.List;

import org.talend.mdm.webapp.base.shared.TypeModel;
import org.talend.mdm.webapp.browserecords.client.model.ItemNodeModel;
import org.talend.mdm.webapp.browserecords.client.widget.ItemDetailToolBar;
import org.talend.mdm.webapp.browserecords.shared.ViewBean;

import com.extjs.gxt.ui.client.widget.ContentPanel;

public interface ForeignKeyRender {

    public void RenderForeignKey(ItemNodeModel parentModel, List<ItemNodeModel> fkNodeModelList, TypeModel fkTypeModel,
            ItemDetailToolBar toolBar, ViewBean pkViewBean, ContentPanel cp);

    public void removeRelationFkPanel(ItemNodeModel parentModel);
}
