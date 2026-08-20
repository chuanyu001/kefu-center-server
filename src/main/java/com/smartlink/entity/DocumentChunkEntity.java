package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 知识库文档切片：documents.content 被切成可检索的纯文本块，供 AI 助手检索(RAG)。
 */
@Data
@TableName("document_chunks")
public class DocumentChunkEntity {

    private Long id;
    private String docId;
    private String docTitle;
    private String platform;
    /** 所属小节标题（如 xlsx 的 sheet 名、markdown 的文件名） */
    private String section;
    private Integer chunkIndex;
    private String chunkText;
}
