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

@Entity
@Table(name = "sequence_definitions")
@Cacheable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private SequenceType type;
    private int seed;
    private int increment;
    private String owner;
    @Column(name = "reset_mode")
    private ResetMode resetMode;
}
