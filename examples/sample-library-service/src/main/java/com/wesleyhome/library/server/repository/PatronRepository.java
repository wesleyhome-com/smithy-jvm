package com.wesleyhome.library.server.repository;

import com.wesleyhome.library.db.tables.records.PatronsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.wesleyhome.library.db.Tables.PATRONS;

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
