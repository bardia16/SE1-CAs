package org.example;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import com.opencsv.exceptions.CsvValidationException;


public class Main {
    public static void main(String[] args) {
        String csvFile = "src/input.csv";
        String playerName = "Gholam";
        String teamName = "Golgohar";

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] nextLine;
            Player player = new Player(playerName);

            // Read the first line outside the loop
            nextLine = reader.readNext();

            while (nextLine != null) {
                try {
                    String currentName = nextLine[0];
                    String currentTeam = nextLine[1];

                    if (currentName.equals(playerName) && currentTeam.equals(teamName)) {
                        int startDay = Integer.parseInt(nextLine[2]);
                        int startMonth = Integer.parseInt(nextLine[3]);
                        int startYear = Integer.parseInt(nextLine[4]);
                        int endDay = Integer.parseInt(nextLine[5]);
                        int endMonth = Integer.parseInt(nextLine[6]);
                        int endYear = Integer.parseInt(nextLine[7]);

                        Date startDate = new Date(startDay, startMonth, startYear);
                        Date endDate = new Date(endDay, endMonth, endYear);
                        Membership membership = new Membership(teamName, startDate, endDate);

                        player.addMembership(membership);
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Handle invalid data format or missing fields
                    e.printStackTrace();
                }

                // Read the next line inside the loop
                nextLine = reader.readNext();
            }

            int totalMembershipDays = player.getTotalMembershipDays(teamName);
            System.out.println(playerName + " has been a member of team " + teamName + " for " + totalMembershipDays + " days.");
        } catch (IOException | CsvValidationException e) {
            // Handle IO exceptions or CSV validation exceptions
            e.printStackTrace();
        }
    }
}




