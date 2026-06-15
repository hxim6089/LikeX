package com.example.rec.controller;

import com.example.rec.service.PersonaService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @GetMapping("/{id}/persona")
    public Map<String, Object> getPersona(@PathVariable Long id) {
        return personaService.getUserPersona(id);
    }
}
