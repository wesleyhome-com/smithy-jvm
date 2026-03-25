package com.example.library.mapper.api;

import com.example.library.domain.Patron;
import com.example.library.generated.model.patron.GetPatronInfoOutputDTO;
import com.example.library.generated.model.patron.MembershipStatusDTO;
import com.example.library.generated.model.patron.PatronDTO;
import com.example.library.generated.model.patron.UpdatePatronContactOutputDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PatronApiMapper {

    @Mapping(target = "membershipStatus", source = "membershipStatus")
    @Mapping(target = "contact.email", source = "email")
    @Mapping(target = "contact.phone", source = "phone")
    PatronDTO toApi(Patron domain);

    @Mapping(target = "email", source = "contact.email")
    @Mapping(target = "phone", source = "contact.phone")
    @Mapping(target = "membershipStatus", source = "membershipStatus")
    Patron toDomain(PatronDTO api);

    @ValueMapping(source = "UNKNOWN_TO_SDK_VERSION", target = "EXPIRED")
    com.example.library.domain.MembershipStatus mapStatus(MembershipStatusDTO apiStatus);

    @Mapping(target = "patron", source = ".")
    GetPatronInfoOutputDTO toGetPatronInfoOutputDTO(Patron domain);

    @Mapping(target = "patron", source = ".")
    UpdatePatronContactOutputDTO toUpdatePatronContactOutputDTO(Patron domain);
}
