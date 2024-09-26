package de.intension.rest.model;

public class RemoveLicenseRequest
{

    private String studentId;

    public RemoveLicenseRequest(String studentId)
    {
        this.studentId = studentId;
    }

    public String getStudentId()
    {
        return studentId;
    }

    public void setStudentId(String studentId)
    {
        this.studentId = studentId;
    }
}