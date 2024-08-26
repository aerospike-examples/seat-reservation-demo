package com.aerospike.demos.seat_reservation_demo.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
                Event event = new Event();
                event.setDate(date);
                event.setUrl(data.get("event_url"));
                event.setCategory(data.get("event_category"));
                event.setSubCategory(data.get("event_subcategory"));
                event.setTitle(data.get("title"));
                event.setId(data.get("event_id"));
                
                Venue venue = new Venue();
                venue.setAddress(data.get("address"));
                venue.setCity(data.get("city"));
                venue.setCountry(data.get("country"));
                venue.setDescription("");
                venue.setId(data.get("name"));
                venue.setName(venue.getId());
                venue.setPostalCode(data.get("postal_code"));
                venue.setSeatCount(5000);
                venue.setNumRows(100);
                event.setVenue(venue);
                
                aerospikeService.save(null, venue, null);
                aerospikeService.save(null, event, null);
            }
            catch (Exception e) {
                return false;
            }
        }
        
        ;
        return true;
    }
    
    public void loadData(File file) throws IOException, InterruptedException {
        Gson gson = new Gson();
        try (Reader reader = new BufferedReader(new FileReader(file))) {
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
    
    public void createNewEvent(Event event) {
        if (event.getVenue() == null) {
            throw new IllegalArgumentException("Event must be associated with a venue");
        }
        aerospikeService.save(null, event, null);
    }
    
    public void setSeatStatus(String eventId, long custId, int row, int seatNumber, SeatStatus newStatus, Txn txn) {
        aerospikeService.setSeatStatus(eventId, custId, row, seatNumber, newStatus, txn);
    }
}
