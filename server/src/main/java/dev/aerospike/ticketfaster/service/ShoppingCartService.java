package dev.aerospike.ticketfaster.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.aerospike.client.Txn;

import dev.aerospike.ticketfaster.model.Event;
import dev.aerospike.ticketfaster.model.Seat;
import dev.aerospike.ticketfaster.model.SeatStatus;
import dev.aerospike.ticketfaster.model.ShoppingCart;
import dev.aerospike.ticketfaster.model.ShoppingCart.Status;
import dev.aerospike.ticketfaster.util.SeatCache.NotEnoughSeatsException;

@Service
public class ShoppingCartService {
    @Autowired
    AerospikeService aerospikeService;

    @Autowired
    EventService eventService;
    
    public void createShoppingCart(ShoppingCart shoppingCart) {
        Txn txn = new Txn();
        try {
            for (Seat thisSeat : shoppingCart.getSeats()) {
                aerospikeService.setSeatStatus(
                        shoppingCart.getEventId(),
                        getCustId(shoppingCart.getCustId(),shoppingCart.getId()),
                        thisSeat.getSectionId(),
                        thisSeat.getRow(),
                        thisSeat.getSeatNumber(),
                        SeatStatus.RESERVED,
                        txn);
            }
            aerospikeService.save(null, shoppingCart, txn);
            aerospikeService.commitTxn(txn);
        }
        catch (Exception e) {
            aerospikeService.rollbackTxn(txn);
            throw e;
        }
    }
    
    public void clearShoppingCart(ShoppingCart shoppingCart) {
        Txn txn = new Txn();
        try {
            for (Seat thisSeat : shoppingCart.getSeats()) {
                aerospikeService.setSeatStatus(
                        shoppingCart.getEventId(),
                        getCustId(shoppingCart.getCustId(),shoppingCart.getId()),
                        thisSeat.getSectionId(),
                        thisSeat.getRow(),
                        thisSeat.getSeatNumber(),
                        SeatStatus.AVAILABLE,
                        txn);
            }
            shoppingCart.setStatus(Status.ABANDONED);
            shoppingCart.getSeats().clear();
            aerospikeService.save(null, shoppingCart, txn);
            aerospikeService.commitTxn(txn);
        }
        catch (Exception e) {
            System.err.printf("Error occurred during booking: %s (%s)\n", e.getMessage(), e.getClass());
            e.printStackTrace();
            aerospikeService.rollbackTxn(txn);
            throw e;
        }
    }
    
    public void removeSeatsFromCart(ShoppingCart shoppingCart, Seat ...seats) {
        Txn txn = new Txn();
        try {
            for (Seat thisSeat : seats) {
                if (!shoppingCart.getSeats().contains(thisSeat)) {
                    String message = String.format("Cart %s does not contain seat %s so cannot remove it\n",
                            shoppingCart.getId(), thisSeat);
                    throw new IllegalArgumentException(message);
                }
                aerospikeService.setSeatStatus(
                        shoppingCart.getEventId(),
                        getCustId(shoppingCart.getCustId(),shoppingCart.getId()),
                        thisSeat.getSectionId(),
                        thisSeat.getRow(),
                        thisSeat.getSeatNumber(),
                        SeatStatus.AVAILABLE,
                        txn);
                shoppingCart.getSeats().remove(thisSeat);
            }
            aerospikeService.save(null, shoppingCart, txn);
            aerospikeService.commitTxn(txn);
        }
        catch (Exception e) {
            System.err.printf("Error occurred during booking: %s (%s)\n", e.getMessage(), e.getClass());
            e.printStackTrace();
            aerospikeService.rollbackTxn(txn);
            throw e;
        }
    }
    
