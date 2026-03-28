package com.wesleyhome.library.server.service;

import com.wesleyhome.library.db.tables.records.ReservationsRecord;
import com.wesleyhome.library.server.domain.Patron;
import com.wesleyhome.library.server.domain.Reservation;
import com.wesleyhome.library.server.domain.ReservationStatus;
import com.wesleyhome.library.server.mapper.db.PatronDbMapper;
import com.wesleyhome.library.server.repository.PatronRepository;
import com.wesleyhome.library.server.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final PatronRepository patronRepository;
    private final PatronDbMapper patronMapper;

    public ReservationService(ReservationRepository reservationRepository,
                              PatronRepository patronRepository,
                              PatronDbMapper patronMapper) {
        this.reservationRepository = reservationRepository;
        this.patronRepository = patronRepository;
        this.patronMapper = patronMapper;
    }

    @Transactional
    public Reservation reserve(String patronId, String resourceId, Instant startTime, int duration) {
        Patron patron = patronRepository.findById(patronId)
                .map(patronMapper::toDomain)
                .orElseThrow(() -> new RuntimeException("Patron not found"));

        String id = UUID.randomUUID().toString();
        ReservationsRecord record = new ReservationsRecord();
        record.setId(id);
        record.setPatronId(patronId);
        record.setResourceId(resourceId);
        record.setStartTime(startTime.atOffset(ZoneOffset.UTC).toLocalDateTime());
        record.setDurationMinutes(duration);
        record.setStatus(ReservationStatus.CONFIRMED.name());

        reservationRepository.save(record);

        return new Reservation(id, patron, resourceId, startTime, duration, ReservationStatus.CONFIRMED);
    }

    public List<Reservation> listForPatron(String patronId) {
        Patron patron = patronRepository.findById(patronId)
                .map(patronMapper::toDomain)
                .orElseThrow(() -> new RuntimeException("Patron not found"));

        return reservationRepository.findByPatronId(patronId).stream()
                .map(r -> new Reservation(
                        r.getId(),
                        patron,
                        r.getResourceId(),
                        Instant.now(), // Simplified for example
                        r.getDurationMinutes(),
                        ReservationStatus.valueOf(r.getStatus())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancel(String id) {
        reservationRepository.deleteById(id);
    }
}
