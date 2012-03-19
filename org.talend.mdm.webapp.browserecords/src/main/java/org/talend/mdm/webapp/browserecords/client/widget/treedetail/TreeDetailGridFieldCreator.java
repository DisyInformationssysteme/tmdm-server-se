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
package org.talend.mdm.webapp.browserecords.client.widget.treedetail;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.talend.mdm.webapp.base.client.model.DataTypeConstants;
import org.talend.mdm.webapp.base.client.model.ForeignKeyBean;
import org.talend.mdm.webapp.base.shared.FacetModel;
import org.talend.mdm.webapp.base.shared.SimpleTypeModel;
import org.talend.mdm.webapp.base.shared.TypeModel;
import org.talend.mdm.webapp.browserecords.client.BrowseRecordsEvents;
import org.talend.mdm.webapp.browserecords.client.i18n.MessagesFactory;
import org.talend.mdm.webapp.browserecords.client.model.ComboBoxModel;
import org.talend.mdm.webapp.browserecords.client.model.ItemNodeModel;
import org.talend.mdm.webapp.browserecords.client.mvc.BrowseRecordsView;
import org.talend.mdm.webapp.browserecords.client.util.DateUtil;
import org.talend.mdm.webapp.browserecords.client.util.Locale;
import org.talend.mdm.webapp.browserecords.client.widget.ItemDetailToolBar;
import org.talend.mdm.webapp.browserecords.client.widget.ItemsDetailPanel;
import org.talend.mdm.webapp.browserecords.client.widget.ForeignKey.FKPropertyEditor;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.BooleanField;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.ComboBoxField;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.ForeignKeyField;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.FormatDateField;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.FormatNumberField;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.FormatTextField;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.PictureField;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.UrlField;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.validator.NumberFieldValidator;
import org.talend.mdm.webapp.browserecords.client.widget.inputfield.validator.TextFieldValidator;
import org.talend.mdm.webapp.browserecords.shared.ComplexTypeModel;
import org.talend.mdm.webapp.browserecords.shared.FacetEnum;

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.mvc.AppEvent;
import com.extjs.gxt.ui.client.mvc.Dispatcher;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.DateField;
import com.extjs.gxt.ui.client.widget.form.DateTimePropertyEditor;
import com.extjs.gxt.ui.client.widget.form.Field;
import com.extjs.gxt.ui.client.widget.form.NumberField;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.google.gwt.user.client.ui.Widget;

public class TreeDetailGridFieldCreator {

