/*
 * Copyright (c)  2023
 * Nahuel Barraza
 */

package org.nfb.sequences.service;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.nfb.sequences.entity.ResetMode;
import org.nfb.sequences.entity.SequenceData;
import org.nfb.sequences.entity.SequenceDefinition;
import org.nfb.sequences.entity.SequenceType;
import org.nfb.sequences.repository.SequenceDefinitionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Service
@Slf4j
public class SequencerImpl implements Sequencer {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SequenceDefinitionsRepository sequenceDefinitionsRepository;

    @Override
    @Transactional
    public long getNext(long sequenceId, String owner) throws SequenceLockedException {
        SequenceDefinition sequenceDefinition = sequenceDefinitionsRepository.findById(sequenceId).orElseThrow();
        SequenceData sequenceData;
        try {
            sequenceData = createQuery(sequenceDefinition.getType(), sequenceId, owner)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setHint("jakarta.persistence.lock.timeout", LockOptions.NO_WAIT)
                    .getSingleResult();
        } catch (NoResultException e) {
            sequenceData = SequenceData.builder()
                    .sequenceId(sequenceId)
                    .value(sequenceDefinition.getSeed())
                    .lastUpdate(new Date())
                    .build();
        } catch (PessimisticLockException | LockTimeoutException e) {
            throw new SequenceLockedException("Unable to lock the sequence record - seq: " + sequenceId, e);
        }

        if (sequenceDefinition.getResetMode() == ResetMode.DAILY && sequenceData.getLastUpdate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now())) {
            sequenceData.setValue(sequenceDefinition.getSeed());
        }

        long value = sequenceData.getValue() + sequenceDefinition.getIncrement();
        sequenceData.setValue(value);
        sequenceData.setLastUpdate(new Date());
        sequenceData.setOwner(sequenceDefinition.getType() == SequenceType.GLOBAL ? null : owner);
        entityManager.persist(sequenceData);
        entityManager.flush();
        return value;
    }

    private TypedQuery<SequenceData> createQuery(SequenceType sequenceType, long sequenceId, String owner) {
        String query =
                "select sd " +
                "from SequenceData sd " +
                "where sd.sequenceId = :sequenceId";
        TypedQuery<SequenceData> typedQuery;

        if (sequenceType == SequenceType.GLOBAL) {
            typedQuery = entityManager
                    .createQuery(query, SequenceData.class)
                    .setParameter("sequenceId", sequenceId);
        } else {
            typedQuery = entityManager
                    .createQuery(query + " and sd.owner = :owner", SequenceData.class)
                    .setParameter("sequenceId", sequenceId)
                    .setParameter("owner", owner);
        }
        return typedQuery;
    }

}
