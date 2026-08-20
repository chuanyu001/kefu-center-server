package com.smartlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlink.entity.DocumentChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunkEntity> {

    /** ngram 中文全文检索，按相关度排序（要求 document_chunks.chunk_text 已建 FULLTEXT ... WITH PARSER ngram） */
    @Select("SELECT id, doc_id, doc_title, platform, section, chunk_index, chunk_text " +
            "FROM document_chunks WHERE MATCH(chunk_text) AGAINST(#{q} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY MATCH(chunk_text) AGAINST(#{q} IN NATURAL LANGUAGE MODE) DESC LIMIT #{limit}")
    List<DocumentChunkEntity> fulltextSearch(@Param("q") String q, @Param("limit") int limit);

    /** 关键词 LIKE 兜底检索（全文索引未建/命中不足时用） */
    @Select("<script>" +
            "SELECT id, doc_id, doc_title, platform, section, chunk_index, chunk_text FROM document_chunks WHERE " +
            "<foreach collection='kws' item='kw' separator=' OR '>chunk_text LIKE CONCAT('%', #{kw}, '%')</foreach>" +
            " LIMIT #{limit}" +
            "</script>")
    List<DocumentChunkEntity> likeSearch(@Param("kws") List<String> kws, @Param("limit") int limit);
}
