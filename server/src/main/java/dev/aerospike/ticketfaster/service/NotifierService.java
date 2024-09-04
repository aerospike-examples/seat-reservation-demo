package dev.aerospike.ticketfaster.service;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotifierService {
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final ArrayBlockingQueue<Message> messages = new ArrayBlockingQueue<>(1000);
    private final Thread sendingThread;
    public NotifierService() {
        sendingThread = new Thread(() -> {
            while (true) {
                try {
                    Message message = messages.take();
                    for (SseEmitter emitter : emitters) {
                        try {
                            System.out.printf("sending: %s->%s\n", message.name, message.message);
                            emitter.send(SseEmitter.event().name(message.name).data(message.message));
                        }
                        catch (IOException e) {
                            emitters.remove(emitter);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "clientNotifierThread");
        sendingThread.setDaemon(true);
        sendingThread.start();
    }
    
    private static final class Message {
        private final String name;
        private final String message;
        public Message(String name, String message) {
            this.name = name;
            this.message = message;
        }
    }
    public void sendMessage(String name, String message) {
        messages.offer(new Message(name, message));
    }
    
    private void removeEmitter(SseEmitter emitter) {
        System.out.println("Emitter removed");
        emitters.remove(emitter);
    }

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter();
        emitters.add(emitter);
        emitter.onCompletion(() -> removeEmitter(emitter));
        System.out.println("Registering client to emit to");
        return emitter;
    }
}
