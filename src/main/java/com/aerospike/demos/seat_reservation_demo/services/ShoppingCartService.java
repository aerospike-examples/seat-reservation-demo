package com.aerospike.demos.seat_reservation_demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aerospike.client.Txn;
import com.aerospike.demos.seat_reservation_demo.model.Booking;
import com.aerospike.demos.seat_reservation_demo.model.Seat;
import com.aerospike.demos.seat_reservation_demo.model.SeatStatus;
import com.aerospike.demos.seat_reservation_demo.model.ShoppingCart;
import com.aerospike.demos.seat_reservation_demo.model.ShoppingCart.Status;

@Service
public class ShoppingCartService {
    @Autowired
    AerospikeService aerospikeService;

    public boolean addSeatsToBooking(ShoppingCart shoppingCart, Seat ... seats) {
        Txn txn = new Txn();
        try  {
            for (Seat thisSeat : seats) {
                if (!shoppingCart.getSeats().contains(thisSeat)) {
                    aerospikeService.setSeatStatus(
                            shoppingCart.getEventId(), 
                            shoppingCart.getCustId(), 
                            thisSeat.getRow(), 
                            thisSeat.getSeatNumber(), 
                            SeatStatus.RESERVED, 
                            txn);
                    shoppingCart.getSeats().add(thisSeat);
                }
            }
            aerospikeService.save(null, shoppingCart, txn);
            aerospikeService.commitTxn(txn);
            return true;
        }
        catch (Exception e) {
            System.err.printf("Error occurred during booking: %s (%s)\n", e.getMessage(), e.getClass());
            e.printStackTrace();
            aerospikeService.rollbackTxn(txn);
            return false;
        }
    }
    
    public boolean purchaseBooking(ShoppingCart shoppingCart, Txn txn) {
        final Txn txnToUse = (txn == null) ? new Txn() : txn;
        try {
            shoppingCart.setStatus(Status.PURCHASED);
            for (Seat thisSeat : shoppingCart.getSeats()) {
                aerospikeService.setSeatStatus(
                        shoppingCart.getEventId(), 
                        shoppingCart.getCustId(), 
                        thisSeat.getRow(), 
                        thisSeat.getSeatNumber(), 
                        SeatStatus.PURCHASED, 
                        txnToUse);
            }
            aerospikeService.save(null, shoppingCart, txnToUse);
            aerospikeService.commitTxn(txnToUse);
            return true;
        }
        catch (Exception e) {
            System.err.printf("Error occurred during booking: %s (%s)\n", e.getMessage(), e.getClass());
            e.printStackTrace();
            aerospikeService.rollbackTxn(txnToUse);
            return false;
        }
    }
}
