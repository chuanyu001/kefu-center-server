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
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
            // 更新可编辑的业务字段
            if (StringUtils.hasText(req.getIccid())) {
                entity.setIccid(req.getIccid());
            }
            if (StringUtils.hasText(req.getConsultationScenario())) {
                entity.setConsultationScenario(req.getConsultationScenario());
            }
            if (StringUtils.hasText(req.getProblemType())) {
                entity.setProblemType(req.getProblemType());
            }
            if (StringUtils.hasText(req.getTemporarySolution())) {
                entity.setTemporarySolution(req.getTemporarySolution());
            }
            if (StringUtils.hasText(req.getSpecialNotes())) {
                entity.setSpecialNotes(req.getSpecialNotes());
            }

            sessionMapper.updateById(entity);
            log.info("工作表 {} 更新成功", id);
        } catch (Exception e) {
            log.error("更新工作表失败: {}", e.getMessage());
            throw new RuntimeException("更新失败: " + e.getMessage());
        }
    }

    /**
     * 实体转视图对象 — 直接映射列字段，JSON字段解析为List/Map
     */
    private SessionVO toVO(SessionEntity entity) {
        SessionVO vo = new SessionVO();
        vo.setId(entity.getId());
        vo.setCustomerName(entity.getCustomerName());
        vo.setCustomerPhone(entity.getCustomerPhone());
        vo.setVin(entity.getVin());
        vo.setWorkRecordType(entity.getWorkRecordType());
        vo.setSessionTime(entity.getSessionTime());
        vo.setAgentName(entity.getAgentName());

        // 业务字段 — 直接映射
        vo.setIccid(entity.getIccid());
        vo.setCarModel(entity.getCarModel());
        vo.setFuelType(entity.getFuelType());
        vo.setTerminalNumber(entity.getTerminalNumber());
        vo.setSimCard(entity.getSimCard());
        vo.setManufacturer(entity.getManufacturer());
        vo.setRecorderModel(entity.getRecorderModel());
        vo.setConsultationScenario(entity.getConsultationScenario());
        vo.setProblemType(entity.getProblemType());
        vo.setTemporarySolution(entity.getTemporarySolution());
        vo.setSpecialNotes(entity.getSpecialNotes());

        // JSON字段解析
        vo.setChatMessages(parseJsonToList(entity.getChatMessages()));
        vo.setModificationHistory(parseJsonToList(entity.getModificationHistory()));

        // 时间字段
        if (entity.getCreatedAt() != null) {
            vo.setCreatedAt(entity.getCreatedAt().format(DTF));
        }
        if (entity.getUpdatedAt() != null) {
            vo.setUpdatedAt(entity.getUpdatedAt().format(DTF));
        }

        return vo;
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

        // 工单1: 车载终端故障 - DPF报警
        SessionVO s1 = new SessionVO();
        s1.setId("SE20260701001");
        s1.setCustomerName("张建国");
        s1.setCustomerPhone("13801001234");
        s1.setVin("LFWJX9C89M1001001");
        s1.setWorkRecordType("after_sales");
        s1.setSessionTime("2026-07-01 09:15:00");
        s1.setAgentName("李娜");
        s1.setIccid("8986032120012345678");
        s1.setCarModel("解放J7 6x4牵引车");
        s1.setFuelType("柴油");
        s1.setTerminalNumber("TBox-V3-20210315");
        s1.setSimCard("13801001234");
        s1.setManufacturer("一汽解放");
        s1.setRecorderModel("FAW-REC-V2");
        s1.setConsultationScenario("行驶中仪表报警");
        s1.setProblemType("DPF颗粒捕集器效率低");
        s1.setTemporarySolution("降低车速至80km/h以下，前往最近服务站进行DPF强制再生");
        s1.setSpecialNotes("已联系长春一汽服务站，预约上午10点检修");
        List<Map<String, Object>> msgs1 = new ArrayList<>();
        msgs1.add(buildMsg("customer", "我的解放J7仪表盘上DPF灯亮了，发动机故障灯也亮了，还能继续开吗？", "09:15:00"));
        msgs1.add(buildMsg("agent", "张先生您好，DPF报警建议尽快处理。请问车辆现在动力有没有明显下降？", "09:16:30"));
        msgs1.add(buildMsg("customer", "动力感觉比平时弱一些，但还能跑。我正从长春往沈阳送货，在高速上。", "09:17:00"));
        msgs1.add(buildMsg("agent", "好的，建议您降低车速到80km/h以下，尽快前往最近的服务站做DPF再生。", "09:18:00"));
        msgs1.add(buildMsg("customer", "好的，那我现在就导航过去。大概还有30公里。", "09:19:00"));
        msgs1.add(buildMsg("agent", "没问题，我已帮您联系长春服务站，预约了上午10点的检修。请安全驾驶。", "09:20:00"));
        s1.setChatMessages(msgs1);
        s1.setModificationHistory(new ArrayList<>());
        s1.setCreatedAt("2026-07-01 09:15:00");
        s1.setUpdatedAt("2026-07-01 10:30:00");
        list.add(s1);

        // 工单2: 保养咨询
        SessionVO s2 = new SessionVO();
        s2.setId("SE20260702002");
        s2.setCustomerName("王德发");
        s2.setCustomerPhone("13901004567");
        s2.setVin("LFWJX9C89M1002002");
        s2.setWorkRecordType("after_sales");
        s2.setSessionTime("2026-07-02 14:20:00");
        s2.setAgentName("张伟");
        s2.setIccid("8986032120012345679");
        s2.setCarModel("解放JH6 6x4牵引车");
        s2.setFuelType("柴油");
        s2.setTerminalNumber("TBox-V3-20210420");
        s2.setSimCard("13901004567");
        s2.setManufacturer("一汽解放");
        s2.setRecorderModel("FAW-REC-V2");
        s2.setConsultationScenario("日常保养咨询");
        s2.setProblemType("10万公里大保养项目");
        s2.setTemporarySolution("已提供保养项目清单和费用估算，客户自行预约服务站");
        s2.setSpecialNotes("客户上次保养时间2025-12-15，里程62000km，当前89200km");
        List<Map<String, Object>> msgs2 = new ArrayList<>();
        msgs2.add(buildMsg("customer", "你好，我的JH6快到9万公里了，想咨询一下10万公里保养需要做哪些项目？", "14:20:00"));
        msgs2.add(buildMsg("agent", "王师傅您好，10万公里保养属于大保养，主要包括：更换机油、机滤、柴滤、空滤，检查气门间隙，更换变速箱油和后桥油。", "14:22:00"));
        msgs2.add(buildMsg("customer", "好的，那大概需要多少钱？需要多长时间？", "14:23:00"));
        msgs2.add(buildMsg("agent", "全套下来大约3500-4000元，保养时间大约需要3-4小时。建议您提前预约。", "14:25:00"));
        s2.setChatMessages(msgs2);
        s2.setModificationHistory(new ArrayList<>());
        s2.setCreatedAt("2026-07-02 14:20:00");
        s2.setUpdatedAt("2026-07-02 14:25:00");
        list.add(s2);

        // 工单3: T-Box掉线
        SessionVO s3 = new SessionVO();
        s3.setId("SE20260703003");
        s3.setCustomerName("李国强");
        s3.setCustomerPhone("13701007890");
        s3.setVin("LFWJX9C89M1003003");
        s3.setWorkRecordType("after_sales");
        s3.setSessionTime("2026-07-03 08:45:00");
        s3.setAgentName("刘芳");
        s3.setIccid("8986032120012345680");
        s3.setCarModel("解放鹰途 4x2牵引车");
        s3.setFuelType("柴油");
        s3.setTerminalNumber("TBox-V4-20220310");
        s3.setSimCard("13701007890");
        s3.setManufacturer("一汽解放");
        s3.setRecorderModel("FAW-REC-V3");
        s3.setConsultationScenario("T-Box设备离线");
        s3.setProblemType("T-Box固件版本过低导致持续掉线");
        s3.setTemporarySolution("远程推送固件V4.2.1升级，10分钟完成，设备恢复正常");
        s3.setSpecialNotes("V3终端固件版本低于V4.2.1的设备存在已知掉线bug");
        List<Map<String, Object>> msgs3 = new ArrayList<>();
        msgs3.add(buildMsg("customer", "我们车队有台鹰途的T-Box两天没上线了，平台上看不到车辆数据。", "08:45:00"));
        msgs3.add(buildMsg("agent", "李先生您好，请问这台车最近是否进入过地下车库或者信号不好的区域？", "08:47:00"));
        msgs3.add(buildMsg("customer", "没有，一直在平原地区跑运输。其他车的T-Box都正常。", "08:48:00"));
        msgs3.add(buildMsg("agent", "了解。我帮您远程诊断一下，可能需要远程升级固件。请司机在停车熄火状态下保持电源接通。", "08:50:00"));
        msgs3.add(buildMsg("customer", "好的，我让司机配合。车牌号我发你。", "08:51:00"));
        msgs3.add(buildMsg("agent", "收到，已执行远程固件升级指令，预计10分钟完成。", "08:55:00"));
        msgs3.add(buildMsg("customer", "好了！平台上可以看到数据了，谢谢你！", "09:08:00"));
        s3.setChatMessages(msgs3);
        s3.setModificationHistory(new ArrayList<>());
        s3.setCreatedAt("2026-07-03 08:45:00");
        s3.setUpdatedAt("2026-07-03 09:30:00");
        list.add(s3);

        // 工单4: 故障报修 - ECU通讯丢失
        SessionVO s4 = new SessionVO();
        s4.setId("SE20260704004");
        s4.setCustomerName("赵大勇");
        s4.setCustomerPhone("13601003456");
        s4.setVin("LFWJX9C89M1004004");
        s4.setWorkRecordType("after_sales");
        s4.setSessionTime("2026-07-04 16:10:00");
        s4.setAgentName("李娜");
        s4.setIccid("8986032120012345681");
        s4.setCarModel("解放J6P 8x4自卸车");
        s4.setFuelType("柴油");
        s4.setTerminalNumber("TBox-V3-20200815");
        s4.setSimCard("13601003456");
        s4.setManufacturer("一汽解放");
        s4.setRecorderModel("FAW-REC-V2");
        s4.setConsultationScenario("车辆行驶中突然熄火");
        s4.setProblemType("ECU通讯丢失(故障码U0100)");
        s4.setTemporarySolution("指导客户检查电瓶负极线，发现松动，紧固后恢复正常");
        s4.setSpecialNotes("工程车辆作业震动大，电瓶桩头松动是常见故障原因");
        List<Map<String, Object>> msgs4 = new ArrayList<>();
        msgs4.add(buildMsg("customer", "我的J6P自卸车突然熄火了，再也打不着，发动机故障灯亮。", "16:10:00"));
        msgs4.add(buildMsg("agent", "赵先生别着急，我先帮您分析一下故障码。请问车辆熄火前有没有异常抖动或异响？", "16:12:00"));
        msgs4.add(buildMsg("customer", "没有明显抖动，就是突然熄火了。仪表盘上好几个灯都亮了。", "16:13:00"));
        msgs4.add(buildMsg("agent", "根据远程诊断数据显示，ECU通讯异常。建议检查电瓶电压和ECU供电保险丝。", "16:15:00"));
        s4.setChatMessages(msgs4);
        s4.setModificationHistory(new ArrayList<>());
        s4.setCreatedAt("2026-07-04 16:10:00");
        s4.setUpdatedAt("2026-07-04 16:15:00");
        list.add(s4);

        // 工单5: 车队管理咨询 - 批量接入
        SessionVO s5 = new SessionVO();
        s5.setId("SE20260705005");
        s5.setCustomerName("孙明辉");
        s5.setCustomerPhone("13501008901");
        s5.setVin("LFWJX9C89M1005005");
        s5.setWorkRecordType("fleet_register");
        s5.setSessionTime("2026-07-05 10:00:00");
        s5.setAgentName("陈静");
        s5.setIccid("");
        s5.setCarModel("解放J7 6x4牵引车(批量35台)");
        s5.setFuelType("柴油");
        s5.setTerminalNumber("");
        s5.setSimCard("");
        s5.setManufacturer("一汽解放");
        s5.setRecorderModel("FAW-REC-V2");
        s5.setConsultationScenario("车队批量接入智能管理平台");
        s5.setProblemType("第三方平台迁移到鱼快创领系统");
        s5.setTemporarySolution("提供VIN+ICCID清单后批量注册，2个工作日内完成35台车迁移");
        s5.setSpecialNotes("签约企业版套餐，每台车600元/年，含全功能");
        List<Map<String, Object>> msgs5 = new ArrayList<>();
        msgs5.add(buildMsg("customer", "我是沈阳恒通物流的车队长，我们车队35台解放J7想接入你们的车队管理系统，需要什么流程？", "10:00:00"));
        msgs5.add(buildMsg("agent", "孙队长您好，首先需要确认所有车辆都已安装T-Box设备。35台车的接入我们建议走批量导入流程。", "10:03:00"));
        msgs5.add(buildMsg("customer", "所有车都有T-Box，之前用的另一个平台，现在想换到你们这边来。", "10:04:00"));
        msgs5.add(buildMsg("agent", "明白了。迁移流程是：提供VIN清单和ICCID清单、后台批量注册、远程推送配置、验证数据上报。约2个工作日。", "10:06:00"));
        msgs5.add(buildMsg("customer", "好的，我现在就把清单发给你。费用方面怎么算？", "10:08:00"));
        msgs5.add(buildMsg("agent", "35台车可以走企业版套餐，每台车每年服务费600元，含实时定位、轨迹回放、油耗分析、故障诊断等全功能。", "10:10:00"));
        msgs5.add(buildMsg("customer", "价格还行，我们签合同吧。", "10:12:00"));
        s5.setChatMessages(msgs5);
        s5.setModificationHistory(new ArrayList<>());
        s5.setCreatedAt("2026-07-05 10:00:00");
        s5.setUpdatedAt("2026-07-05 11:00:00");
        list.add(s5);

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
