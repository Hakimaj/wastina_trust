package com.wastina.trust.mapper;

import com.wastina.trust.dto.ProfessionRequestDto;
import com.wastina.trust.dto.ProfessionResponseDto;
import com.wastina.trust.entity.Profession;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-09T00:19:08+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
@Component
public class ProfessionMapperImpl implements ProfessionMapper {

    @Override
    public Profession toEntity(ProfessionRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        Profession profession = new Profession();

        profession.setName( dto.getName() );

        return profession;
    }

    @Override
    public ProfessionResponseDto toDto(Profession entity) {
        if ( entity == null ) {
            return null;
        }

        ProfessionResponseDto professionResponseDto = new ProfessionResponseDto();

        professionResponseDto.setId( entity.getId() );
        professionResponseDto.setName( entity.getName() );

        return professionResponseDto;
    }
}
