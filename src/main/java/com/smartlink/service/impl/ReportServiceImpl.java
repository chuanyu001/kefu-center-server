package com.smartlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlink.common.PageResult;
import com.smartlink.dto.response.FeedbackVO;
import com.smartlink.dto.response.ReportVO;
import com.smartlink.entity.FeedbackEntity;
import com.smartlink.entity.ReportEntity;
import com.smartlink.mapper.FeedbackMapper;
import com.smartlink.mapper.ReportMapper;
import com.smartlink.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报告服务实现
 *
 * @author smartlink
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final FeedbackMapper feedbackMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<ReportVO> list(Integer pageNum, Integer pageSize, String reportType, String pushStatus) {
        try {
            LambdaQueryWrapper<ReportEntity> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(reportType)) {
                wrapper.eq(ReportEntity::getReportType, reportType);
            }
            if (StringUtils.hasText(pushStatus)) {
                wrapper.eq(ReportEntity::getPushStatus, pushStatus);
            }
            wrapper.orderByDesc(ReportEntity::getGeneratedTime);

            Page<ReportEntity> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
            IPage<ReportEntity> result = reportMapper.selectPage(page, wrapper);

            List<ReportVO> records = result.getRecords().stream()
                    .map(this::toVO)
                    .collect(Collectors.toList());

            return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
        } catch (Exception e) {
            log.warn("数据库查询报告失败，返回Mock数据: {}", e.getMessage());
            return getMockReportList(pageNum, pageSize, reportType, pushStatus);
        }
    }

    @Override
    public ReportVO detail(String id) {
        try {
            ReportEntity entity = reportMapper.selectById(id);
            if (entity != null) {
                return toVO(entity);
            }
        } catch (Exception e) {
            log.warn("数据库查询报告详情失败，返回Mock数据: {}", e.getMessage());
        }
        return getMockReportDetail(id);
    }

    @Override
    public void push(String id) {
        try {
            ReportEntity entity = reportMapper.selectById(id);
            if (entity == null) {
                throw new RuntimeException("报告不存在");
            }
            entity.setPushStatus("PUSHED");
            reportMapper.updateById(entity);
            log.info("报告 {} 推送成功", id);
        } catch (Exception e) {
            log.error("推送报告失败: {}", e.getMessage());
            throw new RuntimeException("推送失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<FeedbackVO> feedbackList(Integer pageNum, Integer pageSize, String status) {
        try {
            LambdaQueryWrapper<FeedbackEntity> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(status)) {
                wrapper.eq(FeedbackEntity::getStatus, status);
            }
            wrapper.orderByDesc(FeedbackEntity::getFeedbackTime);

            Page<FeedbackEntity> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
            IPage<FeedbackEntity> result = feedbackMapper.selectPage(page, wrapper);

            List<FeedbackVO> records = result.getRecords().stream()
                    .map(this::feedbackToVO)
                    .collect(Collectors.toList());

            return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
        } catch (Exception e) {
            log.warn("数据库查询反馈失败，返回Mock数据: {}", e.getMessage());
            return getMockFeedbackList(pageNum, pageSize, status);
        }
    }

    @Override
    public void updateFeedback(String id, Map<String, Object> data) {
        try {
            FeedbackEntity entity = feedbackMapper.selectById(id);
            if (entity == null) {
                throw new RuntimeException("反馈不存在");
            }
            if (data.containsKey("status")) {
                entity.setStatus((String) data.get("status"));
            }
            if (data.containsKey("remark")) {
                entity.setRemark((String) data.get("remark"));
            }
            feedbackMapper.updateById(entity);
            log.info("反馈 {} 更新成功", id);
        } catch (Exception e) {
            log.error("更新反馈失败: {}", e.getMessage());
            throw new RuntimeException("更新失败: " + e.getMessage());
        }
    }

    private ReportVO toVO(ReportEntity entity) {
        ReportVO vo = new ReportVO();
        vo.setId(entity.getId());
        vo.setCustomerName(entity.getCustomerName());
        vo.setReportType(entity.getReportType());
        if (entity.getDateStart() != null) {
            vo.setDateStart(entity.getDateStart().toString());
        }
        if (entity.getDateEnd() != null) {
            vo.setDateEnd(entity.getDateEnd().toString());
        }
        if (entity.getGeneratedTime() != null) {
            vo.setGeneratedTime(entity.getGeneratedTime().format(DTF));
        }
        vo.setPushStatus(entity.getPushStatus());
        vo.setSummaryText(entity.getSummaryText());
        vo.setAnomalyNotes(entity.getAnomalyNotes());
        vo.setChartData(parseJsonToMap(entity.getChartData()));
        vo.setTableData(parseJsonToObject(entity.getTableData()));
        vo.setPushScript(entity.getPushScript());
        return vo;
    }

    private FeedbackVO feedbackToVO(FeedbackEntity entity) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(entity.getId());
        vo.setCustomerName(entity.getCustomerName());
        vo.setReportId(entity.getReportId());
        vo.setContent(entity.getContent());
        if (entity.getFeedbackTime() != null) {
            vo.setFeedbackTime(entity.getFeedbackTime().format(DTF));
        }
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private Map<String, Object> parseJsonToMap(String json) {
        try {
            if (StringUtils.hasText(json)) {
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    private Object parseJsonToObject(String json) {
        try {
            if (StringUtils.hasText(json)) {
                return objectMapper.readTree(json);
            }
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
        }
        return null;
    }

    // ==================== Mock数据 ====================

    private PageResult<ReportVO> getMockReportList(Integer pageNum, Integer pageSize, String reportType, String pushStatus) {
        List<ReportVO> all = buildMockReports();
        List<ReportVO> filtered = all.stream()
                .filter(r -> {
                    if (StringUtils.hasText(reportType) && !reportType.equals(r.getReportType())) return false;
                    if (StringUtils.hasText(pushStatus) && !pushStatus.equals(r.getPushStatus())) return false;
                    return true;
                })
                .collect(Collectors.toList());

        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 10;
        long total = filtered.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, (int) total);
        if (from >= total) return PageResult.of(Collections.emptyList(), total, page, size);
        return PageResult.of(filtered.subList(from, to), total, page, size);
    }

    private ReportVO getMockReportDetail(String id) {
        return buildMockReports().stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    private List<ReportVO> buildMockReports() {
        List<ReportVO> list = new ArrayList<>();

        ReportVO r1 = new ReportVO();
        r1.setId("RP20260701001");
        r1.setCustomerName("智能分析系统");
        r1.setReportType("日报");
        r1.setDateStart("2026-07-01");
        r1.setDateEnd("2026-07-01");
        r1.setGeneratedTime("2026-07-02 08:00:00");
        r1.setPushStatus("PUSHED");
        r1.setSummaryText("今日共处理工单15个，完成13个，完成率86.7%。主要问题集中在DPF故障(4个)、T-Box通讯(3个)、保养咨询(5个)、其他(3个)。客户满意度4.6/5.0。");
        r1.setAnomalyNotes("T-Box掉线数量较昨日增加40%，建议关注固件版本V4.2.1的设备。DPF报警集中在里程15万公里以上的车辆。");
        r1.setChartData(buildDailyChartData());
        r1.setTableData(buildDailyTableData());
        r1.setPushScript("尊敬的领导：今日客服中心运营数据已生成，请查阅。今日完成率86.7%，客户满意度4.6分，重点关注T-Box掉线问题。");
        list.add(r1);

        ReportVO r2 = new ReportVO();
        r2.setId("RP20260702002");
        r2.setCustomerName("智能分析系统");
        r2.setReportType("周报");
        r2.setDateStart("2026-06-26");
        r2.setDateEnd("2026-07-02");
        r2.setGeneratedTime("2026-07-03 08:00:00");
        r2.setPushStatus("NOT_PUSHED");
        r2.setSummaryText("本周共处理工单98个，完成89个，完成率90.8%。TOP3问题：保养咨询(28个)、DPF故障(22个)、T-Box通讯(18个)。本周完成车队批量接入2家，共52台车。");
        r2.setAnomalyNotes("ECU通讯丢失类故障本周出现3次，较上周增加200%，建议技术团队排查该批次车辆ECU版本。");
        r2.setChartData(buildWeeklyChartData());
        r2.setTableData(buildWeeklyTableData());
        r2.setPushScript("尊敬的领导：本周客服中心运营周报已生成。本周完成率90.8%，TOP关注项为ECU通讯故障增长200%，建议技术部门介入排查。");
        list.add(r2);

        ReportVO r3 = new ReportVO();
        r3.setId("RP20260703003");
        r3.setCustomerName("沈阳恒通物流");
        r3.setReportType("月度报告");
        r3.setDateStart("2026-06-01");
        r3.setDateEnd("2026-06-30");
        r3.setGeneratedTime("2026-07-01 08:30:00");
        r3.setPushStatus("PUSHED");
        r3.setSummaryText("贵车队6月份共产生工单23个，已解决22个。车队35台车月总里程286,500公里，平均油耗32.8L/100km。");
        r3.setAnomalyNotes("发现3台车存在怠速时间过长问题，日均怠速超过3小时，建议加强驾驶员培训，预计可节油8%-12%。");
        r3.setChartData(buildFleetChartData());
        r3.setTableData(buildFleetTableData());
        r3.setPushScript("孙队长您好，贵车队6月运营月报已生成。建议关注3台怠速过长车辆，优化后可节省燃油成本约5000元/月。");
        list.add(r3);

        ReportVO r4 = new ReportVO();
        r4.setId("RP20260704004");
        r4.setCustomerName("智能分析系统");
        r4.setReportType("日报");
        r4.setDateStart("2026-07-04");
        r4.setDateEnd("2026-07-04");
        r4.setGeneratedTime("2026-07-05 08:00:00");
        r4.setPushStatus("PUSHED");
        r4.setSummaryText("今日共处理工单12个，完成10个，完成率83.3%。主要问题：故障报修(4个)、保养咨询(3个)、系统使用指导(3个)、配件查询(2个)。");
        r4.setAnomalyNotes("凌晨时段接到紧急救援1起，已妥善处理。建议优化紧急救援响应流程。");
        r4.setChartData(buildDailyChartData());
        r4.setTableData(buildDailyTableData());
        r4.setPushScript("尊敬的领导：今日客服中心运营数据已生成。凌晨紧急救援1起已妥善处理。");
        list.add(r4);

        ReportVO r5 = new ReportVO();
        r5.setId("RP20260705005");
        r5.setCustomerName("长春宏达运输公司");
        r5.setReportType("周报");
        r5.setDateStart("2026-06-29");
        r5.setDateEnd("2026-07-05");
        r5.setGeneratedTime("2026-07-06 08:00:00");
        r5.setPushStatus("NOT_PUSHED");
        r5.setSummaryText("贵公司本周共产生工单8个，已全部解决。20台车队本周总里程42,300公里，DPF报警2次均已处理。车队平均油耗较上周下降1.5%。");
        r5.setAnomalyNotes("无异常。");
        r5.setChartData(buildWeeklyChartData());
        r5.setTableData(buildFleetTableData());
        r5.setPushScript("张经理您好，贵公司本周车队运营周报已生成，所有工单已解决，油耗下降1.5%，表现良好。");
        list.add(r5);

        return list;
    }

    private PageResult<FeedbackVO> getMockFeedbackList(Integer pageNum, Integer pageSize, String status) {
        List<FeedbackVO> all = buildMockFeedbacks();
        List<FeedbackVO> filtered = all.stream()
                .filter(f -> !StringUtils.hasText(status) || status.equals(f.getStatus()))
                .collect(Collectors.toList());
        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 10;
        long total = filtered.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, (int) total);
        if (from >= total) return PageResult.of(Collections.emptyList(), total, page, size);
        return PageResult.of(filtered.subList(from, to), total, page, size);
    }

    private List<FeedbackVO> buildMockFeedbacks() {
        List<FeedbackVO> list = new ArrayList<>();
        FeedbackVO f1 = new FeedbackVO(); f1.setId("FB20260701001"); f1.setCustomerName("张建国"); f1.setReportId("RP20260701001"); f1.setContent("客服李娜服务态度很好，问题解决也很快。DPF再生后车况恢复正常了，非常感谢！"); f1.setFeedbackTime("2026-07-01 14:00:00"); f1.setStatus("RESOLVED"); f1.setRemark("客户满意度高"); list.add(f1);
        FeedbackVO f2 = new FeedbackVO(); f2.setId("FB20260701002"); f2.setCustomerName("王德发"); f2.setReportId("RP20260701001"); f2.setContent("咨询保养信息很详细，已经预约了下周的保养服务。就是希望能提供在线预约功能。"); f2.setFeedbackTime("2026-07-02 16:30:00"); f2.setStatus("PENDING"); f2.setRemark("建议纳入产品优化需求池"); list.add(f2);
        FeedbackVO f3 = new FeedbackVO(); f3.setId("FB20260703003"); f3.setCustomerName("李国强"); f3.setReportId("RP20260701001"); f3.setContent("T-Box远程升级方案很好用，不用去服务站就解决了问题。不过升级过程能不能在APP上显示进度？"); f3.setFeedbackTime("2026-07-03 10:00:00"); f3.setStatus("PENDING"); f3.setRemark("T-Box升级进度可视化需求"); list.add(f3);
        FeedbackVO f4 = new FeedbackVO(); f4.setId("FB20260704004"); f4.setCustomerName("赵大勇"); f4.setReportId("RP20260704004"); f4.setContent("最终是电瓶负极松动导致ECU断电，师傅远程指导检查出来的，省了一笔拖车费！"); f4.setFeedbackTime("2026-07-04 18:00:00"); f4.setStatus("RESOLVED"); f4.setRemark("远程诊断案例"); list.add(f4);
        FeedbackVO f5 = new FeedbackVO(); f5.setId("FB20260705005"); f5.setCustomerName("孙明辉"); f5.setReportId("RP20260703003"); f5.setContent("月度报告很详细，怠速分析这块特别有价值。我们已经开始整治驾驶员怠速习惯了。"); f5.setFeedbackTime("2026-07-05 09:00:00"); f5.setStatus("RESOLVED"); f5.setRemark("车队管理价值体现"); list.add(f5);
        FeedbackVO f6 = new FeedbackVO(); f6.setId("FB20260705006"); f6.setCustomerName("周建华"); f6.setReportId("RP20260701001"); f6.setContent("问题解决了，但是每次都要清缓存比较麻烦，建议修复一下这个bug。"); f6.setFeedbackTime("2026-07-06 14:00:00"); f6.setStatus("PENDING"); f6.setRemark("前端缓存问题"); list.add(f6);
        FeedbackVO f7 = new FeedbackVO(); f7.setId("FB20260707007"); f7.setCustomerName("吴志强"); f7.setReportId("RP20260704004"); f7.setContent("半夜三点还有客服值班真的非常感动，救援也来得很快。一汽解放的服务越来越好了！"); f7.setFeedbackTime("2026-07-07 08:00:00"); f7.setStatus("RESOLVED"); f7.setRemark("紧急救援正面评价"); list.add(f7);
        FeedbackVO f8 = new FeedbackVO(); f8.setId("FB20260708008"); f8.setCustomerName("郑伟明"); f8.setReportId("RP20260705005"); f8.setContent("配件查询很方便，价格透明。最终选择了清洗方案，能省不少钱，谢谢建议。"); f8.setFeedbackTime("2026-07-08 17:00:00"); f8.setStatus("PENDING"); f8.setRemark("配件查询+替代方案典型案例"); list.add(f8);
        return list;
    }

    private Map<String, Object> buildDailyChartData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dailyTrend", Arrays.asList(
                mapOf("hour","08","count",2), mapOf("hour","09","count",3),
                mapOf("hour","10","count",4), mapOf("hour","11","count",2),
                mapOf("hour","14","count",3), mapOf("hour","16","count",1)));
        data.put("categoryPie", Arrays.asList(
                mapOf("name","DPF故障","value",4), mapOf("name","T-Box通讯","value",3),
                mapOf("name","保养咨询","value",5), mapOf("name","其他","value",3)));
        return data;
    }

    private Object buildDailyTableData() {
        return Arrays.asList(
                mapOf("type","DPF故障","count",4,"resolved",4,"avgTime","18分钟","satisfaction",4.8),
                mapOf("type","T-Box通讯","count",3,"resolved",2,"avgTime","25分钟","satisfaction",4.3),
                mapOf("type","保养咨询","count",5,"resolved",5,"avgTime","10分钟","satisfaction",4.7),
                mapOf("type","其他","count",3,"resolved",2,"avgTime","15分钟","satisfaction",4.5));
    }

    private Map<String, Object> buildWeeklyChartData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("weeklyTrend", Arrays.asList(
                mapOf("day","周一","count",18), mapOf("day","周二","count",22),
                mapOf("day","周三","count",16), mapOf("day","周四","count",20),
                mapOf("day","周五","count",14), mapOf("day","周六","count",5),
                mapOf("day","周日","count",3)));
        data.put("resolutionRate", Arrays.asList(85,92,88,95,90,80,100));
        return data;
    }

    private Object buildWeeklyTableData() {
        return Arrays.asList(
                mapOf("weekRank",1,"type","保养咨询","count",28,"trend","up"),
                mapOf("weekRank",2,"type","DPF故障","count",22,"trend","stable"),
                mapOf("weekRank",3,"type","T-Box通讯","count",18,"trend","up"),
                mapOf("weekRank",4,"type","故障报修","count",15,"trend","stable"),
                mapOf("weekRank",5,"type","车队管理","count",10,"trend","up"),
                mapOf("weekRank",6,"type","配件查询","count",5,"trend","stable"));
    }

    private Map<String, Object> buildFleetChartData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fleetMileage", Arrays.asList(
                mapOf("plate","辽A-8K123","mileage",9200), mapOf("plate","辽A-8K125","mileage",8500),
                mapOf("plate","辽A-8K127","mileage",7800)));
        data.put("fuelTrend", Arrays.asList(
                mapOf("month","1月","avgFuel",33.2), mapOf("month","2月","avgFuel",32.8),
                mapOf("month","3月","avgFuel",33.5), mapOf("month","4月","avgFuel",32.1),
                mapOf("month","5月","avgFuel",33.0), mapOf("month","6月","avgFuel",32.8)));
        return data;
    }

    private Object buildFleetTableData() {
        return Arrays.asList(
                mapOf("plate","辽A-8K123","mileage",9200,"avgFuel",31.5,"idleTime","2.8h","alerts",2),
                mapOf("plate","辽A-8K125","mileage",8500,"avgFuel",33.2,"idleTime","3.5h","alerts",5),
                mapOf("plate","辽A-8K127","mileage",7800,"avgFuel",32.1,"idleTime","1.9h","alerts",1),
                mapOf("plate","辽A-8K129","mileage",9100,"avgFuel",34.0,"idleTime","4.1h","alerts",3),
                mapOf("plate","辽A-8K130","mileage",7600,"avgFuel",31.8,"idleTime","2.1h","alerts",0));
    }

    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Object... entries) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((K) entries[i], (V) entries[i + 1]);
        }
        return map;
    }
}
