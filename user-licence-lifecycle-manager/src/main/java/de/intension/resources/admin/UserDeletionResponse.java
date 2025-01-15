package de.intension.resources.admin;

import lombok.Getter;

@Getter
public class UserDeletionResponse {
    private final int deletedUsers;

    public UserDeletionResponse(int deletedUsers) {
        this.deletedUsers = deletedUsers;
    }
}
