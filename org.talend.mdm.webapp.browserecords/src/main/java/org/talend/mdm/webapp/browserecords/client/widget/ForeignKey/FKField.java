package org.talend.mdm.webapp.browserecords.client.widget.ForeignKey;

import org.talend.mdm.webapp.base.client.model.ForeignKeyBean;
import org.talend.mdm.webapp.browserecords.client.i18n.MessagesFactory;
import org.talend.mdm.webapp.browserecords.client.resources.icon.Icons;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.core.El;
import com.extjs.gxt.ui.client.core.XDOM;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.ComponentHelper;
import com.extjs.gxt.ui.client.widget.button.SplitButton;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

public class FKField extends TextField<ForeignKeyBean> implements ReturnCriteriaFK {

    private SplitButton foreignBtn = new SplitButton();

    private FKRelRecordWindow relWindow = new FKRelRecordWindow();

    private boolean retrieveFKinfos = false;

    public boolean isRetrieveFKinfos() {
        return retrieveFKinfos;
    }

    public void setRetrieveFKinfos(boolean retrieveFKinfos) {
        this.retrieveFKinfos = retrieveFKinfos;
    }

    public FKField() {
        this.setFireChangeEventOnSetValue(true);
        relWindow.setSize(470, 340);
        relWindow.setResizable(false);
        relWindow.setModal(true);
        relWindow.setBlinkModal(true);
    }

    protected void onRender(Element target, int index) {
        El wrap = new El(DOM.createDiv());
        wrap.addStyleName("x-form-field-wrap"); //$NON-NLS-1$
        wrap.addStyleName("x-form-file-wrap"); //$NON-NLS-1$

        input = new El(DOM.createInputText());
        input.addStyleName(fieldStyle);
        input.addStyleName("x-form-file-text"); //$NON-NLS-1$
        input.setId(XDOM.getUniqueId());
        input.setEnabled(false);

        if (GXT.isIE && target.getTagName().equals("TD")) { //$NON-NLS-1$
            input.setStyleAttribute("position", "static"); //$NON-NLS-1$  //$NON-NLS-2$
        }

        foreignBtn.setIcon(AbstractImagePrototype.create(Icons.INSTANCE.link()));
        foreignBtn.addSelectionListener(new SelectionListener<ButtonEvent>() {

            public void componentSelected(ButtonEvent ce) {
                relWindow.show();
            }

        });

        wrap.appendChild(input.dom);
        input.setStyleAttribute("float", "left");//$NON-NLS-1$  //$NON-NLS-2$
        El foreignDiv = new El(DOM.createSpan());
        wrap.appendChild(foreignDiv.dom);

        final SelectionListener<ButtonEvent> closer = new SelectionListener<ButtonEvent>() {

            public void componentSelected(ButtonEvent ce) {
                relWindow.close();
            }

        };

        setElement(wrap.dom, target, index);
        foreignBtn.render(foreignDiv.dom);
        super.onRender(target, index);
    }

    protected void onResize(int width, int height) {
        super.onResize(width, height);
        input.setWidth(width - foreignBtn.getWidth() - 4, true);
    }

    protected void doAttachChildren() {
        super.doAttachChildren();
        ComponentHelper.doAttach(foreignBtn);
    }

    protected void doDetachChildren() {
        super.doDetachChildren();
        ComponentHelper.doDetach(foreignBtn);
    }

    public void Update(String foreignKey, ReturnCriteriaFK returnCriteriaFK) {
        relWindow.setFkKey(foreignKey);
        relWindow.setReturnCriteriaFK(returnCriteriaFK);
        relWindow.setHeading(MessagesFactory.getMessages().fk_RelatedRecord());
    }

    public void setCriteriaFK(final ForeignKeyBean fk) {
        if (retrieveFKinfos) {
            fk.setShowInfo(true);
        }

        setValue(fk);
    }

    public void setEnabled(boolean enabled) {
        foreignBtn.setEnabled(enabled);
    }

    public void setValue(ForeignKeyBean fk) {
        super.setValue(fk);
    }

    public ForeignKeyBean getValue() {
        return value;
    }
}
