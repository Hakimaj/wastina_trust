package com.wastina.trust.mapper;

import com.wastina.trust.dto.DigitalContractRequestDto;
import com.wastina.trust.dto.DigitalContractResponseDto;
import com.wastina.trust.entity.DigitalContract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DigitalContractMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "signedDate", ignore = true)
    @Mapping(target = "worker", ignore = true)
    @Mapping(target = "guarantor", ignore = true)
    @Mapping(target = "employer", ignore = true)
    DigitalContract toEntity(DigitalContractRequestDto dto);

    @Mapping(target = "workerId", source = "worker.id")
    @Mapping(target = "guarantorId", source = "guarantor.id")
    @Mapping(target = "employerId", source = "employer.id")
    DigitalContractResponseDto toDto(DigitalContract entity);
}
