package com.aerospike.demos.seat_reservation_demo;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;

@Controller
@RestController
public class AerospikeController {
    @GetMapping("/demo/api/getDatabases") 
    public String getDatabases() {
        IAerospikeClient client = new AerospikeClient("localhost", 3000);
        client.getTranVerifyPolicyDefault();
        return "Success";
    }

}
