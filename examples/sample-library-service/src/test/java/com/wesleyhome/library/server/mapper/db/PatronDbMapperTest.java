package com.wesleyhome.library.server.mapper.db;

import com.wesleyhome.library.db.tables.records.PatronsRecord;
import com.wesleyhome.library.server.domain.MembershipStatus;
import com.wesleyhome.library.server.domain.Patron;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PatronDbMapperTest {

    @Autowired
    private PatronDbMapper mapper;

    @Test
    void toDomain_shouldMapPatronCorrectly() {
        PatronsRecord record = new PatronsRecord();
        record.setId("P1");
        record.setName("Justin Wesley");
        record.setEmail("justin@example.com");
        record.setMembershipStatus("ACTIVE");

        Patron result = mapper.toDomain(record);

        assertThat(result.id()).isEqualTo("P1");
        assertThat(result.name()).isEqualTo("Justin Wesley");
        assertThat(result.membershipStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void toRecord_shouldMapPatronToRecordCorrectly() {
        Patron patron = new Patron("P1", "Justin Wesley", "justin@example.com", "555-1212", MembershipStatus.SUSPENDED);

        PatronsRecord record = mapper.toRecord(patron);

        assertThat(record.getId()).isEqualTo("P1");
        assertThat(record.getMembershipStatus()).isEqualTo("SUSPENDED");
    }
}
