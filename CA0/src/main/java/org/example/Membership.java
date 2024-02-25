package org.example;

class Membership {
    private String teamName;
    private Date startDate;
    private Date endDate;

    public Membership(String teamName, Date startDate, Date endDate) {
        if (teamName == null || teamName.isEmpty() || startDate == null || endDate == null || !(startDate instanceof Date) || !(endDate instanceof Date) || startDate.compareTo(endDate) > 0) {
            throw new IllegalArgumentException("Invalid arguments for membership");
        }

        this.teamName = teamName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean isOverlapping(Membership other) {
        return (startDate.compareTo(other.endDate) <= 0) && (endDate.compareTo(other.startDate) >= 0);
    }

    public int getMembershipDurationInDays() {
        return startDate.differenceInDays(endDate);
    }
    public boolean equalsTeamName(String otherTeamName) {
        return this.teamName.equals(otherTeamName);
    }
}

