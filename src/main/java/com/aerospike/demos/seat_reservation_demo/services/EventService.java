package com.aerospike.demos.seat_reservation_demo.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aerospike.client.Txn;
import com.aerospike.demos.seat_reservation_demo.model.Event;
import com.aerospike.demos.seat_reservation_demo.model.SeatStatus;
import com.aerospike.demos.seat_reservation_demo.model.Venue;
import com.google.gson.Gson;

@Service
public class EventService {
    
    @Autowired
    private AerospikeService aerospikeService;
    
    private static final int LOAD_THREADS = 16;
    
    private boolean save(Map<String, String> data) {
        DateFormat iso8601 = new SimpleDateFormat("yyyy/MM/dd' 'HH:mm:ssX");
        
        String datetime = data.get("datetime_utc");
        if (datetime != null) {
            try {
                Date date = iso8601.parse(datetime + "Z");
                Event event = Event.builder()
                        .date(date)
                        .url(data.get("event_url"))
                        .category(data.get("event_category"))
                        .subCategory(data.get("event_subcategory"))
                        .title(data.get("title"))
                        .id(data.get("event_id"))
                        .build();
                
                Venue venue = new Venue();
                venue.setAddress(data.get("address"));
                venue.setCity(data.get("city"));
                venue.setCountry(data.get("country"));
                venue.setDescription("");
                venue.setId(data.get("name"));
                venue.setName(venue.getId());
                venue.setPostalCode(data.get("postal_code"));
                venue.addMultipleIdenticalSections(12, 24, 24);
                event.setVenue(venue);
                
                aerospikeService.save(null, venue, null);
                aerospikeService.save(null, event, null);
            }
            catch (Exception e) {
                return false;
            }
        }
        return true;
    }
    
    public void loadEventAndVenueData(InputStream stream) throws IOException, InterruptedException {
        Gson gson = new Gson();
        try (Reader reader = new BufferedReader(new InputStreamReader(stream))) {
            List<Map<String, String>> eventList = gson.fromJson(reader, List.class);
            final AtomicInteger nextEvent = new AtomicInteger(eventList.size());
            final AtomicInteger saveSuccessful = new AtomicInteger();
            
            ExecutorService service = Executors.newFixedThreadPool(LOAD_THREADS);
            for (int i = 0; i < LOAD_THREADS; i++) {
                 service.submit(() -> {
                    while (true) {
                        int id = nextEvent.decrementAndGet();
                        if (id < 0) {
                            break;
                        }
                        Map<String, String> thisObject = eventList.get(id);
                        if (thisObject != null) {
                            if (save(thisObject)) {
                                saveSuccessful.incrementAndGet();
                            }
                        }
                    }
                 });
            }
            service.shutdown();
            service.awaitTermination(1, TimeUnit.DAYS);
        }
    }

    public byte[][][] getAvailableSeats(Event event) {
        return aerospikeService.getEventSeatStatus(event);
    }

    public void saveEvent(Event event) {
        if (event.getVenue() == null) {
            throw new IllegalArgumentException("Event must be associated with a venue");
        }
        aerospikeService.save(null, event, null);
    }
    
    public Optional<Event> loadEvent(String eventId) {
        return aerospikeService.readEvent(eventId);
    }

    public void setSeatStatus(String eventId, String custId, int sectionId, int row, int seatNumber, SeatStatus newStatus, Txn txn) {
        aerospikeService.setSeatStatus(eventId, custId, sectionId, row, seatNumber, newStatus, txn);
    }
    
    public List<Event> getAllEvents() {
        return aerospikeService.getEventsInDateRange(new Date(0), new Date(Long.MAX_VALUE));
    }
    
    public List<Event> getEventsInDateRange(Date startDate, Date endDate) {
        return aerospikeService.getEventsInDateRange(startDate, endDate);
    }
    
    /**
     * Remove all seat reservations for an event. 
     * @param eventId
     */
    public void clearAllSeats(Event event) {
        aerospikeService.clearSeatStatusesForEvent(event);
        // TODO: This only clears out the seat data -- if bookings are saved against a customer,
        // we should remove these too.
    }
}
