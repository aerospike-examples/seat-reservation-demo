package dev.aerospike.ticketfaster.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aerospike.client.Txn;

import dev.aerospike.ticketfaster.model.Seat;
import dev.aerospike.ticketfaster.model.SeatStatus;
import dev.aerospike.ticketfaster.model.ShoppingCart;
import dev.aerospike.ticketfaster.model.ShoppingCart.Status;

@Service
public class ShoppingCartService {
    @Autowired
    AerospikeService aerospikeService;

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
            e.printStackTrace();
            aerospikeService.rollbackTxn(txn);
            throw e;
        }
    }
    
    public void purchaseBooking(ShoppingCart shoppingCart, Txn txn) {
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
        }
        catch (Exception e) {
            System.err.printf("Error occurred during booking: %s (%s)\n", e.getMessage(), e.getClass());
            e.printStackTrace();
            aerospikeService.rollbackTxn(txnToUse);
            throw e;
        }
    }
    
    public Optional<ShoppingCart> loadCart(String cartId) {
        return aerospikeService.readCart(cartId);
    }
}
