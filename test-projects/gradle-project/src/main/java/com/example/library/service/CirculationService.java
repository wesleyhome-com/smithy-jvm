package com.example.library.service;

import com.example.library.db.tables.records.LoansRecord;
import com.example.library.domain.Loan;
import com.example.library.domain.LoanStatus;
import com.example.library.mapper.db.LoanDbMapper;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.MediaRepository;
import com.example.library.repository.PatronRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class CirculationService {

    private final LoanRepository loanRepository;
    private final PatronRepository patronRepository;
    private final MediaRepository mediaRepository;
    private final LoanDbMapper loanMapper;

    public CirculationService(LoanRepository loanRepository, 
                              PatronRepository patronRepository, 
                              MediaRepository mediaRepository, 
                              LoanDbMapper loanMapper) {
        this.loanRepository = loanRepository;
        this.patronRepository = patronRepository;
        this.mediaRepository = mediaRepository;
        this.loanMapper = loanMapper;
    }

    @Transactional
    public Loan checkOut(String patronId, String itemId) {
        // Simple logic: verify patron and item exist
        patronRepository.findById(patronId)
                .orElseThrow(() -> new RuntimeException("Patron not found"));
        var itemRecord = mediaRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (itemRecord.getAvailableCopies() <= 0) {
            throw new RuntimeException("Item not available");
        }

        // Create loan
        String loanId = UUID.randomUUID().toString();
        LoansRecord loanRecord = new LoansRecord();
        loanRecord.setId(loanId);
        loanRecord.setPatronId(patronId);
        loanRecord.setItemId(itemId);
        loanRecord.setDueDate(OffsetDateTime.now().plusDays(14).toLocalDateTime());
        loanRecord.setStatus(LoanStatus.ACTIVE.name());
        
        loanRepository.save(loanRecord);

        // Update item availability
        itemRecord.setAvailableCopies(itemRecord.getAvailableCopies() - 1);
        mediaRepository.save(itemRecord);

        // Return full domain object
        var fullRecord = loanRepository.findLoanById(loanId).orElseThrow();
        return loanMapper.toDomain(fullRecord.value1(), fullRecord.value2(), fullRecord.value3());
    }

    @Transactional
    public void checkIn(String loanId) {
        var loanRecord = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        if (LoanStatus.RETURNED.name().equals(loanRecord.getStatus())) {
            return;
        }

        loanRecord.setStatus(LoanStatus.RETURNED.name());
        loanRecord.setReturnedAt(OffsetDateTime.now().toLocalDateTime());
        loanRepository.save(loanRecord);

        // Update item availability
        var itemRecord = mediaRepository.findById(loanRecord.getItemId()).orElseThrow();
        itemRecord.setAvailableCopies(itemRecord.getAvailableCopies() + 1);
        mediaRepository.save(itemRecord);
    }
}
