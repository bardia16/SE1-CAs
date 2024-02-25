package org.example;
import java.util.ArrayList;
import java.util.stream.*;

class Player {
    private String playerName;
    private ArrayList<Membership> membershipHistory;

    public Player(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be empty");
        }
        this.playerName = playerName;
        this.membershipHistory = new ArrayList<>();
    }

    public void addMembership(Membership membership) {
        for (Membership existingMembership : membershipHistory) {
            if (existingMembership.isOverlapping(membership)) {
                throw new IllegalArgumentException("New membership overlaps with existing membership");
            }
        }
        membershipHistory.add(membership);
    }
    public int getTotalMembershipDays(String teamName) {
        return membershipHistory.stream()
                .filter(membership -> membership.equalsTeamName(teamName))
                .mapToInt(membership -> membership.getMembershipDurationInDays())
                .sum();
    }
}