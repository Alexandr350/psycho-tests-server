package com.psychotests.psycho_tests_server.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CPSK {
    private List<String> data_test = new ArrayList<>();
    private Map<Integer, Integer> result_test_CPSK = new HashMap<>();



    public void countClick(int number){
        result_test_CPSK.merge(number, 1,  Integer::sum);
        /*System.out.println(number);

        result_test_CPSK.forEach((key, value) ->
                System.out.println(key + " -> " + value));
*/

    }




}

