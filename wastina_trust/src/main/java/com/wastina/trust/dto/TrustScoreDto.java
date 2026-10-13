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
    private String confidence;
    private String evidenceStatus;
    private String policyVersion;
    private int evidenceCount;

    public TrustScoreDto(int totalScore, String workerId, List<String> scoreBreakdown) {
        this.totalScore = totalScore;
        this.workerId = workerId;
        this.scoreBreakdown = scoreBreakdown;
        this.confidence = "MEDIUM";
        this.evidenceStatus = "VERIFIED";
        this.policyVersion = "v1";
        this.evidenceCount = scoreBreakdown == null ? 0 : scoreBreakdown.size();
    }
}
