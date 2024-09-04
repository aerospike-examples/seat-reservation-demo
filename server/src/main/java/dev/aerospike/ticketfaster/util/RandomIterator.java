package dev.aerospike.ticketfaster.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Given a length, iterate through all the numbers in the sequence, starting
 * at a random number and returning all the numbers up to the starting number - 1;
 */
public class RandomIterator {
    private int start;
    private int current;
    private int length;
    private boolean started;
    
    public RandomIterator(int length) {
        this.reset(length);
    }
    
    public void reset(int length) {
        started = false;
        this.length = length;
        this.start = ThreadLocalRandom.current().nextInt(length);
        this.current = this.start;
    }
    
    private int nextInSequnence() {
        return started ? ((current+1) % length) : start;
    }
    public boolean hasNext() {
        if (!started) {
            return true;
        }
        return nextInSequnence() != start;
    }
    public int next() {
        int next = nextInSequnence();
        if (started && next == start) {
            throw new IllegalStateException("Sequence exhausted");
        }
        if (started) {
            current++;
        }
        else {
            started = true;
        }
        return next;
    }
    
    public static void main(String[] args) {
        RandomIterator iterator = new RandomIterator(10);
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
        iterator.reset(5);
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
    }
}
