package com.aerospike.demos.seat_reservation_demo;

// import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;

@Controller
@RestController
public class AerospikeController {
    @GetMapping("/demo/api/getDatabases")
    public String getDatabases() {
        AerospikeClient client = new AerospikeClient("localhost", 3000);
        if (client.isConnected()) {
            try {
                Key key = new Key("test", "demo", "demokey");
                Bin bin = new Bin("greeting", "Hello, Aerospike!");
                client.put(null, key, bin);
                System.out.println("Record inserted successfully");
            } catch (AerospikeException e) {
                System.err.println("Error inserting record: " + e.getMessage());
            }
            System.out.println("connected!");
            client.close();
            return "Success";
        } else {
            System.out.println("not connected!");
            client.close();
            return "Failure";
        }

    }

}
