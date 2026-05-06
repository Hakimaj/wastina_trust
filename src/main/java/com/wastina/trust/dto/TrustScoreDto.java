package com.wastina.trust.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustScoreDto {
    private int totalScore;
    private String workerId;
    private List<String> scoreBreakdown;
}
