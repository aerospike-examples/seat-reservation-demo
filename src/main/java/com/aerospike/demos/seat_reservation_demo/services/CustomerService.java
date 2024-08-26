package com.aerospike.demos.seat_reservation_demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    @Autowired
    private AerospikeService aerospikeService;
    
    
}
