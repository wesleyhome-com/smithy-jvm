package com.example.library;

import com.example.library.generated.api.patron.*;
import com.example.library.generated.model.patron.*;
import com.example.library.mapper.api.PatronApiMapper;
import com.example.library.service.PatronService;
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
