package com.wastina.trust.graph;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Node("Person")
@Data
@NoArgsConstructor
public class PersonNode {

    @Id
    private UUID userId;

    @Property
    private String name; // For easy debugging/display

    @Property
    private boolean hasFaydaId;

    @Property
    private boolean isGovEmployee;

    @Property
    private int yearsOfGoodWork;

    // A person can guarantee many other people
    @Relationship(type = "GUARANTEES", direction = Relationship.Direction.OUTGOING)
    private Set<PersonNode> guaranteedPersons = new HashSet<>();

    public PersonNode(UUID userId, String name, boolean hasFaydaId, boolean isGovEmployee, int yearsOfGoodWork) {
        this.userId = userId;
        this.name = name;
        this.hasFaydaId = hasFaydaId;
        this.isGovEmployee = isGovEmployee;
        this.yearsOfGoodWork = yearsOfGoodWork;
    }

    public void guarantees(PersonNode person) {
        guaranteedPersons.add(person);
    }
}
