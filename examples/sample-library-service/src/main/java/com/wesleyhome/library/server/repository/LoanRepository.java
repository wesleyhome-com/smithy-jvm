package com.wesleyhome.library.server.repository;

import com.wesleyhome.library.db.tables.records.LoansRecord;
import com.wesleyhome.library.db.tables.records.MediaItemsRecord;
import com.wesleyhome.library.db.tables.records.PatronsRecord;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.wesleyhome.library.db.Tables.*;

@Repository
public class LoanRepository {

    private final DSLContext dsl;

    public LoanRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<LoansRecord> findById(String id) {
        return dsl.selectFrom(LOANS)
                .where(LOANS.ID.eq(id))
                .fetchOptional();
    }

    public Optional<Record3<LoansRecord, PatronsRecord, MediaItemsRecord>> findLoanById(String id) {
        return dsl.select(LOANS, PATRONS, MEDIA_ITEMS)
                .from(LOANS)
                .join(PATRONS).on(LOANS.PATRON_ID.eq(PATRONS.ID))
                .join(MEDIA_ITEMS).on(LOANS.ITEM_ID.eq(MEDIA_ITEMS.ID))
                .where(LOANS.ID.eq(id))
                .fetchOptional();
    }

    public void save(LoansRecord record) {
        record.attach(dsl.configuration());
        record.store();
    }
}
