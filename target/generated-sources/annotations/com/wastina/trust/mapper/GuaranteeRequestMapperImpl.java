package com.wastina.trust.mapper;

import com.wastina.trust.dto.GuaranteeRequestDto;
import com.wastina.trust.dto.GuaranteeResponseDto;
import com.wastina.trust.entity.GuaranteeRequest;
import com.wastina.trust.entity.User;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-06T14:54:06+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class GuaranteeRequestMapperImpl implements GuaranteeRequestMapper {

    @Override
    public GuaranteeRequest toEntity(GuaranteeRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        GuaranteeRequest guaranteeRequest = new GuaranteeRequest();

        guaranteeRequest.setGuarantorPhone( dto.getGuarantorPhone() );

        return guaranteeRequest;
    }

    @Override
    public GuaranteeResponseDto toDto(GuaranteeRequest entity) {
        if ( entity == null ) {
            return null;
        }

        GuaranteeResponseDto guaranteeResponseDto = new GuaranteeResponseDto();

        guaranteeResponseDto.setRequesterId( entityRequesterId( entity ) );
        guaranteeResponseDto.setCreatedAt( entity.getCreatedAt() );
        guaranteeResponseDto.setExpiresAt( entity.getExpiresAt() );
        guaranteeResponseDto.setGuarantorPhone( entity.getGuarantorPhone() );
        guaranteeResponseDto.setId( entity.getId() );
        if ( entity.getStatus() != null ) {
            guaranteeResponseDto.setStatus( entity.getStatus().name() );
        }

        return guaranteeResponseDto;
    }

    private UUID entityRequesterId(GuaranteeRequest guaranteeRequest) {
        if ( guaranteeRequest == null ) {
            return null;
        }
        User requester = guaranteeRequest.getRequester();
        if ( requester == null ) {
            return null;
        }
        UUID id = requester.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
