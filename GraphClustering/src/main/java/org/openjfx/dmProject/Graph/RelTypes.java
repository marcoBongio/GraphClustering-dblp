package org.openjfx.dmProject.Graph;

import org.neo4j.graphdb.RelationshipType;

public enum RelTypes implements RelationshipType {
    COAUTHOR_OF,        // P -> P
    PRESENTED_BY,       // P -> V

}
