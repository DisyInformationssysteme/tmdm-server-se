/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.objects.versioning.ejb.remote;

/**
 * Home interface for VersioningSystemCtrl.
 * @xdoclet-generated at 14-10-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public interface VersioningSystemCtrlHome
   extends javax.ejb.EJBHome
{
   public static final String COMP_NAME="java:comp/env/ejb/VersioningSystemCtrl";
   public static final String JNDI_NAME="amalto/remote/core/versioningsystemctrl";

   public com.amalto.core.objects.versioning.ejb.remote.VersioningSystemCtrl create()
      throws javax.ejb.CreateException,java.rmi.RemoteException;

}
