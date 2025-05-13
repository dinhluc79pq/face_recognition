package com.facerecognition.Server;

import java.time.LocalDate;
import java.util.List;

public class User {
    private String uid;
    private String name;
    private LocalDate dob;
    private String avata;
    private List<Double> faceEncoding;

    public User(){}

    public User(String uid, String name, LocalDate dob, String avata, List<Double> faceEncoding) {
        this.uid = uid;
        this.name = name;
        this.dob = dob;
        this.avata = avata;
        this.faceEncoding = faceEncoding;
    }
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public LocalDate getDob() {
        return dob;
    }
    public void setDoB(LocalDate dob) {
        this.dob = dob;
    }
    public String getAvata() {
        return avata;
    }
    public void setAvata(String avata) {
        this.avata = avata;
    }
    public List<Double> getFaceEncoding() {
        return faceEncoding;
    }
    public void setFaceEncoding(List<Double> faceEncoding) {
        this.faceEncoding = faceEncoding;
    }

    @Override
    public String toString() {
        return "User{" +
            "uid='" + uid + '\'' +
            ", name='" + name + '\'' +
            ", doB=" + dob +
            ", avata='" + avata + '\'' +
            '}';
    }
}
