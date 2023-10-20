/*
 * Copyright (c)  2023
 * Nahuel Barraza
 */

package org.nfb.sequences;

import lombok.extern.slf4j.Slf4j;
import org.nfb.sequences.service.SequenceLockedException;
import org.nfb.sequences.service.Sequencer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@SpringBootApplication
@Slf4j
public class App2 {

    public static void main(String[] args) {
        System.setProperty("server.port", "9000");
        SpringApplication.run(App2.class, args);
    }

    @Autowired
    private Sequencer sequencer;

    @Scheduled(fixedDelay = 300000)
    public void x() {
        try {
            log.info("sequence ({}) -> {}", 1, sequencer.getNext(1, "jb"));
        } catch (SequenceLockedException e) {
            log.debug(e.getMessage());
        }
    }

}
