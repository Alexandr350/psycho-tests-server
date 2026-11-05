package com.psychotests.psycho_tests_server.unit.service;

import com.psychotests.psycho_tests_server.projection.SectorCountProjection;

// Вспомогательная реализация проекции для тестов
public record TestSectorCount(int numberSector, long count) implements SectorCountProjection {

    @Override
    public int getNumberSector() {
        return numberSector;
    }

    @Override
    public long getCount() {
        return count;
    }
}
