package com.wesleyhome.library.server.domain;

public record Patron(
    String id,
    String name,
    String email,
    String phone,
    MembershipStatus membershipStatus
) {
    public boolean canBorrow() {
        return membershipStatus == MembershipStatus.ACTIVE;
    }
}