    private String getCustId(long custId, String cartId) {
        if (custId == 0) {
            return "user-" + cartId;
        }
        else {
            return Long.toString(custId);
        }
    }
    public void addSeatsToCart(ShoppingCart shoppingCart, Seat ... seats) {
        Txn txn = new Txn();
        try  {
            for (Seat thisSeat : seats) {
                if (!shoppingCart.getSeats().contains(thisSeat)) {
                    aerospikeService.setSeatStatus(
                            shoppingCart.getEventId(), 
                            getCustId(shoppingCart.getCustId(),shoppingCart.getId()),
                            thisSeat.getSectionId(),
                            thisSeat.getRow(), 
                            thisSeat.getSeatNumber(), 
                            SeatStatus.RESERVED, 
                            txn);
                    shoppingCart.getSeats().add(thisSeat);
                }
            }
            aerospikeService.save(null, shoppingCart, txn);
            aerospikeService.commitTxn(txn);
        }
        catch (Exception e) {
            System.err.printf("Error occurred during booking: %s (%s)\n", e.getMessage(), e.getClass());
            aerospikeService.rollbackTxn(txn);
            throw e;
        }
    }
    
    public void purchaseBooking(ShoppingCart shoppingCart, Txn txn) {
        // Since seats are stored in rows, with each row being a record,
        // it is still possible to get MRT exceptions on purchasing a booking.
        // (For example, start the transaction on one seat, the second seat is
        // on a seat in a different row, this one is already locked doing a booking)
        // So we retry multiple times as this operation should eventually succeed.
        int MAX_RETRIES = 15;
        RuntimeException lastException = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            final Txn txnToUse = (txn == null) ? new Txn() : txn;
            try {
                shoppingCart.setStatus(Status.PURCHASED);
                for (Seat thisSeat : shoppingCart.getSeats()) {
                    aerospikeService.setSeatStatus(
                            shoppingCart.getEventId(), 
                            getCustId(shoppingCart.getCustId(),shoppingCart.getId()),
                            thisSeat.getSectionId(),
                            thisSeat.getRow(), 
                            thisSeat.getSeatNumber(), 
                            SeatStatus.PURCHASED, 
                            txnToUse);
                }
                shoppingCart.getSeats().clear();
                aerospikeService.save(null, shoppingCart, txnToUse);
                aerospikeService.commitTxn(txnToUse);
                return;
            }
            catch (RuntimeException e) {
                System.out.printf("Retrying purchaseBooking transaction, %d of %d%n", (i+1), MAX_RETRIES);
                // Note that if the commit fails, we do not need to call abort, but it doesn't hurt
                // to do so. It's possible that another exception has occurred, and we might need to rollback.
                aerospikeService.rollbackTxn(txnToUse);
                lastException = e;
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(10)+2);
                } catch (InterruptedException e1) {
                    break;
                }
            }
        }
        System.err.printf("Error occurred during booking: %s (%s)\n", lastException.getMessage(), lastException.getClass());
        lastException.printStackTrace();
        throw lastException;
    }
    
    public List<Seat> findAndReserveRandomSeats(ShoppingCart cart, String eventId, int count) {
        final int MAX_RETRIES = 30;
        Optional<Event> event = eventService.loadEvent(eventId);
        if (event.isEmpty()) {
            throw new IllegalArgumentException("Event must be provided");
        }
        Event theEvent = event.get();
        AerospikeException lastException = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                List<Seat> seats = aerospikeService.getRandomAvailableSeats(theEvent, count);
                addSeatsToCart(cart, seats.toArray(new Seat[0]));
                return seats;
            }
            catch (AerospikeException ae) {
                if (ae.getResultCode() == ResultCode.TXN_FAILED) {
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(10)+3);
                    } catch (InterruptedException ignored) {
                    }
                }
                lastException = ae;
            }
        }
        if (lastException.getResultCode() == ResultCode.TXN_FAILED) {
            throw new NotEnoughSeatsException(0, 0);
        }
        throw lastException;
    }


    public Optional<ShoppingCart> loadCart(String cartId) {
        return aerospikeService.readCart(cartId);
    }
}
