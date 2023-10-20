/*
 * Copyright (c)  2023
 * Nahuel Barraza
 */

package org.nfb.sequences.repository;

import org.nfb.sequences.entity.SequenceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SequenceDefinitionsRepository extends JpaRepository<SequenceDefinition, Long> {
}
