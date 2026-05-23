package com.superapp.demo;

import java.util.Collections;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/editor")
public class EditorController {

    @GetMapping("/ping")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')")
    public Map<String, String> editorPing() {
        return Collections.singletonMap("message", "editor access granted");
    }
}

