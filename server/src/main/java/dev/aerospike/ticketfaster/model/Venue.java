package dev.aerospike.ticketfaster.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Venue {
    private String id;
    private String name;
    private String description;
//    /** The number of rows of seats in this venue */
//    private int numRows;
//    /** The number of seats per row */
//    private int seatCount;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    
    private List<Section> seatMap = new ArrayList<Section>();
    
    /**
     * Calculate the total number of rows in the venue
     * @return the sum of rows in all the sections of the venue
     */
    public int calcNumRowsOfSeatsInVenue() {
        int result = 0;
        for (Section section : seatMap) {
            result += section.getNumRows();
        }
        return result;
    }
    
    /**
     * Add a section into the seat map. 
     * @param section - The section to add. The sectionId is overwritten with the next section id
     * @return The id of this section. The passes section will also have been updated with this value
     */
    public synchronized int addSection(Section section) {
        int currentSize = seatMap.size();
        section.setSectionNumber(currentSize);
        seatMap.add(section);
        return currentSize;
    }
    
    /**
     * Add a number of identical sections to the venue. These sections will have names of "Section-1", "Section-2" ....
     * @param numSections
     * @param numRows
     * @param seatsPerRow
     */
    public synchronized void addMultipleIdenticalSections(int numSections, int numRows, int seatsPerRow) {
        for (int i = 0; i < numSections; i++) {
            Section section = new Section.SectionBuilder()
                    .seatsPerRow(seatsPerRow)
                    .numRows(numRows)
                    .name("Section-" + (i+1))
                    .build();
            addSection(section);
        }
    }
}
