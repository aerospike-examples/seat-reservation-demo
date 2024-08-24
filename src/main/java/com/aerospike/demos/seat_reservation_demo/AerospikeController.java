package com.aerospike.demos.seat_reservation_demo;

// import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.Txn;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;

@Controller
@RestController
public class AerospikeController {
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
