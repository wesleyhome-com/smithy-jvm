package com.example.library;

import com.example.library.generated.api.reservations.*;
import com.example.library.generated.model.reservations.*;
import com.example.library.mapper.api.ReservationApiMapper;
import com.example.library.service.ReservationService;
import org.springframework.stereotype.Service;

@Service
public class ReservationsApiAdapter implements ReserveComputerApi, ListReservationsApi, CancelReservationApi {

    private final ReservationService reservationService;
    private final ReservationApiMapper apiMapper;

    public ReservationsApiAdapter(ReservationService reservationService, ReservationApiMapper apiMapper) {
        this.reservationService = reservationService;
        this.apiMapper = apiMapper;
    }

    @Override
    public ReserveComputerOutputDTO reserveComputer(ComputerReservationDetailsDTO request) {
        var res = reservationService.reserve(
            request.patronId(), 
            request.computerId(), 
            request.startTime(), 
            request.durationMinutes()
        );
        return apiMapper.toReserveComputerOutputDTO(res);
    }

    @Override
    public ListReservationsOutputDTO listReservations(String patronId) {
        var list = reservationService.listForPatron(patronId);
        return apiMapper.toListReservationsOutputDTO(list);
    }

    @Override
    public CancelReservationOutputDTO cancelReservation(String id) {
        reservationService.cancel(id);
        return new CancelReservationOutputDTO();
    }
}
