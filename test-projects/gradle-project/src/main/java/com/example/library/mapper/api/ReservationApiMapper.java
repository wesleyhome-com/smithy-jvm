package com.example.library.mapper.api;

import com.example.library.domain.Reservation;
import com.example.library.generated.model.reservations.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReservationApiMapper {

    @Mapping(target = "reservation", source = ".")
    ReserveComputerOutputDTO toReserveComputerOutputDTO(Reservation domain);

    @Mapping(target = "reservationId", source = "id")
    ReservationDTO toApi(Reservation domain);

    @ValueMapping(source = "COMPLETED", target = "CONFIRMED")
    ReservationStatusDTO mapStatus(com.example.library.domain.ReservationStatus status);

    List<ReservationDTO> mapList(List<Reservation> reservations);

    default ListReservationsOutputDTO toListReservationsOutputDTO(List<Reservation> reservations) {
        if (reservations == null) return null;
        return new ListReservationsOutputDTO(mapList(reservations));
    }
}
