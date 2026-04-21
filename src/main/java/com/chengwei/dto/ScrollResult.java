package com.chengwei.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "滚动分页结果")
public class ScrollResult {
    @Schema(description = "当前页数据列表")
    private List<?> list;

    @Schema(description = "当前批次最小时间戳，用于下一次滚动查询", example = "1713500000000")
    private Long minTime;

    @Schema(description = "偏移量", example = "0")
    private Integer offset;
}
