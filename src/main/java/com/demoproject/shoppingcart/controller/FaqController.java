package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.model.Faq;
import com.demoproject.shoppingcart.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<List<Faq>> getAllActiveFaqs() {
        return ResponseEntity.ok(faqService.getAllActiveFaqs());
    }

    @PostMapping
    public ResponseEntity<Faq> createFaq(@RequestBody Faq faq) {
        return ResponseEntity.ok(faqService.createFaq(faq));
    }
}
