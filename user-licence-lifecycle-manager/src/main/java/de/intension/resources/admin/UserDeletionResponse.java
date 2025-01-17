package de.intension.resources.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDeletionResponse {
    private final int deletedUsers;
}
