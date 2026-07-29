package com.smartlink.service;

import com.smartlink.common.PageResult;
import com.smartlink.dto.request.QaCreateReq;
import com.smartlink.dto.request.QaQueryReq;
import com.smartlink.dto.response.*;

import java.util.Map;

/**
 * 知识库服务接口
 *
 * @author smartlink
 */
public interface KnowledgeService {

    // ===== 文档管理 =====

    /** 分页查询文档列表 */
    PageResult<DocumentVO> documentList(Integer page, Integer pageSize, String category, String parseStatus);

    /** 上传/创建文档 */
    void documentCreate(Map<String, Object> data);

    // ===== 问答对管理 =====

    /** 分页查询问答列表 */
    PageResult<QaPairVO> qaList(QaQueryReq req);

    /** 查询问答详情 */
    QaPairVO qaDetail(String id);

    /** 创建问答 */
    void qaCreate(QaCreateReq req);

    /** 更新问答 */
    void qaUpdate(String id, Map<String, Object> data);

    /** 删除问答 */
    void qaDelete(String id);

    // ===== 审核管理 =====

    /** 分页查询审核列表 */
    PageResult<ReviewVO> reviewList(Integer page, Integer pageSize, String type, String status);

    /** 更新审核 */
    void reviewUpdate(String id, Map<String, Object> data);

    // ===== 迭代记录管理 =====

    /** 分页查询迭代列表 */
    PageResult<IterationVO> iterationList(Integer page, Integer pageSize, String status);

    /** 更新迭代记录 */
    void iterationUpdate(String id, Map<String, Object> data);

    // ===== 分析概览 =====

    /** 获取分析概览数据 */
    Map<String, Object> analytics();
}
