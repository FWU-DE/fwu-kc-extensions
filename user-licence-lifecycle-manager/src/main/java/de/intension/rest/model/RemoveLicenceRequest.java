package de.intension.rest.model;

public class RemoveLicenceRequest {

    private String studentId;

    public RemoveLicenceRequest(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}