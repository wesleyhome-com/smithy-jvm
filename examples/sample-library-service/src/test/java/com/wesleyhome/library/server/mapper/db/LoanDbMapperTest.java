package com.wesleyhome.library.server.mapper.db;

import com.wesleyhome.library.db.tables.records.LoansRecord;
import com.wesleyhome.library.db.tables.records.MediaItemsRecord;
import com.wesleyhome.library.db.tables.records.PatronsRecord;
import com.wesleyhome.library.server.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LoanDbMapperTest {

    @Autowired
    private LoanDbMapper mapper;

    @Test
    void toDomain_shouldMapJoinedRecordsCorrectly() {
        LocalDateTime now = LocalDateTime.now();

        LoansRecord loanRec = new LoansRecord();
        loanRec.setId("L1");
        loanRec.setStatus("ACTIVE");
        loanRec.setDueDate(now.plusDays(7));

        PatronsRecord patronRec = new PatronsRecord();
        patronRec.setId("P1");
        patronRec.setName("John");

        MediaItemsRecord itemRec = new MediaItemsRecord();
        itemRec.setId("M1");
        itemRec.setMediaType("BOOK");
        itemRec.setTitle("Book Title");

        Loan result = mapper.toDomain(loanRec, patronRec, itemRec);

        assertThat(result.id()).isEqualTo("L1");
        assertThat(result.patron().id()).isEqualTo("P1");
        assertThat(result.item().id()).isEqualTo("M1");
        assertThat(result.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(result.dueDate()).isEqualTo(now.plusDays(7).toInstant(ZoneOffset.UTC));
    }

    @Test
    void toRecord_shouldMapLoanToRecordCorrectly() {
        Patron patron = new Patron("P1", "John", "john@example.com", null, MembershipStatus.ACTIVE);
        MediaItem.Book book = new MediaItem.Book("M1", "Title", "Author", "ISBN", 100, 1, 1);
        Loan loan = new Loan("L1", patron, book, Instant.now(), null, LoanStatus.OVERDUE);

        LoansRecord record = mapper.toRecord(loan);

        assertThat(record.getId()).isEqualTo("L1");
        assertThat(record.getPatronId()).isEqualTo("P1");
        assertThat(record.getItemId()).isEqualTo("M1");
        assertThat(record.getStatus()).isEqualTo("OVERDUE");
    }
}
