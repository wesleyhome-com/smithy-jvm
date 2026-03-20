package com.example.library.mapper.db;

import com.example.library.db.tables.records.PatronsRecord;
import com.example.library.domain.Patron;
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
