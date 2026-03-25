package com.wesleyhome.library.server.mapper.api;

import com.wesleyhome.library.server.domain.Reservation;
import com.wesleyhome.library.server.model.reservations.ListReservationsOutputDTO;
import com.wesleyhome.library.server.model.reservations.ReservationDTO;
import com.wesleyhome.library.server.model.reservations.ReservationStatusDTO;
import com.wesleyhome.library.server.model.reservations.ReserveComputerOutputDTO;
import com.wesleyhome.library.server.domain.ReservationStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReservationApiMapper {

    @Mapping(target = "reservation", source = ".")
    ReserveComputerOutputDTO toReserveComputerOutputDTO(Reservation domain);

    @Mapping(target = "reservationId", source = "id")
    ReservationDTO toApi(Reservation domain);

    @ValueMapping(source = "COMPLETED", target = "CONFIRMED")
    ReservationStatusDTO mapStatus(ReservationStatus status);

    List<ReservationDTO> mapList(List<Reservation> reservations);

    default ListReservationsOutputDTO toListReservationsOutputDTO(List<Reservation> reservations) {
        if (reservations == null) return null;
        return new ListReservationsOutputDTO(mapList(reservations));
    }
}
