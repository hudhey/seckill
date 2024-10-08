package com.ygh.vo;

import com.ygh.entity.Goods;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class GoodsVO extends Goods {
    private Double seckillPrice;
    private Integer stockCount;
    private Date startDate;
    private Date endDate;
    private Integer version;

}

