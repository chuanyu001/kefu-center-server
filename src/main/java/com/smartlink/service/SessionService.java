package com.smartlink.service;

import com.smartlink.common.PageResult;
import com.smartlink.dto.request.SessionQueryReq;
import com.smartlink.dto.request.SessionUpdateReq;
import com.smartlink.dto.response.SessionVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface SessionService {

    PageResult<SessionVO> list(SessionQueryReq req);

    SessionVO detail(String id);

    void update(String id, SessionUpdateReq req);

    Map<String, Object> importExcel(MultipartFile file);

    byte[] exportExcel(List<String> ids, List<String> columns);

    Map<String, Object> syncVehicleInfo();

    /** 工单真实数据聚合（供 AI 内容设计使用） */
    Map<String, Object> analytics(String range);
}
