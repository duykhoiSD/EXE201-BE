package com.insurmatch.service;

import com.insurmatch.entity.QuoteRequest;
import com.insurmatch.entity.User;
import com.insurmatch.exception.ResourceNotFoundException;
import com.insurmatch.repository.QuoteRequestRepository;
import com.insurmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteRequestService {

    private final QuoteRequestRepository quoteRequestRepository;
    private final UserRepository userRepository;

    public List<QuoteRequest> getAllQuotes() {
        return quoteRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public QuoteRequest getQuoteById(Long id) {
        return quoteRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QuoteRequest not found with id: " + id));
    }

    public QuoteRequest createQuote(QuoteRequest quote) {
        quote.setStatus("NEW");
        return quoteRequestRepository.save(quote);
    }

    public QuoteRequest assignAgent(Long quoteId, Long agentId) {
        QuoteRequest quote = getQuoteById(quoteId);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));
        quote.setAssignedAgent(agent);
        quote.setStatus("ASSIGNED");
        return quoteRequestRepository.save(quote);
    }

    public long count() {
        return quoteRequestRepository.count();
    }

    public long countByStatus(String status) {
        return quoteRequestRepository.findByStatus(status).size();
    }
}
