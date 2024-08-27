package com.aerospike.demos.seat_reservation_demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aerospike.client.Txn;
import com.aerospike.demos.seat_reservation_demo.model.Booking;
import com.aerospike.demos.seat_reservation_demo.model.Booking.Status;
import com.aerospike.demos.seat_reservation_demo.model.Seat;
import com.aerospike.demos.seat_reservation_demo.model.SeatStatus;

@Service
public class BookingService {
    @Autowired
    AerospikeService aerospikeService;

    public boolean addSeatsToBooking(Booking booking, Seat ... seats) {
        Txn txn = new Txn();
        try  {
            for (Seat thisSeat : seats) {
                if (!booking.getSeats().contains(thisSeat)) {
                    aerospikeService.setSeatStatus(
                            booking.getEventId(), 
                            booking.getCustId(), 
                            thisSeat.getRow(), 
                            thisSeat.getSeatNumber(), 
                            SeatStatus.RESERVED, 
                            txn);
                    booking.getSeats().add(thisSeat);
                }
            }
            aerospikeService.save(null, booking, txn);
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
    
    public boolean purchaseBooking(Booking booking, Txn txn) {
        final Txn txnToUse = (txn == null) ? new Txn() : txn;
        try {
            booking.setStatus(Status.PURCHASED);
            for (Seat thisSeat : booking.getSeats()) {
                aerospikeService.setSeatStatus(
                        booking.getEventId(), 
                        booking.getCustId(), 
                        thisSeat.getRow(), 
                        thisSeat.getSeatNumber(), 
                        SeatStatus.PURCHASED, 
                        txnToUse);
            }
            aerospikeService.save(null, booking, txnToUse);
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
