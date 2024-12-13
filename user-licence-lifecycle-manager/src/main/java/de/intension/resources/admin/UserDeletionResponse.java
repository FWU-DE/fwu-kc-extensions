package de.intension.resources.admin;

public class UserDeletionResponse {
    private final int deletedUsers;

    public UserDeletionResponse(int deletedUsers) {
        this.deletedUsers = deletedUsers;
    }

    public int getDeletedUsers() {
        return deletedUsers;
    }
}
