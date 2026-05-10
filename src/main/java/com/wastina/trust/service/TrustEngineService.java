package com.wastina.trust.service;

import com.wastina.trust.dto.TrustScoreDto;
import com.wastina.trust.graph.PersonNode;
import com.wastina.trust.graph.PersonNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrustEngineService {

    private final PersonNodeRepository personNodeRepository;

    public TrustScoreDto calculateTrustScore(UUID workerId) {
        Optional<PersonNode> optionalWorker = personNodeRepository.findById(workerId);
        if (optionalWorker.isEmpty()) {
            throw new IllegalArgumentException("Worker not found in the trust network");
        }

        PersonNode worker = optionalWorker.get();
        int score = 0;
        List<String> breakdown = new ArrayList<>();

        // Rule 1: Verified Fayda ID (+20)
        if (worker.isHasFaydaId()) {
            score += 20;
            breakdown.add("Verified Fayda ID: +20 points");
        } else {
            breakdown.add("Fayda ID not verified: 0 points");
        }

        // Rule 2: 2+ Years of Good Work (+50)
        if (worker.getYearsOfGoodWork() >= 2) {
            score += 50;
            breakdown.add("2+ Years of Verified Employment: +50 points");
        } else if (worker.getYearsOfGoodWork() == 1) {
            score += 20; // Partial score for 1 year
            breakdown.add("1 Year of Verified Employment: +20 points");
        } else {
            breakdown.add("Less than 1 year of employment history: 0 points");
        }

        // Rule 3: Guarantor is a Government Employee (+30)
        Optional<PersonNode> guarantorOpt = personNodeRepository.findGuarantorForWorker(workerId);
        if (guarantorOpt.isPresent()) {
            PersonNode guarantor = guarantorOpt.get();
            if (guarantor.isGovEmployee()) {
                score += 30;
                breakdown.add("Guarantor is a Government Employee: +30 points");
            } else {
                score += 10; // Base score for having any verified guarantor
                breakdown.add("Verified Guarantor (Non-Gov): +10 points");
            }
        } else {
            breakdown.add("No Verified Guarantor found: 0 points");
        }

        // Ensure score doesn't exceed 100
        score = Math.min(score, 100);
        breakdown.add(0, "Total Score Calculated: " + score);

        return new TrustScoreDto(score, workerId.toString(), breakdown);
    }

    // Utility to sync from MySQL to Neo4j
    public PersonNode syncUserToGraph(UUID userId, String name, boolean hasFaydaId, boolean isGovEmployee, int yearsOfWork) {
        PersonNode node = new PersonNode(userId, name, hasFaydaId, isGovEmployee, yearsOfWork);
        return personNodeRepository.save(node);
    }

    // Utility to establish a guarantee relationship in Neo4j
    public void establishGuaranteeLink(UUID guarantorId, UUID workerId) {
        PersonNode guarantor = personNodeRepository.findById(guarantorId)
                .orElseThrow(() -> new IllegalArgumentException("Guarantor not found"));
        PersonNode worker = personNodeRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        guarantor.guarantees(worker);
        personNodeRepository.save(guarantor);
    }
}
