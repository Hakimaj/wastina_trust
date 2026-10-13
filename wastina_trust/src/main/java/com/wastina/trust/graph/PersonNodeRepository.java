package com.wastina.trust.graph;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonNodeRepository extends Neo4jRepository<PersonNode, UUID> {

    // Custom query to find the guarantor for a specific person
    @Query("MATCH (guarantor:Person)-[:GUARANTEES]->(worker:Person {userId: $userId}) RETURN guarantor")
    Optional<PersonNode> findGuarantorForWorker(UUID userId);

}
