package com.smartlink.service;

import com.smartlink.common.PageResult;
import com.smartlink.dto.response.FeedbackVO;
import com.smartlink.dto.response.ReportVO;

import java.util.Map;

/**
 * 报告服务接口
 *
 * @author smartlink
 */
public interface ReportService {

    /**
     * 分页查询报告列表
     */
    PageResult<ReportVO> list(Integer page, Integer pageSize, String reportType, String pushStatus);

    /**
     * 查询报告详情
     */
    ReportVO detail(String id);

    /**
     * 推送报告
     */
    void push(String id);

    /**
     * 分页查询反馈列表
     */
    PageResult<FeedbackVO> feedbackList(Integer page, Integer pageSize, String status);

    /**
     * 更新反馈
     */
    void updateFeedback(String id, Map<String, Object> data);
}
