package com.smartlink.dto.request;

import lombok.Data;

@Data
public class SessionUpdateReq {

    private String iccid;
    private String consultationScenario;
    private String problemType;
    private String temporarySolution;
    private String specialNotes;
    private String antennaPosition;
    private String noPositionReason;
    private String noPositionIssue;
    private String antennaDamaged;
    private String carModel;
    private String fuelType;
    private String manufacturer;
    private String recorderModel;
}
