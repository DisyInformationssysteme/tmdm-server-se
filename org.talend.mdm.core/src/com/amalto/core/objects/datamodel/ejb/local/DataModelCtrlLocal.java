/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.objects.datamodel.ejb.local;

/**
 * Local interface for DataModelCtrl.
 * @xdoclet-generated at 14-10-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface DataModelCtrlLocal
   extends javax.ejb.EJBLocalObject
{
   /**
    * Creates or updates a DataModel
    * @throws XtentisException
    */
   public com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK putDataModel( com.amalto.core.objects.datamodel.ejb.DataModelPOJO dataModel ) throws com.amalto.core.util.XtentisException;

   /**
    * Get Data Model
    * @throws XtentisException
    */
   public com.amalto.core.objects.datamodel.ejb.DataModelPOJO getDataModel( com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK pk ) throws com.amalto.core.util.XtentisException;

   /**
    * Get a DataModel - no exception is thrown: returns null if not found
    * @throws XtentisException
    */
   public com.amalto.core.objects.datamodel.ejb.DataModelPOJO existsDataModel( com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK pk ) throws com.amalto.core.util.XtentisException;

   /**
    * Remove an Data Model
    * @throws XtentisException
    */
   public com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK removeDataModel( com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK pk ) throws com.amalto.core.util.XtentisException;

   /**
    * Retrieve all DataModel PKs
    * @throws XtentisException
    */
   public java.util.Collection getDataModelPKs( java.lang.String regex ) throws com.amalto.core.util.XtentisException;

   /**
    * Checks the datamodel - returns the "corrected schema"
    * @throws XtentisException
    */
   public java.lang.String checkSchema( java.lang.String schema ) throws com.amalto.core.util.XtentisException;

   /**
    * Put a Business Concept Schema
    * @throws XtentisException
    * @return its name
    */
   public java.lang.String putBusinessConceptSchema( com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK pk,java.lang.String conceptSchemaString ) throws com.amalto.core.util.XtentisException;

   /**
    * Delete a Business Concept
    * @throws XtentisException
    * @return its name
    */
   public java.lang.String deleteBusinessConcept( com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK pk,java.lang.String businessConceptName ) throws com.amalto.core.util.XtentisException;

   /**
    * Find all Business Concepts names
    * @throws XtentisException
    */
   public java.lang.String[] getAllBusinessConceptsNames( com.amalto.core.objects.datamodel.ejb.DataModelPOJOPK pk ) throws com.amalto.core.util.XtentisException;

}
