package de.intension.rest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemoveLicenceRequest {

    private String studentId;

    public RemoveLicenceRequest(String studentId) {
        this.studentId = studentId;
    }
}