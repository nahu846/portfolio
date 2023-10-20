/*
 * Copyright (c)  2023
 * Nahuel Barraza
 */

package org.nfb.sequences.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.nfb.sequences.entity.ResetMode;
import org.nfb.sequences.entity.SequenceData;
import org.nfb.sequences.entity.SequenceDefinition;
import org.nfb.sequences.entity.SequenceType;
import org.nfb.sequences.repository.SequenceDefinitionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssumptions.given;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SequencerTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private SequenceDefinitionsRepository sequenceDefinitionsRepository;

    @InjectMocks
    private SequencerImpl sequencer;

    @Mock
    private TypedQuery<SequenceData> query = mock(TypedQuery.class);

    @BeforeEach
    void beforeEach() {
        reset(entityManager, sequenceDefinitionsRepository, query);
    }

    @Test
    void given_a_sequence_with_no_value_and_seed_10_and_increment_5_when_getNext_then_return_15_and_sequence_is_persisted()
            throws SequenceLockedException {
        // given
        long sequenceId = 1L;
        SequenceDefinition sequenceDefinition = createDefinition(sequenceId);
        sequenceDefinition.setSeed(10);
        sequenceDefinition.setIncrement(5);
        mocksSetup(sequenceDefinition, NoResultException.class);

        // when
        long sequenceValue = sequencer.getNext(sequenceId, "owner");

        // then
        assertThat(sequenceValue).isEqualTo(15L);
        verify(entityManager).persist(any(SequenceData.class));
    }

    @Test
    void given_a_sequence_updated_the_previous_day_when_getNext_then_sequence_is_reset()
            throws SequenceLockedException {
        // given
        long sequenceId = 3L;
        SequenceDefinition sequenceDefinition = createDefinition(sequenceId);
        sequenceDefinition.setResetMode(ResetMode.DAILY);

        mocksSetup(sequenceDefinition, 300);

        // when
        long sequenceValue = sequencer.getNext(sequenceId, "dummy-owner");

        // then
        assertThat(sequenceValue).isEqualTo(1L);
    }

    @Test
    void given_a_sequence_of_type_INDIVIDUAL_when_getNext_then_sequence_is_different_for_each_owner()
            throws SequenceLockedException {
        // given
        long sequenceId = 4L;
        SequenceDefinition sequenceDefinition = createDefinition(sequenceId);
        sequenceDefinition.setType(SequenceType.INDIVIDUAL);

        mocksSetup(sequenceDefinition);
        SequenceData sequenceData = SequenceData.builder()
                .sequenceId(sequenceDefinition.getId())
                .value(12L)
                .lastUpdate(getYesterday())
                .build();
        when(query.getSingleResult())
                .thenReturn(SequenceData.builder()
                        .sequenceId(sequenceDefinition.getId())
                        .value(12L)
                        .lastUpdate(getYesterday())
                        .build())
                .thenReturn(SequenceData.builder()
                        .sequenceId(sequenceDefinition.getId())
                        .value(5L)
                        .lastUpdate(getYesterday())
                        .build());

        // when
        // then
        assertThat(sequencer.getNext(sequenceId, "owner-1")).isEqualTo(13L);
        assertThat(sequencer.getNext(sequenceId, "owner-2")).isEqualTo(6L);
    }

    private SequenceDefinition createDefinition(long sequenceId) {
        return SequenceDefinition.builder()
                .id(sequenceId)
                .name("any-sequence")
                .type(SequenceType.GLOBAL)
                .seed(0)
                .increment(1)
                .resetMode(ResetMode.NEVER)
                .build();
    }

    private void mocksSetup(SequenceDefinition sequenceDefinition, Class<? extends Throwable> exeptionClass) {
        mocksSetup(sequenceDefinition);
        when(query.getSingleResult()).thenThrow(exeptionClass);
    }

    private void mocksSetup(SequenceDefinition sequenceDefinition, long sequenceValue) {
        mocksSetup(sequenceDefinition);
        SequenceData sequenceData = SequenceData.builder()
                .sequenceId(sequenceDefinition.getId())
                .value(sequenceValue)
                .lastUpdate(getYesterday())
                .build();
        when(query.getSingleResult()).thenReturn(sequenceData);
    }

    private void mocksSetup(SequenceDefinition sequenceDefinition) {
        when(sequenceDefinitionsRepository.findById(sequenceDefinition.getId()))
                .thenReturn(Optional.of(sequenceDefinition));
        when(entityManager.createQuery(anyString(), any(Class.class))).thenReturn(query);
        when(query.setParameter(anyString(), anyLong())).thenReturn(query);
        when(query.setParameter(anyString(), anyString())).thenReturn(query);
        when(query.setLockMode(LockModeType.PESSIMISTIC_WRITE)).thenReturn(query);
        when(query.setHint("jakarta.persistence.lock.timeout", LockOptions.NO_WAIT)).thenReturn(query);
    }

    private Date getYesterday() {
        return Date.from(
                LocalDate.now()
                        .minusDays(1)
                        .atStartOfDay()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );
    }

    @Test
    void given_a_sequence_with_value_100_and_increment_1_when_getNext_then_return_101_and_sequence_is_persisted()
            throws SequenceLockedException {
        // given
        long sequenceId = 2L;
        SequenceDefinition sequenceDefinition = createDefinition(sequenceId);
        mocksSetup(sequenceDefinition, 100);

        // when
        long sequenceValue = sequencer.getNext(sequenceId, "owner");

        // then
        assertThat(sequenceValue).isEqualTo(101L);
        verify(entityManager).persist(any(SequenceData.class));
    }

    @Test
    void given_a_sequence_when_getNext_then_SequenceLockedException()  {
        // given
        long sequenceId = 1L;
        SequenceDefinition sequenceDefinition = createDefinition(sequenceId);
        sequenceDefinition.setSeed(10);
        sequenceDefinition.setIncrement(5);
        mocksSetup(sequenceDefinition, PessimisticLockException.class);

        // when
        // then
        assertThatThrownBy(() -> sequencer.getNext(sequenceId, "owner"))
                .isInstanceOf(SequenceLockedException.class);
    }

}