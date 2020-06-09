package com.advmeds.advmeds_cardreader_lib.cardreader;

import androidx.annotation.Keep;


import java.io.Serializable;
import java.sql.Date;
import java.util.List;

@Keep
public class UserModel implements Serializable {
    private Long id;

    private String icId;

    private String name;

    private String gender;

    private Date birthday;

    private boolean certification;

    private boolean upload;

    private boolean edited;

    //0 = Unknown, 1 = HealthCard, 2 = StaffCard
    private Integer cardType = 0;

    private List<Integer> measureUidList;

    private String cardNumber; //感應卡卡號，先不用
    private String staffCode; //員工編號

    private String note;//備註

    private boolean consentSign = false; //api確認隱私權時用到的欄位

    private boolean isRegistered = false; //api確認隱私權時用到的欄位

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIcId() {
        return icId;
    }

    public void setIcId(String icId) {
        this.icId = icId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public Integer getCardType() {
        return cardType;
    }

    public void setCardType(Integer cardType) {
        this.cardType = cardType;
    }

    public List<Integer> getMeasureUidList() {
        return measureUidList;
    }

    public void setMeasureUidList(List<Integer> measureUidList) {
        this.measureUidList = measureUidList;
    }

    public boolean isCertification() {
        return certification;
    }

    public void setCertification(boolean certification) {
        this.certification = certification;
    }


    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public boolean getConsentSign() {
        return consentSign;
    }

    public void setConsentSign(boolean _consentSign) {
        this.consentSign = _consentSign;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        this.isRegistered = registered;
    }

    public boolean isUpload() {
        return upload;
    }

    public void setUpload(boolean upload) {
        this.upload = upload;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    //    public List<GroupModel> getGroupModelList() {
//        return groupModelList;
//    }
//
//    public void setGroupModelList(List<GroupModel> groupModelList) {
//        this.groupModelList = groupModelList;
//    }

    public int getSex() {
        if (this.gender == null || this.gender.isEmpty()) {
            return 0;
        } else {
            if (this.gender.equals("M")) {
                return 1;
            } else {
                return 2;
            }
        }
    }

    public void setToTestData(String name, Date birthday, String icId, String gender) {
        setIcId(icId);
        setName(name);
        setGender(gender);
        setBirthday(birthday);
    }

    public Boolean isCompleted() {
        return getIcId() != null && !getIcId().isEmpty() &&
                getBirthday() != null &&
                getGender() != null && !getGender().isEmpty() &&
                getName() != null && !getName().isEmpty();
    }

    public UserModel() {
    }
}
