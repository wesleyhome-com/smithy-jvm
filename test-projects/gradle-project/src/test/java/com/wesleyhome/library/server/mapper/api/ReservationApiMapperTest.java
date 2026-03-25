package com.wesleyhome.library.server.mapper.api;

import com.wesleyhome.library.server.domain.MembershipStatus;
import com.wesleyhome.library.server.domain.Patron;
import com.wesleyhome.library.server.domain.Reservation;
import com.wesleyhome.library.server.domain.ReservationStatus;
import com.wesleyhome.library.server.model.reservations.ReservationDTO;
import com.wesleyhome.library.server.model.reservations.ReservationStatusDTO;
import com.wesleyhome.library.server.model.reservations.ReserveComputerOutputDTO;
import com.wesleyhome.library.server.model.reservations.ListReservationsOutputDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationApiMapperTest {

    @Autowired
    private ReservationApiMapper mapper;

    @Test
    void toReserveComputerOutputDTO_shouldMapCorrectly() {
        Patron patron = new Patron("P1", "John", "j@e.com", "555", MembershipStatus.ACTIVE);
        Reservation domain = new Reservation("R1", patron, "Comp1", Instant.now(), 60, ReservationStatus.CONFIRMED);

        ReserveComputerOutputDTO result = mapper.toReserveComputerOutputDTO(domain);

        assertThat(result.reservation().reservationId()).isEqualTo("R1");
        assertThat(result.reservation().status()).isEqualTo(ReservationStatusDTO.CONFIRMED);
    }

    @Test
    void toListReservationsOutputDTO_shouldMapListCorrectly() {
        Patron patron = new Patron("P1", "John", "j@e.com", "555", MembershipStatus.ACTIVE);
        Reservation r1 = new Reservation("R1", patron, "Comp1", Instant.now(), 60, ReservationStatus.CONFIRMED);
        Reservation r2 = new Reservation("R2", patron, "Comp2", Instant.now(), 30, ReservationStatus.CANCELLED);

        ListReservationsOutputDTO result = mapper.toListReservationsOutputDTO(List.of(r1, r2));

        assertThat(result.reservations()).hasSize(2);
        assertThat(result.reservations().get(0).reservationId()).isEqualTo("R1");
        assertThat(result.reservations().get(1).status()).isEqualTo(ReservationStatusDTO.CANCELLED);
    }

    @Test
    void mapStatus_shouldHandleCompletedFallback() {
        ReservationStatusDTO status = mapper.mapStatus(ReservationStatus.COMPLETED);
        assertThat(status).isEqualTo(ReservationStatusDTO.CONFIRMED);
    }
}
