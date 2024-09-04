package dev.aerospike.ticketfaster.exception;

public class SimulatorStopException extends RuntimeException {
    public SimulatorStopException() {
        super("No more seats, stopping simulator");
    }
}