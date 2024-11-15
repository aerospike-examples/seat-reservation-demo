package dev.aerospike.ticketfaster.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotifierService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public void sendMessage(String name, String message) {
        emitters.forEach(emitter -> {
            try {
                System.out.printf("sending: %s->%s\n", name, message);
                emitter.send(SseEmitter.event().name(name).data(message));
            }
            catch (IOException e) {
                System.out.print(e);
                emitter.complete();
            }
        });
    }
    
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter();
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitter.complete());
        emitters.add(emitter);
        return emitter;
    }
}
