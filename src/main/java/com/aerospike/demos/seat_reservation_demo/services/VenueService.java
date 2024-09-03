package com.aerospike.demos.seat_reservation_demo.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aerospike.client.Txn;
import com.aerospike.demos.seat_reservation_demo.model.Venue;

@Service
public class VenueService {
    @Autowired
    private AerospikeService aerospikeService;
    
    public Optional<Venue> loadVenue(String venueId) {
        return aerospikeService.readVenue(venueId);
    }
    
    public void saveVenue(Venue venue, Txn txn) {
        aerospikeService.save(null, venue, txn);
    }
}
