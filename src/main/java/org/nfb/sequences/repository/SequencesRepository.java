/*
 * Copyright (c)  2023
 * Nahuel Barraza
 */

package org.nfb.sequences.repository;

import org.nfb.sequences.entity.SequenceData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SequencesRepository extends JpaRepository<SequenceData, Long> {
}
