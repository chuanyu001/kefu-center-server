package com.smartlink.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 问答对创建请求
 *
 * @author smartlink
 */
@Data
public class QaCreateReq {

    /** 问题 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 答案 */
    @NotBlank(message = "答案不能为空")
    private String answer;

    /** 答案摘要 */
    private String answerBrief;

    /** 分类 */
    private String category;

    /** 标签列表 */
    private List<String> tags;

    /** 关键词列表 */
    private List<String> keywords;
}
