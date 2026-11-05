package com.psychotests.psycho_tests_server.service;

import lombok.experimental.UtilityClass;


import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class Utility {


    public static List<String> splitTokens(String line){
        if(line == null || line.isBlank()){
            return List.of();
        }

        String[] tokens = line.split("(?U)[\\s,]+");
        return Arrays.stream(tokens)
                .filter(t -> !t.isEmpty())
                .toList();
    }


}
