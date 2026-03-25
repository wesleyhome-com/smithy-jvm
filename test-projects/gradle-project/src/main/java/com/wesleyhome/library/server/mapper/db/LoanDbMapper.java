package com.wesleyhome.library.server.mapper.db;

import com.wesleyhome.library.db.tables.records.LoansRecord;
import com.wesleyhome.library.db.tables.records.MediaItemsRecord;
import com.wesleyhome.library.db.tables.records.PatronsRecord;
import com.wesleyhome.library.server.domain.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING, 
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {PatronDbMapper.class, MediaItemDbMapper.class, CommonDbMapper.class}
)
public interface LoanDbMapper {

    @Mapping(target = "id", source = "loan.id")
    @Mapping(target = "status", source = "loan.status")
    @Mapping(target = "patron", source = "patron")
    @Mapping(target = "item", source = "item")
    Loan toDomain(LoansRecord loan, PatronsRecord patron, MediaItemsRecord item);

    @Mapping(target = "patronId", source = "patron.id")
    @Mapping(target = "itemId", source = "item.id")
    LoansRecord toRecord(Loan domain);
}
