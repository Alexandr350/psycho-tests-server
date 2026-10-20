package com.psychotests.psycho_tests_server.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ResearchStatus {
    DRAFT, NEW, RUNNING, PAUSE, STOPPED, ARCHIVE;

    private static final Map<ResearchStatus, Set<ResearchStatus>> ALLOWED = Map.of(
            DRAFT,   EnumSet.of(NEW),
            NEW,     EnumSet.of(RUNNING),
            RUNNING, EnumSet.of(PAUSE, STOPPED),
            PAUSE,   EnumSet.of(RUNNING, STOPPED),
            STOPPED, EnumSet.of(ARCHIVE),
            ARCHIVE, EnumSet.noneOf(ResearchStatus.class)
    );

    public boolean canTransitionTo(ResearchStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public  boolean canBeDeleted(){
        return this == DRAFT || this == NEW || this == STOPPED || this == ARCHIVE;
    }
}

