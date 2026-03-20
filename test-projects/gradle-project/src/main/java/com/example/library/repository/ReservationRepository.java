package com.example.library.repository;

import com.example.library.db.tables.records.ReservationsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.library.db.Tables.RESERVATIONS;

@Repository
public class ReservationRepository {

    private final DSLContext dsl;

    public ReservationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<ReservationsRecord> findByPatronId(String patronId) {
        return dsl.selectFrom(RESERVATIONS)
                .where(RESERVATIONS.PATRON_ID.eq(patronId))
                .fetch();
    }

    public Optional<ReservationsRecord> findById(String id) {
        return dsl.selectFrom(RESERVATIONS)
                .where(RESERVATIONS.ID.eq(id))
                .fetchOptional();
    }

    public void save(ReservationsRecord record) {
        record.attach(dsl.configuration());
        record.store();
    }

    public void deleteById(String id) {
        dsl.deleteFrom(RESERVATIONS)
                .where(RESERVATIONS.ID.eq(id))
                .execute();
    }
}
