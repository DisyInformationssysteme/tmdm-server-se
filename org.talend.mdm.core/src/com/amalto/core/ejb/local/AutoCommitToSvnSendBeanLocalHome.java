/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.ejb.local;

/**
 * Local home interface for AutoCommitToSvnSendBean.
 * @xdoclet-generated at 14-10-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface AutoCommitToSvnSendBeanLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/AutoCommitToSvnSendBeanLocal";
   public static final String JNDI_NAME="amalto/local/core/autocommittosvnsend";

   public com.amalto.core.ejb.local.AutoCommitToSvnSendBeanLocal create()
      throws javax.ejb.CreateException;

}
