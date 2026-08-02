package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.model.Faq;
import com.demoproject.shoppingcart.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    public List<Faq> getAllActiveFaqs() {
        return faqRepository.findByActiveTrue();
    }

    @Transactional
    public Faq createFaq(Faq faq) {
        return faqRepository.save(faq);
    }
}
