package com.pettimeline.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PageVO<T> {
    private long total;
    private int page;
    private int size;
    private List<T> records;
}
