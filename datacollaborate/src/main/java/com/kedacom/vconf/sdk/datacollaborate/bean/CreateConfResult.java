package com.kedacom.vconf.sdk.datacollaborate.bean;

public class CreateConfResult {
    private String       confE164;
    private String       confName;
    private EConfMode   confMode;
    private EConfType   confType;

    public String getConfE164() {
        return confE164;
    }

    public void setConfE164(String confE164) {
        this.confE164 = confE164;
    }

    public String getConfName() {
        return confName;
    }

    public void setConfName(String confName) {
        this.confName = confName;
    }

    public EConfMode getConfMode() {
        return confMode;
    }

    public void setConfMode(EConfMode confMode) {
        this.confMode = confMode;
    }

    public EConfType getConfType() {
        return confType;
    }

    public void setConfType(EConfType confType) {
        this.confType = confType;
    }
}