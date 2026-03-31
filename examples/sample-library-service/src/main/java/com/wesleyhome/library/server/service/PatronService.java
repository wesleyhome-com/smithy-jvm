package com.wesleyhome.library.server.service;

import com.wesleyhome.library.db.tables.records.PatronsRecord;
import com.wesleyhome.library.server.domain.Patron;
import com.wesleyhome.library.server.mapper.db.PatronDbMapper;
import com.wesleyhome.library.server.repository.PatronRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PatronService {

    private final PatronRepository patronRepository;
    private final PatronDbMapper patronMapper;

    public PatronService(PatronRepository patronRepository, PatronDbMapper patronMapper) {
        this.patronRepository = patronRepository;
        this.patronMapper = patronMapper;
    }

    public Optional<Patron> getPatron(String id) {
        return patronRepository.findById(id)
                .map(patronMapper::toDomain);
    }

    @Transactional
    public Patron updateContact(String id, String email, String phone) {
        PatronsRecord record = patronRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patron not found: " + id));

        record.setEmail(email);
        record.setPhone(phone);

        patronRepository.save(record);
        return patronMapper.toDomain(record);
    }
}
