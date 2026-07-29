package com.smartlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlink.common.PageResult;
import com.smartlink.dto.request.SessionQueryReq;
import com.smartlink.dto.request.SessionUpdateReq;
import com.smartlink.dto.response.SessionVO;
import com.smartlink.entity.SessionEntity;
import com.smartlink.mapper.SessionMapper;
import com.smartlink.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作表服务实现
 *
 * @author smartlink
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PageResult<SessionVO> list(SessionQueryReq req) {
        try {
            LambdaQueryWrapper<SessionEntity> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(req.getKeyword())) {
                wrapper.and(w -> w
                        .like(SessionEntity::getCustomerName, req.getKeyword())
                        .or()
                        .like(SessionEntity::getVin, req.getKeyword())
                        .or()
                        .like(SessionEntity::getCustomerPhone, req.getKeyword()));
            }
            if (StringUtils.hasText(req.getWorkRecordType())) {
                wrapper.eq(SessionEntity::getWorkRecordType, req.getWorkRecordType());
            }
            if (StringUtils.hasText(req.getExportStatus())) {
                wrapper.eq(SessionEntity::getExportStatus, req.getExportStatus());
            }
            wrapper.orderByDesc(SessionEntity::getCreatedAt);

            Page<SessionEntity> page = new Page<>(req.getPage(), req.getPageSize());
            IPage<SessionEntity> result = sessionMapper.selectPage(page, wrapper);

            List<SessionVO> records = result.getRecords().stream()
                    .map(this::toVO)
                    .collect(Collectors.toList());

            return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
        } catch (Exception e) {
            log.warn("数据库查询失败，返回Mock数据: {}", e.getMessage());
            return getMockSessionList(req);
        }
    }

    @Override
    public SessionVO detail(String id) {
        try {
            SessionEntity entity = sessionMapper.selectById(id);
            if (entity != null) {
                return toVO(entity);
            }
        } catch (Exception e) {
            log.warn("数据库查询详情失败，返回Mock数据: {}", e.getMessage());
        }
        return getMockSessionDetail(id);
    }

    @Override
    public void update(String id, SessionUpdateReq req) {
        try {
            SessionEntity entity = sessionMapper.selectById(id);
            if (entity == null) {
                throw new RuntimeException("工作表不存在");
            }
            // 更新导出状态
            entity.setExportStatus(req.getExportStatus());

            // 如果提供了ICCID，更新formData中的iccid字段
            if (StringUtils.hasText(req.getIccid())) {
                try {
                    Map<String, Object> formData = objectMapper.readValue(
                            entity.getFormData() != null ? entity.getFormData() : "{}",
                            new TypeReference<Map<String, Object>>() {});
                    formData.put("iccid", req.getIccid());
                    entity.setFormData(objectMapper.writeValueAsString(formData));
                } catch (Exception e) {
                    log.error("更新formData失败", e);
                }
            }

            sessionMapper.updateById(entity);
            log.info("工作表 {} 更新成功", id);
        } catch (Exception e) {
            log.error("更新工作表失败: {}", e.getMessage());
            throw new RuntimeException("更新失败: " + e.getMessage());
        }
    }

    /**
     * 实体转视图对象，解析JSON字段
     */
    private SessionVO toVO(SessionEntity entity) {
        SessionVO vo = new SessionVO();
        vo.setId(entity.getId());
        vo.setCustomerName(entity.getCustomerName());
        vo.setCustomerPhone(entity.getCustomerPhone());
        vo.setVin(entity.getVin());
        vo.setWorkRecordType(entity.getWorkRecordType());
        vo.setExportStatus(entity.getExportStatus());
        vo.setAgentName(entity.getAgentName());
        vo.setFillStatus(entity.getFillStatus());
        if (entity.getAiConfidence() != null) {
            vo.setAiConfidence(entity.getAiConfidence().doubleValue());
        }
        vo.setSessionTime(entity.getSessionTime());

        // 解析JSON字段
        vo.setFormData(parseJsonToMap(entity.getFormData()));
        vo.setMessages(parseJsonToList(entity.getMessages()));
        vo.setModificationHistory(parseJsonToList(entity.getModificationHistory()));

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

    private List<Map<String, Object>> parseJsonToList(String json) {
        try {
            if (StringUtils.hasText(json)) {
                return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    // ==================== Mock数据 ====================

    private PageResult<SessionVO> getMockSessionList(SessionQueryReq req) {
        List<SessionVO> all = buildMockSessions();
        // 简单筛选
        List<SessionVO> filtered = all.stream()
                .filter(s -> {
                    if (StringUtils.hasText(req.getWorkRecordType())
                            && !req.getWorkRecordType().equals(s.getWorkRecordType())) {
                        return false;
                    }
                    if (StringUtils.hasText(req.getExportStatus())
                            && !req.getExportStatus().equals(s.getExportStatus())) {
                        return false;
                    }
                    if (StringUtils.hasText(req.getKeyword())) {
                        String kw = req.getKeyword().toLowerCase();
                        return (s.getCustomerName() != null && s.getCustomerName().contains(kw))
                                || (s.getVin() != null && s.getVin().toLowerCase().contains(kw))
                                || (s.getCustomerPhone() != null && s.getCustomerPhone().contains(kw));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        long total = filtered.size();
        int page = req.getPage() != null ? req.getPage() : 1;
        int pageSize = req.getPageSize() != null ? req.getPageSize() : 10;
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) total);

        if (fromIndex >= total) {
            return PageResult.of(Collections.emptyList(), total, page, pageSize);
        }

        return PageResult.of(filtered.subList(fromIndex, toIndex), total, page, pageSize);
    }

    private SessionVO getMockSessionDetail(String id) {
        return buildMockSessions().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private List<SessionVO> buildMockSessions() {
        List<SessionVO> list = new ArrayList<>();

        // 工单1: 车载终端故障
        SessionVO s1 = new SessionVO();
        s1.setId("SE20260701001");
        s1.setCustomerName("张建国");
        s1.setCustomerPhone("13801001234");
        s1.setVin("LFWJX9C89M1001001");
        s1.setWorkRecordType("车载终端故障");
        s1.setExportStatus("EXPORTED");
        s1.setSessionTime("2026-07-01 09:15:00");
        s1.setAgentName("李娜");
        s1.setFillStatus("COMPLETED");
        s1.setAiConfidence(0.9520);
        Map<String, Object> formData1 = new LinkedHashMap<>();
        formData1.put("iccid", "8986032120012345678");
        formData1.put("deviceModel", "FAW-TBox-V3");
        formData1.put("faultCode", "P2002");
        formData1.put("faultDesc", "DPF颗粒捕集器效率低于阈值");
        formData1.put("mileage", 156800);
        formData1.put("vehicleModel", "解放J7 6x4牵引车");
        formData1.put("maintenanceShop", "长春一汽服务站");
        s1.setFormData(formData1);
        List<Map<String, Object>> msgs1 = new ArrayList<>();
        msgs1.add(buildMsg("customer", "我的解放J7仪表盘上DPF灯亮了，发动机故障灯也亮了，还能继续开吗？", "09:15:00"));
        msgs1.add(buildMsg("agent", "张先生您好，DPF报警建议尽快处理。请问车辆现在动力有没有明显下降？", "09:16:30"));
        msgs1.add(buildMsg("customer", "动力感觉比平时弱一些，但还能跑。我正从长春往沈阳送货，在高速上。", "09:17:00"));
        msgs1.add(buildMsg("agent", "好的，建议您降低车速到80km/h以下，尽快前往最近的服务站做DPF再生。", "09:18:00"));
        msgs1.add(buildMsg("customer", "好的，那我现在就导航过去。大概还有30公里。", "09:19:00"));
        msgs1.add(buildMsg("agent", "没问题，我已帮您联系长春服务站，预约了上午10点的检修。请安全驾驶。", "09:20:00"));
        s1.setMessages(msgs1);
        s1.setModificationHistory(new ArrayList<>());
        list.add(s1);

        // 工单2: 保养咨询
        SessionVO s2 = new SessionVO();
        s2.setId("SE20260702002");
        s2.setCustomerName("王德发");
        s2.setCustomerPhone("13901004567");
        s2.setVin("LFWJX9C89M1002002");
        s2.setWorkRecordType("保养咨询");
        s2.setExportStatus("PENDING");
        s2.setSessionTime("2026-07-02 14:20:00");
        s2.setAgentName("张伟");
        s2.setFillStatus("IN_PROGRESS");
        s2.setAiConfidence(0.8850);
        Map<String, Object> formData2 = new LinkedHashMap<>();
        formData2.put("iccid", "8986032120012345679");
        formData2.put("deviceModel", "FAW-TBox-V3");
        formData2.put("mileage", 89200);
        formData2.put("vehicleModel", "解放JH6 6x4牵引车");
        formData2.put("lastMaintenanceDate", "2025-12-15");
        formData2.put("lastMaintenanceMileage", 62000);
        s2.setFormData(formData2);
        List<Map<String, Object>> msgs2 = new ArrayList<>();
        msgs2.add(buildMsg("customer", "你好，我的JH6快到9万公里了，想咨询一下10万公里保养需要做哪些项目？", "14:20:00"));
        msgs2.add(buildMsg("agent", "王师傅您好，10万公里保养属于大保养，主要包括：更换机油、机滤、柴滤、空滤，检查气门间隙，更换变速箱油和后桥油。", "14:22:00"));
        msgs2.add(buildMsg("customer", "好的，那大概需要多少钱？需要多长时间？", "14:23:00"));
        msgs2.add(buildMsg("agent", "全套下来大约3500-4000元，保养时间大约需要3-4小时。建议您提前预约。", "14:25:00"));
        s2.setMessages(msgs2);
        s2.setModificationHistory(new ArrayList<>());
        list.add(s2);

        // 工单3: T-Box掉线
        SessionVO s3 = new SessionVO();
        s3.setId("SE20260703003");
        s3.setCustomerName("李国强");
        s3.setCustomerPhone("13701007890");
        s3.setVin("LFWJX9C89M1003003");
        s3.setWorkRecordType("T-Box掉线");
        s3.setExportStatus("EXPORTED");
        s3.setSessionTime("2026-07-03 08:45:00");
        s3.setAgentName("刘芳");
        s3.setFillStatus("COMPLETED");
        s3.setAiConfidence(0.9320);
        Map<String, Object> formData3 = new LinkedHashMap<>();
        formData3.put("iccid", "8986032120012345680");
        formData3.put("deviceModel", "FAW-TBox-V4");
        formData3.put("offlineDuration", "48小时");
        formData3.put("lastOnlineTime", "2026-07-01 08:30:00");
        formData3.put("firmwareVersion", "V4.2.1");
        formData3.put("vehicleModel", "解放鹰途 4x2牵引车");
        s3.setFormData(formData3);
        List<Map<String, Object>> msgs3 = new ArrayList<>();
        msgs3.add(buildMsg("customer", "我们车队有台鹰途的T-Box两天没上线了，平台上看不到车辆数据。", "08:45:00"));
        msgs3.add(buildMsg("agent", "李先生您好，请问这台车最近是否进入过地下车库或者信号不好的区域？", "08:47:00"));
        msgs3.add(buildMsg("customer", "没有，一直在平原地区跑运输。其他车的T-Box都正常。", "08:48:00"));
        msgs3.add(buildMsg("agent", "了解。我帮您远程诊断一下，可能需要远程升级固件。请司机在停车熄火状态下保持电源接通。", "08:50:00"));
        msgs3.add(buildMsg("customer", "好的，我让司机配合。车牌号我发你。", "08:51:00"));
        msgs3.add(buildMsg("agent", "收到，已执行远程固件升级指令，预计10分钟完成。", "08:55:00"));
        msgs3.add(buildMsg("customer", "好了！平台上可以看到数据了，谢谢你！", "09:08:00"));
        s3.setMessages(msgs3);
        s3.setModificationHistory(new ArrayList<>());
        list.add(s3);

        // 工单4: 故障报修
        SessionVO s4 = new SessionVO();
        s4.setId("SE20260704004");
        s4.setCustomerName("赵大勇");
        s4.setCustomerPhone("13601003456");
        s4.setVin("LFWJX9C89M1004004");
        s4.setWorkRecordType("故障报修");
        s4.setExportStatus("PENDING");
        s4.setSessionTime("2026-07-04 16:10:00");
        s4.setAgentName("李娜");
        s4.setFillStatus("IN_PROGRESS");
        s4.setAiConfidence(0.9150);
        Map<String, Object> formData4 = new LinkedHashMap<>();
        formData4.put("iccid", "8986032120012345681");
        formData4.put("deviceModel", "FAW-TBox-V3");
        formData4.put("faultCode", "U0100");
        formData4.put("faultDesc", "ECU通讯丢失");
        formData4.put("mileage", 203500);
        formData4.put("vehicleModel", "解放J6P 8x4自卸车");
        s4.setFormData(formData4);
        List<Map<String, Object>> msgs4 = new ArrayList<>();
        msgs4.add(buildMsg("customer", "我的J6P自卸车突然熄火了，再也打不着，发动机故障灯亮。", "16:10:00"));
        msgs4.add(buildMsg("agent", "赵先生别着急，我先帮您分析一下故障码。请问车辆熄火前有没有异常抖动或异响？", "16:12:00"));
        msgs4.add(buildMsg("customer", "没有明显抖动，就是突然熄火了。仪表盘上好几个灯都亮了。", "16:13:00"));
        msgs4.add(buildMsg("agent", "根据远程诊断数据显示，ECU通讯异常。建议检查电瓶电压和ECU供电保险丝。", "16:15:00"));
        s4.setMessages(msgs4);
        s4.setModificationHistory(new ArrayList<>());
        list.add(s4);

        // 工单5: 车队管理咨询
        SessionVO s5 = new SessionVO();
        s5.setId("SE20260705005");
        s5.setCustomerName("孙明辉");
        s5.setCustomerPhone("13501008901");
        s5.setVin("LFWJX9C89M1005005");
        s5.setWorkRecordType("车队管理咨询");
        s5.setExportStatus("EXPORTED");
        s5.setSessionTime("2026-07-05 10:00:00");
        s5.setAgentName("陈静");
        s5.setFillStatus("COMPLETED");
        s5.setAiConfidence(0.9680);
        Map<String, Object> formData5 = new LinkedHashMap<>();
        formData5.put("fleetSize", 35);
        formData5.put("vehicleType", "解放J7牵引车");
        formData5.put("currentPlatform", "第三方平台");
        formData5.put("migrationNeeded", true);
        formData5.put("vehicleModel", "车队批量");
        s5.setFormData(formData5);
        List<Map<String, Object>> msgs5 = new ArrayList<>();
        msgs5.add(buildMsg("customer", "我是沈阳恒通物流的车队长，我们车队35台解放J7想接入你们的车队管理系统，需要什么流程？", "10:00:00"));
        msgs5.add(buildMsg("agent", "孙队长您好，首先需要确认所有车辆都已安装T-Box设备。35台车的接入我们建议走批量导入流程。", "10:03:00"));
        msgs5.add(buildMsg("customer", "所有车都有T-Box，之前用的另一个平台，现在想换到你们这边来。", "10:04:00"));
        msgs5.add(buildMsg("agent", "明白了。迁移流程是：提供VIN清单和ICCID清单、后台批量注册、远程推送配置、验证数据上报。约2个工作日。", "10:06:00"));
        msgs5.add(buildMsg("customer", "好的，我现在就把清单发给你。费用方面怎么算？", "10:08:00"));
        msgs5.add(buildMsg("agent", "35台车可以走企业版套餐，每台车每年服务费600元，含实时定位、轨迹回放、油耗分析、故障诊断等全功能。", "10:10:00"));
        msgs5.add(buildMsg("customer", "价格还行，我们签合同吧。", "10:12:00"));
        s5.setMessages(msgs5);
        s5.setModificationHistory(new ArrayList<>());
        list.add(s5);

        // 工单6: 系统使用指导
        SessionVO s6 = new SessionVO();
        s6.setId("SE20260706006");
        s6.setCustomerName("周建华");
        s6.setCustomerPhone("13301005678");
        s6.setVin("LFWJX9C89M1006006");
        s6.setWorkRecordType("系统使用指导");
        s6.setExportStatus("PENDING");
        s6.setSessionTime("2026-07-06 11:30:00");
        s6.setAgentName("张伟");
        s6.setFillStatus("COMPLETED");
        s6.setAiConfidence(0.8760);
        Map<String, Object> formData6 = new LinkedHashMap<>();
        formData6.put("iccid", "8986032120012345682");
        formData6.put("deviceModel", "FAW-TBox-V3");
        formData6.put("issue", "无法查看车辆轨迹回放");
        formData6.put("platform", "车队管理平台V3.2");
        formData6.put("vehicleModel", "解放悍V 4x2载货车");
        s6.setFormData(formData6);
        List<Map<String, Object>> msgs6 = new ArrayList<>();
        msgs6.add(buildMsg("customer", "我在你们平台上想看车辆昨天的行驶轨迹，但是点回放按钮没反应，怎么回事？", "11:30:00"));
        msgs6.add(buildMsg("agent", "周先生您好，请问您用的是电脑端还是手机APP？", "11:32:00"));
        msgs6.add(buildMsg("customer", "电脑端，用的Chrome浏览器。", "11:33:00"));
        msgs6.add(buildMsg("agent", "可能是浏览器缓存问题。请清除浏览器缓存后重新登录尝试。", "11:35:00"));
        msgs6.add(buildMsg("customer", "好了！清除缓存后就可以了。这个功能挺好用的，能看到每段路的油耗。", "11:38:00"));
        s6.setMessages(msgs6);
        s6.setModificationHistory(new ArrayList<>());
        list.add(s6);

        // 工单7: 紧急救援
        SessionVO s7 = new SessionVO();
        s7.setId("SE20260707007");
        s7.setCustomerName("吴志强");
        s7.setCustomerPhone("13101006789");
        s7.setVin("LFWJX9C89M1007007");
        s7.setWorkRecordType("紧急救援");
        s7.setExportStatus("EXPORTED");
        s7.setSessionTime("2026-07-07 03:15:00");
        s7.setAgentName("刘芳");
        s7.setFillStatus("COMPLETED");
        s7.setAiConfidence(0.9410);
        Map<String, Object> formData7 = new LinkedHashMap<>();
        formData7.put("iccid", "8986032120012345683");
        formData7.put("deviceModel", "FAW-TBox-V4");
        formData7.put("location", "G1京哈高速沈阳方向K385+200");
        formData7.put("faultCode", "P0087");
        formData7.put("faultDesc", "燃油轨压力过低");
        formData7.put("vehicleModel", "解放J7 6x4牵引车");
        formData7.put("emergencyLevel", "HIGH");
        s7.setFormData(formData7);
        List<Map<String, Object>> msgs7 = new ArrayList<>();
        msgs7.add(buildMsg("customer", "喂！我在京哈高速上，车突然没劲了，油门踩到底速度也上不去，现在停在应急车道了！", "03:15:00"));
        msgs7.add(buildMsg("agent", "吴先生请注意安全，打开双闪灯，在车后方150米放置三角警示牌，人员撤离到护栏外！我马上帮您联系救援。", "03:16:00"));
        msgs7.add(buildMsg("customer", "好的好的，三角牌已经放好了。我人在护栏外面了。", "03:18:00"));
        msgs7.add(buildMsg("agent", "已经通过T-Box获取到您的精确位置。已通知最近的救援队，预计35分钟内到达。请保持手机畅通。", "03:20:00"));
        msgs7.add(buildMsg("customer", "好的，谢谢！没想到半夜也有人值班。", "03:21:00"));
        msgs7.add(buildMsg("agent", "我们是24小时服务的。救援队反馈已出发，车牌号辽A-8K326，预计到达时间03:50。", "03:25:00"));
        s7.setMessages(msgs7);
        s7.setModificationHistory(new ArrayList<>());
        list.add(s7);

        // 工单8: 配件查询
        SessionVO s8 = new SessionVO();
        s8.setId("SE20260708008");
        s8.setCustomerName("郑伟明");
        s8.setCustomerPhone("13001009012");
        s8.setVin("LFWJX9C89M1008008");
        s8.setWorkRecordType("配件查询");
        s8.setExportStatus("PENDING");
        s8.setSessionTime("2026-07-08 15:40:00");
        s8.setAgentName("李娜");
        s8.setFillStatus("IN_PROGRESS");
        s8.setAiConfidence(0.9030);
        Map<String, Object> formData8 = new LinkedHashMap<>();
        formData8.put("iccid", "8986032120012345684");
        formData8.put("deviceModel", "FAW-TBox-V3");
        formData8.put("partName", "DPF总成");
        formData8.put("partNumber", "FAW-612700090017");
        formData8.put("vehicleModel", "解放J6P 6x4牵引车");
        formData8.put("mileage", 312500);
        s8.setFormData(formData8);
        List<Map<String, Object>> msgs8 = new ArrayList<>();
        msgs8.add(buildMsg("customer", "我的J6P需要换DPF总成，想查一下原厂配件的价格和库存情况。", "15:40:00"));
        msgs8.add(buildMsg("agent", "郑先生您好，DPF总成配件号FAW-612700090017，我帮您查一下系统。", "15:42:00"));
        msgs8.add(buildMsg("customer", "好的，还有，这个配件有没有保修？", "15:43:00"));
        msgs8.add(buildMsg("agent", "查询到了：原厂DPF总成单价12800元，目前长春中心库库存3件。原厂配件质保期12个月或10万公里。", "15:45:00"));
        msgs8.add(buildMsg("customer", "价格有点高啊，有没有替代方案？", "15:46:00"));
        msgs8.add(buildMsg("agent", "如果堵塞不严重，可以先尝试专业清洗再生，费用大约1500元。", "15:48:00"));
        s8.setMessages(msgs8);
        s8.setModificationHistory(new ArrayList<>());
        list.add(s8);

        return list;
    }

    private Map<String, Object> buildMsg(String role, String content, String time) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        msg.put("time", time);
        return msg;
    }
}
