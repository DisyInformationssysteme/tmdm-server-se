// This class was generated by the JAXRPC SI, do not edit.
// Contents subject to change without notice.
// JAX-RPC Standard Implementation ��1.1.2_01������� R40��
// Generated source version: 1.1.2

package com.amalto.core.webservice;


public class WSAssignTask {
    protected java.lang.String taskUUID;
    protected com.amalto.core.webservice.WSStringArray candicates;
    
    public WSAssignTask() {
    }
    
    public WSAssignTask(java.lang.String taskUUID, com.amalto.core.webservice.WSStringArray candicates) {
        this.taskUUID = taskUUID;
        this.candicates = candicates;
    }
    
    public java.lang.String getTaskUUID() {
        return taskUUID;
    }
    
    public void setTaskUUID(java.lang.String taskUUID) {
        this.taskUUID = taskUUID;
    }
    
    public com.amalto.core.webservice.WSStringArray getCandicates() {
        return candicates;
    }
    
    public void setCandicates(com.amalto.core.webservice.WSStringArray candicates) {
        this.candicates = candicates;
    }
}
