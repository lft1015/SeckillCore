package com.reditickets.activity.dto;

import lombok.Data;

/**
 * 活动列表查询请求数据传输对象（DTO）
 * <p>
 * 封装活动分页查询的请求参数，支持按活动状态筛选
 * </p>
 *
 * @author gugu
 */
@Data
public class ActivityListDTO {

    /** 当前页码，默认第1页 */
    private Integer page = 1;

    /** 每页显示条数，默认10条 */
    private Integer size = 10;

    /** 活动状态：0=未开始，1=进行中，2=已结束 */
    private Integer status;
}