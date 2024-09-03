package dev.aerospike.ticketfaster.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aerospike.ticketfaster.dto.ResetConcertRequest;
import dev.aerospike.ticketfaster.dto.ShoppingCartCreateRequest;
import dev.aerospike.ticketfaster.dto.ShoppingCartCreateResponse;
import picocli.CommandLine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(name = "Simulator", mixinStandardHelpOptions = true, description = "Simulates seat reservation actions.")
public class Simulator implements Runnable {

    @CommandLine.Option(
            names = {"-i", "--init-reset"},
            description = "Initialize and reset the concert"
    )
    private boolean shouldInitAndReset = true;

    @CommandLine.Option(
            names = {"-t", "--threads"},
            description = "Number of threads",
            defaultValue = "1")
    private int numOfThreads = 1;

    @CommandLine.Option(
            names = {"-s", "--seats"},
            description = "Number of seats",
            defaultValue = "2")
    private int numOfSeats = 2;

    public static void main(String[] args) {
        // Triggers the CLI
        int exitCode = new CommandLine(new Simulator()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        try {
            System.out.printf("Executing with the following args: \n" +
                    "shouldInitAndReset: %b \n" +
                    "numOfThreads: %d \n" +
                    "numOfSeats: %d \n%n", shouldInitAndReset, numOfThreads, numOfSeats);

            execute(shouldInitAndReset, numOfThreads, numOfSeats);
        } catch (Exception e) {
            System.err.println("Execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void execute(boolean shouldInitAndReset, int numOfThreads, int numOfSeats) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String concertId = "Oasis-one-day";

        if (shouldInitAndReset) {
            System.out.println("Initializing...");
            init(client);
            System.out.println("Reset concert information...");
            resetConcert(client, concertId);
        }

        // Create an ExecutorService with a fixed thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);

        // Submit tasks to the executor
        for (int i = 0; i < numOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    // TODO: do that repeatedly until end condition - no more seats?
                    reserveAndPurchaseSeats(client, numOfSeats, concertId);
                } catch (Exception e) {
                    System.out.println("Error: %s".formatted(e));
                }
            });
        }
        executorService.close();
        // Shutdown the executor service gracefully
        executorService.shutdown();
        try {
            // Wait for all tasks to complete
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Force shutdown if tasks take too long
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow(); // Force shutdown if interrupted
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
    }

    private void init(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/rpc/init"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Could not execute init, error: %s".formatted(response.body()));
        }
    }

    private void resetConcert(HttpClient client, String concertId) throws Exception {
        ResetConcertRequest requestModel = new ResetConcertRequest();
        requestModel.setConcertId(concertId);

        // Convert the model to a JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        String concertJson = objectMapper.writeValueAsString(requestModel);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/rpc/resetConcert"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(concertJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Could not reset concert, error: %s".formatted(response.body()));
        }
    }

    private void reserveAndPurchaseSeats(HttpClient client, int numOfSeats, String concertId) throws Exception {
        System.out.println("Creating shopping cart...");
        // Create shopping cart
        ShoppingCartCreateResponse cart = createShoppingCart(client, concertId);
        System.out.println("Adding items to the shopping cart...");
        // Add items to cart
        addItemsToCart(client, concertId, cart.getShoppingCartId(), numOfSeats);
        System.out.println("Finalizing purchase...");
        // Finalize purchase
        finalizePurchase(client, concertId, cart.getShoppingCartId());
    }

    private ShoppingCartCreateResponse createShoppingCart(HttpClient client, String concertId) throws Exception {
        ShoppingCartCreateRequest requestModel = new ShoppingCartCreateRequest();
        requestModel.setId("abc123");
        requestModel.setUserId(1L);
        // TODO: Replace with getRandomSeats/bookRandomSeats
        requestModel.setSeats(List.of("0-2-3", "0-2-4"));

        ObjectMapper objectMapper = new ObjectMapper();
        String cartJson = objectMapper.writeValueAsString(requestModel);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/concerts/" + concertId + "/shopping-carts"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cartJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.out.println("Error during createShoppingCart!!!!");
            throw new Exception("Could not create shopping cart, error: %s".formatted(response.body()));
        }

        ShoppingCartCreateResponse cart = objectMapper.readValue(response.body(), ShoppingCartCreateResponse.class);
        System.out.println(cart);
        return cart;
    }

    private void addItemsToCart(HttpClient client, String concertId, String cartId, int numOfSeats) throws Exception {
        // TODO: Replace with getRandomSeats/bookRandomSeats (and use numOfSeats)
        String seatsJson = "{\"seats\":[\"0-3-5\", \"0-3-6\"]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/concerts/" + concertId + "/shopping-carts/" + cartId))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(seatsJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Could not add items to shopping cart, error: %s".formatted(response.body()));
        }
    }

    private void finalizePurchase(HttpClient client, String concertId, String cartId) throws Exception {
        String purchaseJson = "{\"id\" : \"" + cartId + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/concerts/" + concertId + "/shopping-carts/purchases"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(purchaseJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Could not add items to shopping cart, error: %s".formatted(response.body()));
        }
    }
}
