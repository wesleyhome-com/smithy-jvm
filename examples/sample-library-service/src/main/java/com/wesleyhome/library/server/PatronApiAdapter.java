package com.wesleyhome.library.server;

import com.wesleyhome.library.server.mapper.api.PatronApiMapper;
import com.wesleyhome.library.server.patron.api.GetPatronInfoApi;
import com.wesleyhome.library.server.patron.api.UpdatePatronContactApi;
import com.wesleyhome.library.server.patron.model.ContactDetailsDTO;
import com.wesleyhome.library.server.patron.model.GetPatronInfoOutputDTO;
import com.wesleyhome.library.server.patron.model.UpdatePatronContactOutputDTO;
import com.wesleyhome.library.server.service.PatronService;
import org.springframework.stereotype.Service;

@Service
public class PatronApiAdapter implements GetPatronInfoApi, UpdatePatronContactApi {

    private final PatronService patronService;
    private final PatronApiMapper apiMapper;

    public PatronApiAdapter(PatronService patronService, PatronApiMapper apiMapper) {
        this.patronService = patronService;
        this.apiMapper = apiMapper;
    }

    @Override
    public GetPatronInfoOutputDTO getPatronInfo(String id) {
        return patronService.getPatron(id)
                .map(apiMapper::toGetPatronInfoOutputDTO)
                .orElseThrow(() -> new RuntimeException("Patron not found"));
    }

    @Override
    public UpdatePatronContactOutputDTO updatePatronContact(String id, ContactDetailsDTO contact) {
        var updated = patronService.updateContact(id, contact.email(), contact.phone());
        return apiMapper.toUpdatePatronContactOutputDTO(updated);
    }
}