    public static Field<?> createField(ItemNodeModel node, final TypeModel dataType, String language,
            Map<String, Field<?>> fieldMap, String operation, final ItemsDetailPanel itemsDetailPanel) {
        // Field

        Serializable value = node.getObjectValue();
        Field<?> field;
        boolean hasValue = value != null && !"".equals(value); //$NON-NLS-1$
        if (dataType.getForeignkey() != null) {
            ForeignKeyField fkField = new ForeignKeyField(dataType.getForeignkey(), dataType.getForeignKeyInfo(), itemsDetailPanel);
            if (value instanceof ForeignKeyBean) {
                ForeignKeyBean fkBean = (ForeignKeyBean) value;
                if (fkBean != null) {
                    // fkBean.setDisplayInfo(fkBean.getId());
                    fkField.setValue(fkBean);
                    fkField.setPropertyEditor(new FKPropertyEditor());
                }
            }
            field = fkField;
        } else if (dataType.hasEnumeration()) {
            SimpleComboBox<String> comboBox = new SimpleComboBox<String>();
            comboBox.setFireChangeEventOnSetValue(true);
            if (dataType.getMinOccurs() > 0)
                comboBox.setAllowBlank(false);
            comboBox.setEditable(false);
            comboBox.setForceSelection(true);
            comboBox.setTriggerAction(TriggerAction.ALL);
            setEnumerationValues(dataType, comboBox);
            comboBox.setSimpleValue(hasValue ? value.toString() : ""); //$NON-NLS-1$
            field = comboBox;

        } else if (dataType.getType().equals(DataTypeConstants.UUID)) {
            TextField<String> uuidField = new TextField<String>();
            uuidField.setEnabled(false);
            uuidField.setReadOnly(true);
            if (hasValue)
                uuidField.setValue(value.toString());
            field = uuidField;
        } else if (dataType.getType().equals(DataTypeConstants.AUTO_INCREMENT)) {
            TextField<String> autoIncrementField = new TextField<String>();
            autoIncrementField.setEnabled(false);
            autoIncrementField.setReadOnly(true);
            if (hasValue) {
                autoIncrementField.setValue(value.toString());
            } else {
                autoIncrementField.setValue(MessagesFactory.getMessages().auto());
            }
            field = autoIncrementField;
        } else if (dataType.getType().equals(DataTypeConstants.PICTURE)) {
            PictureField pictureField = new PictureField();
            pictureField.setValue(hasValue ? value.toString() : ""); //$NON-NLS-1$
            field = pictureField;
        } else if (dataType.getType().equals(DataTypeConstants.URL)) {
            UrlField urlField = new UrlField();
            urlField.setFieldLabel(dataType.getLabel(language));
            urlField.setValue(hasValue ? value.toString() : ""); //$NON-NLS-1$
            field = urlField;
        } else if (dataType instanceof ComplexTypeModel) {
            final ComboBoxField<ComboBoxModel> comboxField = new ComboBoxField<ComboBoxModel>();
            comboxField.setDisplayField("value"); //$NON-NLS-1$
            comboxField.setValueField("value"); //$NON-NLS-1$
            comboxField.setTypeAhead(true);
            comboxField.setTriggerAction(TriggerAction.ALL);

            // final ComplexTypeModel complexTypeModel = (ComplexTypeModel) dataType;
            List<ComplexTypeModel> reusableTypes = ((ComplexTypeModel) dataType).getReusableComplexTypes();
            ListStore<ComboBoxModel> comboxStore = new ListStore<ComboBoxModel>();
            comboxField.setStore(comboxStore);
            for (int i = 0; i < reusableTypes.size(); i++) {
                ComboBoxModel cbm;
                if (dataType.isAbstract() && i == 0) {
                    cbm = new ComboBoxModel(reusableTypes.get(i).getName(),
                            "[" + MessagesFactory.getMessages().abstract_type() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    cbm = new ComboBoxModel(reusableTypes.get(i).getName(), reusableTypes.get(i).getName());
                }
                cbm.set("reusableType", reusableTypes.get(i)); //$NON-NLS-1$
                comboxStore.add(cbm);
            }
            if (comboxStore.getCount() > 0) {
                comboxField.setValue(comboxStore.getAt(0));
            }

            if (node.getRealType() != null && node.getRealType().trim().length() > 0) {
                comboxField.setValue(comboxStore.findModel("value", node.getRealType())); //$NON-NLS-1$
            } else if (hasValue) {
                comboxField.setValue(comboxStore.findModel(value.toString(), value));
            }

            comboxField.addSelectionChangedListener(new SelectionChangedListener<ComboBoxModel>() {

                @Override
                public void selectionChanged(SelectionChangedEvent<ComboBoxModel> se) {
                    ComplexTypeModel reusableType = se.getSelectedItem().get("reusableType"); //$NON-NLS-1$
                    AppEvent app = new AppEvent(BrowseRecordsEvents.UpdatePolymorphism);
                    app.setData(reusableType);
                    app.setData(BrowseRecordsView.ITEMS_DETAIL_PANEL, itemsDetailPanel);
                    Dispatcher.forwardEvent(app);
                }

            });

            field = comboxField;
        } else {
            field = createCustomField(node, dataType, language);
        }

        field.setFieldLabel(dataType.getLabel(language));
        field.setName(dataType.getXpath());
        if (!dataType.getType().equals(DataTypeConstants.UUID) && !dataType.getType().equals(DataTypeConstants.AUTO_INCREMENT)) {
            field.setReadOnly(dataType.isReadOnly());
            field.setEnabled(!dataType.isReadOnly());
        }

        if (node.isKey() && hasValue && ItemDetailToolBar.DUPLICATE_OPERATION.equals(operation)) {
            field.setEnabled(true);
            field.setReadOnly(false);
        } else if (node.isKey() && hasValue && ItemDetailToolBar.CREATE_OPERATION.equals(operation)) {
            field.setEnabled(true);
            field.setReadOnly(false);
        } else if (node.isKey() && hasValue) {
            field.setEnabled(false);
            field.setReadOnly(true);
        }

        // facet set
        if (field instanceof TextField<?> && !(dataType instanceof ComplexTypeModel)) {
            buildFacets(dataType, field);

            String errorMsg = dataType.getFacetErrorMsgs().get(language);
            field.setData("facetErrorMsgs", errorMsg);//$NON-NLS-1$        
            FacetEnum.setFacetValue("maxOccurence", (Widget) field, String.valueOf(dataType.getMaxOccurs())); //$NON-NLS-1$

            if (((TextField<?>) field).getValidator() == null)
                ((TextField<?>) field).setValidator(TextFieldValidator.getInstance());

            if (errorMsg != null && !errorMsg.equals("")) //$NON-NLS-1$                
                ((TextField<?>) field).getMessages().setBlankText(errorMsg);

        }
        fieldMap.put(node.getId().toString(), field);
        updateMandatory(field, node, fieldMap);
        addFieldListener(field, node, fieldMap);
        return field;
    }

    public static Field<?> createField(ItemNodeModel node, final TypeModel dataType, String language,
            Map<String, Field<?>> fieldMap, final ItemsDetailPanel itemsDetailPanel) {
        return createField(node, dataType, language, fieldMap, null, itemsDetailPanel);
    }

    public static Field<?> createCustomField(ItemNodeModel node, TypeModel dataType, String language) {
        Serializable value = node.getObjectValue();
        String pattern = dataType.getDisplayFomats().get("format_" + Locale.getLanguage()); //$NON-NLS-1$
        Field<?> field;
        boolean hasValue = value != null && !"".equals(value); //$NON-NLS-1$
        String baseType = dataType.getType().getBaseTypeName();
        if (DataTypeConstants.INTEGER.getTypeName().equals(baseType) || DataTypeConstants.INT.getTypeName().equals(baseType)
                || DataTypeConstants.LONG.getTypeName().equals(baseType)) {
            FormatNumberField numberField = new FormatNumberField();
            if (pattern != null && !"".equals(pattern)) { //$NON-NLS-1$
                numberField.setFormatPattern(pattern);
            }
            numberField.setData("numberType", "integer");//$NON-NLS-1$ //$NON-NLS-2$
            numberField.setPropertyEditorType(Integer.class);
            numberField.setValidator(NumberFieldValidator.getInstance());
            if (pattern != null && !"".equals(pattern)) { //$NON-NLS-1$     
                // numberField.setPropertyEditor(new NumberPropertyEditor(pattern));
            }
            numberField.setValue((hasValue ? Long.parseLong(value.toString()) : null));
            field = numberField;
        } else if (DataTypeConstants.FLOAT.getTypeName().equals(baseType)
                || DataTypeConstants.DOUBLE.getTypeName().equals(baseType)) {
            FormatNumberField numberField = new FormatNumberField();
            if (pattern != null && !"".equals(pattern)) { //$NON-NLS-1$
                numberField.setFormatPattern(pattern);
            }
            numberField.setData("numberType", "double");//$NON-NLS-1$ //$NON-NLS-2$
            numberField.setPropertyEditorType(Double.class);
            numberField.setValidator(NumberFieldValidator.getInstance());
            if (DataTypeConstants.DOUBLE.getTypeName().equals(baseType))
                numberField.setValue((hasValue ? Double.parseDouble(value.toString()) : null));
            else
                numberField.setValue((hasValue ? Float.parseFloat(value.toString()) : null));
            field = numberField;
        } else if (DataTypeConstants.DECIMAL.getTypeName().equals(baseType)) {
            FormatNumberField numberField = new FormatNumberField();
            if (pattern != null && !"".equals(pattern)) { //$NON-NLS-1$
                numberField.setFormatPattern(pattern);
            }
            numberField.setData("numberType", "decimal");//$NON-NLS-1$ //$NON-NLS-2$
            numberField.setValidator(NumberFieldValidator.getInstance());
            numberField.setPropertyEditorType(Double.class);
            numberField.setValue((hasValue ? Double.parseDouble(value.toString()) : null));

            field = numberField;
        } else if (DataTypeConstants.BOOLEAN.getTypeName().equals(baseType)) {
            CheckBox checkBox = new CheckBox();
            checkBox.setValue(hasValue ? ((value.toString().equals("true") || value.equals(true)) ? true : false) : null); //$NON-NLS-1$
            field = checkBox;
        } else if (DataTypeConstants.DATE.getTypeName().equals(baseType)) {
            FormatDateField dateField = new FormatDateField(node);
            if (pattern != null && !"".equals(pattern)) { //$NON-NLS-1$                
                dateField.setFormatPattern(pattern);
                dateField.setShowFormateValue(true);
            }
                dateField.setPropertyEditor(new DateTimePropertyEditor(DateUtil.datePattern));
                    
            
            if (hasValue)
                dateField.setValue(hasValue ? DateUtil.convertStringToDate(value.toString()) : null);

            field = dateField;
        } else if (DataTypeConstants.DATETIME.getTypeName().equals(baseType)) {
            FormatDateField dateTimeField = new FormatDateField(node);
            if (pattern != null && !"".equals(pattern)) { //$NON-NLS-1$
                dateTimeField.setFormatPattern(pattern);
            }
            dateTimeField.setPropertyEditor(new DateTimePropertyEditor(DateUtil.formatDateTimePattern));
            if (hasValue)
                dateTimeField
                        .setValue(hasValue ? DateUtil.convertStringToDate(DateUtil.dateTimePattern, value.toString()) : null);

            field = dateTimeField;
        } else if (DataTypeConstants.STRING.getTypeName().equals(baseType)) {
            FormatTextField textField = new FormatTextField();
            if (pattern != null && !"".equals(pattern)) { //$NON-NLS-1$
                textField.setFormatPattern(pattern);
            }
            textField.setValidator(TextFieldValidator.getInstance());
            textField.setValue(hasValue ? value.toString() : ""); //$NON-NLS-1$

            field = textField;
        } else {
            FormatTextField textField = new FormatTextField();
            if (pattern != null && !"".equals(pattern)) { //$NON-NLS-1$
                textField.setFormatPattern(pattern);
            }
            textField.setValue(hasValue ? value.toString() : ""); //$NON-NLS-1$
            textField.setValidator(TextFieldValidator.getInstance());
            field = textField;
        }

        field.setWidth(400);
        return field;
    }

    public static void deleteField(ItemNodeModel node, Map<String, Field<?>> fieldMap) {

        Field<?> updateField = fieldMap.get(node.getId().toString());
        node.setObjectValue(null);
        updateMandatory(updateField, node, fieldMap);
        fieldMap.remove(node.getId().toString());
    }

    private static void addFieldListener(final Field<?> field, final ItemNodeModel node, final Map<String, Field<?>> fieldMap) {

        field.addListener(Events.Change, new Listener<FieldEvent>() {

            @SuppressWarnings("rawtypes")
            public void handleEvent(FieldEvent fe) {
                if (fe.getField() instanceof ComboBoxField) {
                    node.setObjectValue(((ComboBoxModel) fe.getValue()).getValue());
                } else if (fe.getField() instanceof CheckBox) {
                    node.setObjectValue(fe.getValue().toString());
                } else {
                    node.setObjectValue(fe.getField() instanceof ComboBox ? ((SimpleComboValue) fe.getValue()).getValue()
                            .toString() : (Serializable) fe.getValue());
                }
                node.setChangeValue(true);

                validate(fe.getField(), node);

                updateMandatory(field, node, fieldMap);
            }
        });

        field.addListener(Events.Attach, new Listener<FieldEvent>() {

            public void handleEvent(FieldEvent fe) {
                validate(field, node);
            }
        });

        field.addListener(Events.Blur, new Listener<FieldEvent>() {

            public void handleEvent(FieldEvent fe) {
            	// TMDM-3353 only when node is valid, call setObjectValue(); otherwise objectValue is changed to
                // original value
                if (node.isValid())
	                if (fe.getField() instanceof FormatTextField) {
	                    node.setObjectValue(((FormatTextField) fe.getField()).getOjbectValue());
	                } else if (fe.getField() instanceof FormatNumberField) {
	                    node.setObjectValue(((FormatNumberField) fe.getField()).getOjbectValue());
	                } else if (fe.getField() instanceof FormatDateField) {
	                    node.setObjectValue(((FormatDateField) fe.getField()).getOjbectValue());
	                }
            }
        });
    }

    private static void buildFacets(TypeModel typeModel, Widget w) {
        if (typeModel instanceof SimpleTypeModel) {
            List<FacetModel> facets = ((SimpleTypeModel) typeModel).getFacets();
            for (FacetModel facet : facets) {
                FacetEnum.setFacetValue(facet.getName(), w, facet.getValue());
            }
        }
    }

    private static void setEnumerationValues(TypeModel typeModel, Widget w) {
        List<String> enumeration = ((SimpleTypeModel) typeModel).getEnumeration();
        if (enumeration != null && enumeration.size() > 0) {
            @SuppressWarnings("unchecked")
            SimpleComboBox<String> field = (SimpleComboBox<String>) w;
            for (String value : enumeration) {
                field.add(value);
            }
        }
    }

    public static void updateMandatory(Field<?> field, ItemNodeModel node, Map<String, Field<?>> fieldMap) {

        boolean flag = false;
        ItemNodeModel parent = (ItemNodeModel) node.getParent();
        if (parent != null && parent.getParent() != null && !parent.isMandatory()) {
            List<ModelData> childs = parent.getChildren();
            for (int i = 0; i < childs.size(); i++) {
                ItemNodeModel child = (ItemNodeModel) childs.get(i);
                if (child.getObjectValue() != null && !"".equals(child.getObjectValue())) { //$NON-NLS-1$
                    flag = true;
                    break;
                }
            }

            for (int i = 0; i < childs.size(); i++) {
                ItemNodeModel mandatoryNode = (ItemNodeModel) childs.get(i);
                Field<?> updateField = fieldMap.get(mandatoryNode.getId().toString());
                if (updateField != null && mandatoryNode.isMandatory()) {
                    setMandatory(updateField, flag ? mandatoryNode.isMandatory() : !mandatoryNode.isMandatory());
                    mandatoryNode.setValid(updateField.validate());
                }
            }

        } else {
            setMandatory(field, node.isMandatory());
        }
    }

    @SuppressWarnings("rawtypes")
    private static void setMandatory(Field<?> field, boolean mandatory) {
        if (field instanceof NumberField) {
            ((NumberField) field).setAllowBlank(!mandatory);
        } else if (field instanceof BooleanField) {
            ((BooleanField) field).setAllowBlank(!mandatory);
        } else if (field instanceof DateField) {
            ((DateField) field).setAllowBlank(!mandatory);
        } else if (field instanceof TextField) {
            ((TextField) field).setAllowBlank(!mandatory);
        }
    }

    private static void validate(Field<?> field, ItemNodeModel node) {
        node.setValid(field.isValid());
    }
}
