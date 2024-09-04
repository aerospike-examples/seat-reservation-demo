package dev.aerospike.ticketfaster.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.aerospike.ticketfaster.model.SeatLocation;
import dev.aerospike.ticketfaster.model.ShoppingCart;
import dev.aerospike.ticketfaster.service.ShoppingCartService;

@Controller
public class ShoppingCartController_Evan {
/*
    private final ShoppingCartService shoppingCartService;

    // Constructor injection of the ShoppingCartService
    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @GetMapping("/shopping-cart")
    public ResponseEntity<ShoppingCart> viewCart() {
        ShoppingCart cart = shoppingCartService.getCurrentCart();
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/shopping-cart/seats")
    public ResponseEntity<Void> addToCart(@RequestParam Long showId, @RequestParam int sectionNumber,
            @RequestParam int seatNumber) {
        SeatLocation seatLocation = new SeatLocation(showId, sectionNumber, seatNumber);
        shoppingCartService.addSeatToCart(seatLocation);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/shopping-cart")
    public ResponseEntity<Void> clear(@PathVariable Long shoppingCartId) {
        shoppingCartService.clear();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shopping-cart/checkout")
    public ResponseEntity<Void> checkout() {
        shoppingCartService.checkout();
        return ResponseEntity.ok().build();
    }
    */
}