package com.smartlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlink.entity.FeedbackEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 反馈 Mapper
 *
 * @author smartlink
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<FeedbackEntity> {
}
