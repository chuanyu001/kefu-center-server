package com.smartlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartlink.entity.SessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SessionMapper extends BaseMapper<SessionEntity> {

    /** 按受理人聚合工单数 */
    @Select("<script>SELECT agent_name AS name, COUNT(*) AS cnt FROM sessions WHERE agent_name IS NOT NULL AND agent_name != ''" +
            "<if test='from != null'> AND LEFT(session_time,10) &gt;= #{from}</if>" +
            " GROUP BY agent_name ORDER BY cnt DESC LIMIT 10</script>")
    List<Map<String, Object>> countByAgent(@Param("from") String from);

    /** 按七鱼工单状态聚合 */
    @Select("<script>SELECT qiyu_ticket_status AS status, COUNT(*) AS cnt FROM sessions WHERE qiyu_ticket_status IS NOT NULL" +
            "<if test='from != null'> AND LEFT(session_time,10) &gt;= #{from}</if>" +
            " GROUP BY qiyu_ticket_status ORDER BY cnt DESC</script>")
    List<Map<String, Object>> countByQiyuStatus(@Param("from") String from);

    /** 按问题类型聚合 */
    @Select("<script>SELECT problem_type AS name, COUNT(*) AS cnt FROM sessions WHERE problem_type IS NOT NULL AND problem_type != ''" +
            "<if test='from != null'> AND LEFT(session_time,10) &gt;= #{from}</if>" +
            " GROUP BY problem_type ORDER BY cnt DESC LIMIT 10</script>")
    List<Map<String, Object>> countByProblemType(@Param("from") String from);

    /** 按七鱼工单分类聚合 */
    @Select("<script>SELECT qiyu_ticket_category AS name, COUNT(*) AS cnt FROM sessions WHERE qiyu_ticket_category IS NOT NULL AND qiyu_ticket_category != ''" +
            "<if test='from != null'> AND LEFT(session_time,10) &gt;= #{from}</if>" +
            " GROUP BY qiyu_ticket_category ORDER BY cnt DESC LIMIT 10</script>")
    List<Map<String, Object>> countByCategory(@Param("from") String from);

    /** 按月份聚合（session_time 为日期字符串，取前7位） */
    @Select("<script>SELECT LEFT(session_time,7) AS month, COUNT(*) AS cnt FROM sessions WHERE session_time IS NOT NULL AND session_time != ''" +
            "<if test='from != null'> AND LEFT(session_time,10) &gt;= #{from}</if>" +
            " GROUP BY LEFT(session_time,7) ORDER BY month</script>")
    List<Map<String, Object>> countByMonth(@Param("from") String from);

    /** 按车型聚合 */
    @Select("<script>SELECT car_model AS name, COUNT(*) AS cnt FROM sessions WHERE car_model IS NOT NULL AND car_model != ''" +
            "<if test='from != null'> AND LEFT(session_time,10) &gt;= #{from}</if>" +
            " GROUP BY car_model ORDER BY cnt DESC LIMIT 10</script>")
    List<Map<String, Object>> countByCarModel(@Param("from") String from);

    /** 按燃料类型聚合 */
    @Select("<script>SELECT fuel_type AS name, COUNT(*) AS cnt FROM sessions WHERE fuel_type IS NOT NULL AND fuel_type != ''" +
            "<if test='from != null'> AND LEFT(session_time,10) &gt;= #{from}</if>" +
            " GROUP BY fuel_type ORDER BY cnt DESC LIMIT 10</script>")
    List<Map<String, Object>> countByFuelType(@Param("from") String from);

    /** 总数 */
    @Select("<script>SELECT COUNT(*) AS cnt FROM sessions" +
            "<if test='from != null'> WHERE LEFT(session_time,10) &gt;= #{from}</if></script>")
    long countTotal(@Param("from") String from);
}
