package com.example.library.repository;

import com.example.library.db.tables.records.LoansRecord;
import com.example.library.db.tables.records.MediaItemsRecord;
import com.example.library.db.tables.records.PatronsRecord;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.library.db.Tables.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // Roll back changes after each test
class LoanRepositoryTest {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void findLoanById_shouldReturnJoinedRecord() {
        // Arrange
        String patronId = "p2";
        String mediaId = "m2";
        String loanId = "L2";
        LocalDateTime dueDate = LocalDateTime.now().plusDays(14).withNano(0); // Nano precision can be tricky with DBs

        insertTestData(patronId, mediaId, loanId, dueDate);

        // Act
        Optional<Record3<LoansRecord, PatronsRecord, MediaItemsRecord>> result = loanRepository.findLoanById(loanId);

        // Assert
        assertThat(result).isPresent().hasValueSatisfying(record -> {
            // Extract typed records
            LoansRecord loan = record.value1();
            PatronsRecord patron = record.value2();
            MediaItemsRecord item = record.value3();

            // Detailed Assertions for each record
            assertThat(loan)
                .returns(loanId, LoansRecord::getId)
                .returns(patronId, LoansRecord::getPatronId)
                .returns(mediaId, LoansRecord::getItemId);
                
            assertThat(patron)
                .returns("John Doe", PatronsRecord::getName)
                .returns("john@example.com", PatronsRecord::getEmail);

            assertThat(item)
                .returns("The Hobbit", MediaItemsRecord::getTitle)
                .returns("BOOK", MediaItemsRecord::getMediaType);
        });
    }

    private void insertTestData(String patronId, String mediaId, String loanId, LocalDateTime dueDate) {
        dsl.insertInto(PATRONS)
                .set(PATRONS.ID, patronId)
                .set(PATRONS.NAME, "John Doe")
                .set(PATRONS.EMAIL, "john@example.com")
                .execute();

        dsl.insertInto(MEDIA_ITEMS)
                .set(MEDIA_ITEMS.ID, mediaId)
                .set(MEDIA_ITEMS.MEDIA_TYPE, "BOOK")
                .set(MEDIA_ITEMS.TITLE, "The Hobbit")
                .execute();

        dsl.insertInto(LOANS)
                .set(LOANS.ID, loanId)
                .set(LOANS.PATRON_ID, patronId)
                .set(LOANS.ITEM_ID, mediaId)
                .set(LOANS.DUE_DATE, dueDate)
                .execute();
    }
}
