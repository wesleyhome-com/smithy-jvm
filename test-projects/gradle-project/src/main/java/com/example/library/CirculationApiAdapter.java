package com.example.library;

import com.example.library.generated.api.circulation.CheckOutItemApi;
import com.example.library.generated.model.circulation.CheckOutItemOutputDTO;
import com.example.library.generated.model.circulation.CheckOutRequestDTO;
import com.example.library.mapper.api.CirculationApiMapper;
import com.example.library.service.CirculationService;
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
