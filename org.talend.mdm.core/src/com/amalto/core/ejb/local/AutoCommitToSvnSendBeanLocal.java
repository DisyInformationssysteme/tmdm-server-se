/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.ejb.local;

/**
 * Local interface for AutoCommitToSvnSendBean.
 * @xdoclet-generated at 14-10-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface AutoCommitToSvnSendBeanLocal
   extends javax.ejb.EJBLocalObject
{
   /**
    * Send the saved item
    * @throws XtentisException
    */
   public boolean sendMsg( java.lang.String text ) throws com.amalto.core.util.XtentisException;

}
