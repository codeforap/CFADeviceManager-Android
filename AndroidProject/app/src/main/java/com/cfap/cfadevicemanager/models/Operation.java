package com.cfap.cfadevicemanager.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Shreya Jagarlamudi on 21/09/15.
 */
public class Operation implements Serializable {

    private String code;
    private String type;
    private String complianceType;
    private int id;
    private String status;
    private String receivedTimeStamp;
    private String createdTimeStamp;
    private boolean enabled;
    private Object payLoad;

    public Operation() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getComplianceType() { return complianceType; }

    public void setComplianceType(String complianceType) { this.complianceType = complianceType; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReceivedTimeStamp() {
        return receivedTimeStamp;
    }

    public void setReceivedTimeStamp(String receivedTimeStamp) {
        this.receivedTimeStamp = receivedTimeStamp;
    }

    public String getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    public void setCreatedTimeStamp(String createdTimeStamp) {
        this.createdTimeStamp = createdTimeStamp;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Object getPayLoad() {
        return payLoad;
    }

    public void setPayLoad(Object payLoad) {
        this.payLoad = payLoad;
        JSONArray convertedOperations = new JSONArray();
        if (payLoad instanceof ArrayList) {
            ArrayList<LinkedHashMap<String, String>> operations = (ArrayList) payLoad;
            for (LinkedHashMap operation : operations) {
                JSONObject jsonObject = new JSONObject(operation);
                convertedOperations.put(jsonObject);
            }
            this.payLoad = convertedOperations.toString();
        }
    }
}
