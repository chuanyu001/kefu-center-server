package com.smartlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlink.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper
 *
 * @author smartlink
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {
}
