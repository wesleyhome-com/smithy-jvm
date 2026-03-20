package com.example.library.mapper.api;

import com.example.library.domain.*;
import com.example.library.generated.model.circulation.CheckOutItemOutputDTO;
import com.example.library.generated.model.circulation.LoanRecordDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CirculationApiMapperTest {

    @Autowired
    private CirculationApiMapper mapper;

    @Test
    void toCheckOutItemOutputDTO_shouldMapLoanCorrectly() {
        Patron patron = new Patron("P1", "John", "j@e.com", "555", MembershipStatus.ACTIVE);
        MediaItem.Book item = new MediaItem.Book("B1", "Title", "Author", "ISBN", 100, 1, 1);
        Loan loan = new Loan("L1", patron, item, OffsetDateTime.now(), null, LoanStatus.ACTIVE);

        CheckOutItemOutputDTO result = mapper.toCheckOutItemOutputDTO(loan);

        assertThat(result.loan().loanId()).isEqualTo("L1");
        assertThat(result.loan().dueDate()).isNotNull();
    }

    @Test
    void toLoanRecordDTO_shouldMapLoanCorrectly() {
        Patron patron = new Patron("P1", "John", "j@e.com", "555", MembershipStatus.ACTIVE);
        MediaItem.Book item = new MediaItem.Book("B1", "Title", "Author", "ISBN", 100, 1, 1);
        Loan loan = new Loan("L1", patron, item, OffsetDateTime.now(), null, LoanStatus.ACTIVE);

        LoanRecordDTO result = mapper.toLoanRecordDTO(loan);

        assertThat(result.loanId()).isEqualTo("L1");
    }
}
