package com.wesleyhome.library.server.mapper.db;

import com.wesleyhome.library.db.tables.records.PatronsRecord;
import com.wesleyhome.library.server.domain.Patron;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING, 
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {CommonDbMapper.class}
)
public interface PatronDbMapper {

    Patron toDomain(PatronsRecord record);

    PatronsRecord toRecord(Patron domain) ;
}
