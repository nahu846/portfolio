/*
 * Copyright (c)  2023
 * Nahuel Barraza
 */

package org.nfb.sequences.service;

public class SequenceLockedException extends Exception {
    public SequenceLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
