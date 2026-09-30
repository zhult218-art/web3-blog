package com.web3.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3.media.entity.NailBooking;
import org.apache.ibatis.annotations.Mapper;

/**
 * NailBookingMapper —— 美甲预约表数据访问接口
 * <p>
 * 所属模块：media-service（媒体模块）。
 * <p>
 * 职责：操作 nail_booking 美甲预约表，继承 BaseMapper 获得通用 CRUD。
 */
@Mapper
public interface NailBookingMapper extends BaseMapper<NailBooking> {
}
