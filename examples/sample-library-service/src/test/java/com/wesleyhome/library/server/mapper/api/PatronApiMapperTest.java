package com.wesleyhome.library.server.mapper.api;

import com.wesleyhome.library.server.domain.MembershipStatus;
import com.wesleyhome.library.server.domain.Patron;
import com.wesleyhome.library.server.patron.model.ContactDetailsDTO;
import com.wesleyhome.library.server.patron.model.GetPatronInfoOutputDTO;
import com.wesleyhome.library.server.patron.model.MembershipStatusDTO;
import com.wesleyhome.library.server.patron.model.PatronDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PatronApiMapperTest {

    @Autowired
    private PatronApiMapper mapper;

    @Test
    void toApi_shouldMapFlatDomainToNestedApi() {
        Patron domain = new Patron("P1", "John", "john@example.com", "555", MembershipStatus.ACTIVE);

        PatronDTO result = mapper.toApi(domain);

        assertThat(result.id()).isEqualTo("P1");
        assertThat(result.name()).isEqualTo("John");
        assertThat(result.contact().email()).isEqualTo("john@example.com");
        assertThat(result.membershipStatus()).isEqualTo(MembershipStatusDTO.ACTIVE);
    }

    @Test
    void toDomain_shouldMapNestedApiToFlatDomain() {
        ContactDetailsDTO contact = new ContactDetailsDTO("john@example.com", "555");
        PatronDTO apiDto = new PatronDTO("P1", "John", contact, MembershipStatusDTO.SUSPENDED);

        Patron result = mapper.toDomain(apiDto);

        assertThat(result.id()).isEqualTo("P1");
        assertThat(result.email()).isEqualTo("john@example.com");
        assertThat(result.phone()).isEqualTo("555");
        assertThat(result.membershipStatus()).isEqualTo(MembershipStatus.SUSPENDED);
    }

    @Test
    void toGetPatronInfoOutputDTO_shouldMapCorrectly() {
        Patron domain = new Patron("P1", "John", "john@example.com", "555", MembershipStatus.ACTIVE);

        GetPatronInfoOutputDTO result = mapper.toGetPatronInfoOutputDTO(domain);

        assertThat(result.patron().id()).isEqualTo("P1");
        assertThat(result.patron().contact().email()).isEqualTo("john@example.com");
    }
}
