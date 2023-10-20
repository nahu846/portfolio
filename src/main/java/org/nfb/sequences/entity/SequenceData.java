/*
 * Copyright (c)  2023
 * Nahuel Barraza
 */

package org.nfb.sequences.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "sequences_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_id")
    private long sequenceId;
    private long value;
    private String owner;
    @Column(name = "last_update")
    private Date lastUpdate;
}
