/*
 * Generated by XDoclet - Do not edit!
 */
package com.amalto.core.ejb.dao;

/**
 * Data object for Service.
 * @xdoclet-generated at 14-10-09
 * @copyright The XDoclet Team
 * @author XDoclet
 * @version ${version}
 */
public class ServiceData
   extends java.lang.Object
   implements java.io.Serializable
{
   private java.lang.String serviceName;
   private java.lang.String configuration;
   private java.lang.String serviceData;

  /* begin value object */
   private com.amalto.core.ejb.remote.ServiceValue ServiceValue = null;

   public com.amalto.core.ejb.remote.ServiceValue getServiceValue()
   {
	  if( ServiceValue == null )
	  {
          ServiceValue = new com.amalto.core.ejb.remote.ServiceValue();
	  }
      try
         {
            ServiceValue.setServiceName( getServiceName() );
            ServiceValue.setConfiguration( getConfiguration() );
            ServiceValue.setServiceData( getServiceData() );
                   }
         catch (Exception e)
         {
            throw new javax.ejb.EJBException(e);
         }

	  return ServiceValue;
   }

  /* end value object */

   public ServiceData()
   {
   }

   public ServiceData( java.lang.String serviceName,java.lang.String configuration,java.lang.String serviceData )
   {
      setServiceName(serviceName);
      setConfiguration(configuration);
      setServiceData(serviceData);
   }

   public ServiceData( ServiceData otherData )
   {
      setServiceName(otherData.getServiceName());
      setConfiguration(otherData.getConfiguration());
      setServiceData(otherData.getServiceData());

   }

   public com.amalto.core.ejb.remote.ServicePK getPrimaryKey() {
     com.amalto.core.ejb.remote.ServicePK pk = new com.amalto.core.ejb.remote.ServicePK(this.getServiceName());
     return pk;
   }

   public java.lang.String getServiceName()
   {
      return this.serviceName;
   }
   public void setServiceName( java.lang.String serviceName )
   {
      this.serviceName = serviceName;
   }

   public java.lang.String getConfiguration()
   {
      return this.configuration;
   }
   public void setConfiguration( java.lang.String configuration )
   {
      this.configuration = configuration;
   }

   public java.lang.String getServiceData()
   {
      return this.serviceData;
   }
   public void setServiceData( java.lang.String serviceData )
   {
      this.serviceData = serviceData;
   }

   public String toString()
   {
      StringBuffer str = new StringBuffer("{");

      str.append("serviceName=" + getServiceName() + " " + "configuration=" + getConfiguration() + " " + "serviceData=" + getServiceData());
      str.append('}');

      return(str.toString());
   }

   public boolean equals( Object pOther )
   {
      if( pOther instanceof ServiceData )
      {
         ServiceData lTest = (ServiceData) pOther;
         boolean lEquals = true;

         if( this.serviceName == null )
         {
            lEquals = lEquals && ( lTest.serviceName == null );
         }
         else
         {
            lEquals = lEquals && this.serviceName.equals( lTest.serviceName );
         }
         if( this.configuration == null )
         {
            lEquals = lEquals && ( lTest.configuration == null );
         }
         else
         {
            lEquals = lEquals && this.configuration.equals( lTest.configuration );
         }
         if( this.serviceData == null )
         {
            lEquals = lEquals && ( lTest.serviceData == null );
         }
         else
         {
            lEquals = lEquals && this.serviceData.equals( lTest.serviceData );
         }

         return lEquals;
      }
      else
      {
         return false;
      }
   }

   public int hashCode()
   {
      int result = 17;

      result = 37*result + ((this.serviceName != null) ? this.serviceName.hashCode() : 0);

      result = 37*result + ((this.configuration != null) ? this.configuration.hashCode() : 0);

      result = 37*result + ((this.serviceData != null) ? this.serviceData.hashCode() : 0);

      return result;
   }

}
