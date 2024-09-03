package dev.aerospike.ticketfaster.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.javafaker.Faker;

import dev.aerospike.ticketfaster.model.Customer;

@Service
public class CustomerService {

    @Autowired
    private AerospikeService aerospikeService;
    
    private Customer generateCustomer(long id) {
        Customer result = new Customer();
        result.setId(id);
        result.setFirstName(Faker.instance().name().firstName());
        result.setLastName(Faker.instance().name().lastName());
        result.setDateOfBirth(Faker.instance().date().birthday());
        return result;
    }
    
    public int createCustomers(long startId, int numCustomers, int numThreads) throws InterruptedException {
        final AtomicLong currentId = new AtomicLong(startId);
        final AtomicInteger successfulCustomers = new AtomicInteger();
        if (numThreads <= 0) {  
            numThreads = Runtime.getRuntime().availableProcessors();
        }
        
        ExecutorService service = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
             service.submit(() -> {
                while (true) {
                    long id = currentId.getAndIncrement();
                    if (id >= startId + numCustomers) {
                        break;
                    }
                    Customer customer = generateCustomer(id);
                    try {
                        aerospikeService.save(null, customer, null);
                        successfulCustomers.incrementAndGet();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
             });
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.DAYS);
        return successfulCustomers.get();
    }
}
