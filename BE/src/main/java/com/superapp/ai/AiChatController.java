package com.superapp.ai;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class AiChatController {

    @GetMapping(value = "/api/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("prompt") String prompt) {
        SseEmitter emitter = new SseEmitter(0L);
        String safePrompt = StringUtils.hasText(prompt) ? prompt.trim() : "Hello";
        String response = "This is a live streaming demo from Spring Boot SSE for prompt: " + safePrompt;
        String[] tokens = response.split(" ");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final int[] index = {0};

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (index[0] >= tokens.length) {
                    emitter.send("[DONE]");
                    emitter.complete();
                    scheduler.shutdown();
                    return;
                }
                emitter.send(tokens[index[0]] + " ");
                index[0]++;
            } catch (IOException ex) {
                emitter.completeWithError(ex);
                scheduler.shutdown();
            }
        }, 0, 160, TimeUnit.MILLISECONDS);

        emitter.onCompletion(scheduler::shutdown);
        emitter.onTimeout(() -> {
            emitter.complete();
            scheduler.shutdown();
        });

        return emitter;
    }
}

