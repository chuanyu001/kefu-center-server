package com.smartlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlink.entity.ReviewEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审核 Mapper
 *
 * @author smartlink
 */
@Mapper
public interface ReviewMapper extends BaseMapper<ReviewEntity> {
}
