package com.wesleyhome.library.server;

import com.wesleyhome.library.server.api.circulation.CheckOutItemApi;
import com.wesleyhome.library.server.mapper.api.CirculationApiMapper;
import com.wesleyhome.library.server.model.circulation.CheckOutItemOutputDTO;
import com.wesleyhome.library.server.model.circulation.CheckOutRequestDTO;
import com.wesleyhome.library.server.service.CirculationService;
import org.springframework.stereotype.Service;

@Service
public class CirculationApiAdapter implements CheckOutItemApi {

    private final CirculationService circulationService;
    private final CirculationApiMapper apiMapper;

    public CirculationApiAdapter(CirculationService circulationService, CirculationApiMapper apiMapper) {
        this.circulationService = circulationService;
        this.apiMapper = apiMapper;
    }

    @Override
    public CheckOutItemOutputDTO checkOutItem(CheckOutRequestDTO request) {
        var loan = circulationService.checkOut(request.patronId(), request.itemId());
        return apiMapper.toCheckOutItemOutputDTO(loan);
    }

//    @Override
//    public void checkInItem(String loanId) {
//        circulationService.checkIn(loanId);
//    }
}
