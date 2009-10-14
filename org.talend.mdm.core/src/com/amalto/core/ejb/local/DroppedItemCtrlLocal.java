/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.ejb.local;

/**
 * Local interface for DroppedItemCtrl.
 * @xdoclet-generated at 14-10-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface DroppedItemCtrlLocal
   extends javax.ejb.EJBLocalObject
{
   /**
    * Recover a dropped item
    * @throws XtentisException
    */
   public com.amalto.core.ejb.ItemPOJOPK recoverDroppedItem( com.amalto.core.ejb.DroppedItemPOJOPK droppedItemPOJOPK ) throws com.amalto.core.util.XtentisException;

   /**
    * Find all dropped items pks
    * @throws XtentisException
    */
   public java.util.List findAllDroppedItemsPKs( java.lang.String regex ) throws com.amalto.core.util.XtentisException;

   /**
    * Load a dropped item
    * @throws XtentisException
    */
   public com.amalto.core.ejb.DroppedItemPOJO loadDroppedItem( com.amalto.core.ejb.DroppedItemPOJOPK droppedItemPOJOPK ) throws com.amalto.core.util.XtentisException;

   /**
    * Remove a dropped item
    * @throws XtentisException
    */
   public com.amalto.core.ejb.DroppedItemPOJOPK removeDroppedItem( com.amalto.core.ejb.DroppedItemPOJOPK droppedItemPOJOPK ) throws com.amalto.core.util.XtentisException;

}
