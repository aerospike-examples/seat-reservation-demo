package com.aerospike.demos.seat_reservation_demo;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Txn;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.demos.seat_reservation_demo.model.SeatStatus;
import com.aerospike.demos.seat_reservation_demo.services.EventService;

@Controller
@RestController
public class AerospikeController {
    @Autowired
    private EventService eventService;
    
    @GetMapping("/demo/loadEvents")
    public String loadEvents() {
        // TODO: Move this from an absolute path
        File file = new File("/Users/tfaulkes/Programming/Aerospike/git/seat-reservation-demo/data/ticketmaster_events_2023-08-02.json");
        try {
            eventService.loadData(file);
            return "Success";
        }
        catch (IOException | InterruptedException ioe) {
            System.err.printf("Error loading data from file %s: %s (%s)", file.getAbsoluteFile(), ioe.getMessage(), ioe.getClass());
            ioe.printStackTrace();
            return "Failed";
        }
    }
    
    @GetMapping("test")
    public String test() {
        String eventId = "testEvent";
        eventService.setSeatStatus(eventId, 12345, 10, 48, SeatStatus.RESERVED, null);
        eventService.setSeatStatus(eventId, 12345, 10, 49, SeatStatus.RESERVED, null);
        eventService.setSeatStatus(eventId, 12345, 10, 50, SeatStatus.RESERVED, null);
        eventService.setSeatStatus(eventId, 12345, 10, 50, SeatStatus.PURCHASED, null);
        eventService.setSeatStatus(eventId, 12345, 10, 50, SeatStatus.AVAILABLE, null);
        return "Done";
    }
    
    @GetMapping("/demo/api/getDatabases") 
    public String getDatabases() {
        try (IAerospikeClient client = new AerospikeClient("localhost", 3000)) {
            Txn tran = new Txn();
            try {
//                WritePolicy wp = client.copyWritePolicyDefault();
                WritePolicy wp = new WritePolicy();
                wp.txn = tran;
                
                System.out.printf("Begin txn %d\n", wp.txn.getId());
                client.put(wp, new Key("test", "testSet", 123), new Bin("name", "value"));
                client.put(wp, new Key("test", "testSet", 456), new Bin("age", 37));
                client.commit(tran);
                return "Success";
            }
            catch (Exception e) {
                client.abort(tran);
                throw e;
            }
        }
    }

    @GetMapping("/tmp/testTxn")
    public String testTxn() {
        AerospikeClient client = new AerospikeClient("localhost", 3000);
        if (client.isConnected()) {

            Txn txn = new Txn();
            System.out.println("Begin txn: " + txn.getId());
            try {
                WritePolicy wp = client.copyWritePolicyDefault();
                wp.txn = txn;
                Key key1 = new Key("test", "demo", 1);
                client.put(wp, key1, new Bin("a", "val1"));
                Key key2 = new Key("test", "demo", 2);
                client.put(wp, key2, new Bin("b", "val2"));
                Policy p = client.copyReadPolicyDefault();
                p.txn = txn;
                Key key3 = new Key("test", "demo", 3);
                Record rec = client.get(p, key3);
                WritePolicy dp = client.copyWritePolicyDefault();
                dp.txn = txn;
                dp.durableDelete = true; // Required when running delete in a MRT.
                client.delete(dp, key3);
                client.commit(txn);
                client.close();
            } catch (Throwable t) {
                // Abort and rollback MRT (multi-record transaction) if any errors occur.
                client.abort(txn);
                client.close();
                throw t;
            }
            return "I did a transaction: " + String.valueOf(txn.getId());

        } else {
            client.close();
            return "Failed to connect to Aerospike database!";
        }

    }
}
