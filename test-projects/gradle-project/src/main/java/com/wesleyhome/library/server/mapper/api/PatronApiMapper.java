package com.wesleyhome.library.server.mapper.api;

import com.wesleyhome.library.server.domain.Patron;
import com.wesleyhome.library.server.model.patron.GetPatronInfoOutputDTO;
import com.wesleyhome.library.server.model.patron.MembershipStatusDTO;
import com.wesleyhome.library.server.model.patron.PatronDTO;
import com.wesleyhome.library.server.model.patron.UpdatePatronContactOutputDTO;
import com.wesleyhome.library.server.domain.MembershipStatus;
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
    MembershipStatus mapStatus(MembershipStatusDTO apiStatus);

    @Mapping(target = "patron", source = ".")
    GetPatronInfoOutputDTO toGetPatronInfoOutputDTO(Patron domain);

    @Mapping(target = "patron", source = ".")
    UpdatePatronContactOutputDTO toUpdatePatronContactOutputDTO(Patron domain);
}
