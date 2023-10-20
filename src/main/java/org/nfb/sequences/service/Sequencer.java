/*
 * Copyright (c)  2023
 * Nahuel Barraza
 */

package org.nfb.sequences.service;

public interface Sequencer {
    long getNext(long sequenceId, String owner) throws SequenceLockedException;
}
