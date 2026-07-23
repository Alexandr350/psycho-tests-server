package com.psychotests.psycho_tests_server.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Data;


@Max(8)
@Min(0)
@Data
public class CPSKClickRequest {
    private int numberSector;

}
