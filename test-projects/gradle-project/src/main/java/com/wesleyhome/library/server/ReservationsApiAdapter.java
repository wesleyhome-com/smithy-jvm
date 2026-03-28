package com.wesleyhome.library.server;

import com.wesleyhome.library.server.api.reservations.CancelReservationApi;
import com.wesleyhome.library.server.api.reservations.ListReservationsApi;
import com.wesleyhome.library.server.api.reservations.ReserveComputerApi;
import com.wesleyhome.library.server.mapper.api.ReservationApiMapper;
import com.wesleyhome.library.server.model.reservations.CancelReservationOutputDTO;
import com.wesleyhome.library.server.model.reservations.ComputerReservationDetailsDTO;
import com.wesleyhome.library.server.model.reservations.ListReservationsOutputDTO;
import com.wesleyhome.library.server.model.reservations.ReserveComputerOutputDTO;
import com.wesleyhome.library.server.service.ReservationService;
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
