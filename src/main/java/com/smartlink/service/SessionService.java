package com.smartlink.service;

import com.smartlink.common.PageResult;
import com.smartlink.dto.request.SessionQueryReq;
import com.smartlink.dto.request.SessionUpdateReq;
import com.smartlink.dto.response.SessionVO;

/**
 * 工作表服务接口
 *
 * @author smartlink
 */
public interface SessionService {

    /**
     * 分页查询工作表列表
     */
    PageResult<SessionVO> list(SessionQueryReq req);

    /**
     * 查询工作表详情
     */
    SessionVO detail(String id);

    /**
     * 更新工作表
     */
    void update(String id, SessionUpdateReq req);
}
