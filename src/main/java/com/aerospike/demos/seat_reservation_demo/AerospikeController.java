package com.aerospike.demos.seat_reservation_demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Tran;
import com.aerospike.client.policy.WritePolicy;

@Controller
@RestController
public class AerospikeController {
    @GetMapping("/demo/api/getDatabases") 
    public String getDatabases() {
        IAerospikeClient client = new AerospikeClient("localhost", 3000);
        
        Tran tran = new Tran();
        WritePolicy wp = new WritePolicy();
        wp.tran = tran;
        client.put(wp, new Key("test", "testSet", 123), new Bin("name", "value"));
        client.put(wp, new Key("test", "testSet", 456), new Bin("age", 37));
        client.commit(tran);
        client.close();
        return "Success";
    }
}
