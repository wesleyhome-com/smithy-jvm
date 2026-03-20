package com.example.library.mapper.api;

import com.example.library.domain.Loan;
import com.example.library.generated.model.circulation.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CirculationApiMapper {

    @Mapping(target = "loan", source = ".")
    CheckOutItemOutputDTO toCheckOutItemOutputDTO(Loan domain);

    @Mapping(target = "loanId", source = "id")
    LoanRecordDTO toLoanRecordDTO(Loan domain);
}
