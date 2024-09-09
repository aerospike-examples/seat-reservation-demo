package dev.aerospike.ticketfaster.service;

import java.util.ArrayList;
import java.util.List;
// import java.util.concurrent.ArrayBlockingQueue;
// import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotifierService {
    private final List<SseEmitter> emitters = new ArrayList<>();
    // public NotifierService() {
    //     sendingThread = new Thread(() -> {
    //         while (true) {
    //             try {
    //                 Message message = messages.take();
    //                 for (SseEmitter emitter : emitters) {
    //                     try {
    //                         System.out.printf("sending: %s->%s\n", message.name, message.message);
    //                         emitter.send(SseEmitter.event().name(message.name).data(message.message));
    //                     }
    //                     catch (Exception e) {
    //                         emitter.completeWithError(e);
    //                         emitters.remove(emitter);
    //                     }
    //                 }
    //             } catch (InterruptedException e) {
    //                 e.printStackTrace();
    //             }
    //         }
    //     }, "clientNotifierThread");
    //     sendingThread.setDaemon(true);
    //     sendingThread.start();
    // }
    
    // private static final class Message {
    //     private final String name;
    //     private final String message;
    //     public Message(String name, String message) {
    //         this.name = name;
    //         this.message = message;
    //     }
    // }

    public void sendMessage(String name, String message) {
        emitters.forEach(emitter -> {
            try {
                System.out.printf("sending: %s->%s\n", name, message);
                emitter.send(SseEmitter.event().name(name).data(message));
            }
            catch (Exception e) {
                emitter.completeWithError(e);
                emitters.remove(emitter);
            }
        });
    }
    
    private void removeEmitter(SseEmitter emitter) {
        System.out.println("Emitter removed");
        emitters.remove(emitter);
    }

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter();
        emitter.onCompletion(() -> removeEmitter(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitters.add(emitter);
        System.out.println("Registering client to emit to");
        return emitter;
    }
}
