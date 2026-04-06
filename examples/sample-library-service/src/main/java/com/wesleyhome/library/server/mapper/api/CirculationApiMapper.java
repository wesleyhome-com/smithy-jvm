package com.wesleyhome.library.server.mapper.api;

import com.wesleyhome.library.server.circulation.model.CheckOutItemOutputDTO;
import com.wesleyhome.library.server.circulation.model.LoanRecordDTO;
import com.wesleyhome.library.server.domain.Loan;
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
