package com.example.library.repository;

import com.example.library.db.tables.records.PatronsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.example.library.db.Tables.PATRONS;

@Repository
public class PatronRepository {

    private final DSLContext dsl;

    public PatronRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<PatronsRecord> findById(String id) {
        return dsl.selectFrom(PATRONS)
                .where(PATRONS.ID.eq(id))
                .fetchOptional();
    }

    public void save(PatronsRecord record) {
        record.attach(dsl.configuration());
        record.store();
    }
}
