package com.psychotests.psycho_tests_server.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class CPSK_Logic {
    
    @Value("${game.images.base-path:/CPSKFront/image}")
    private String basePath;

    private int count = 0;

    public synchronized String getNextImagePath() {
        count++;
        // Ограничим максимум, чтобы не уйти в несуществующие файлы
        int imageNumber = Math.min(count, 100); 
        return basePath + "/" + imageNumber + ".jpg";
    }


}

