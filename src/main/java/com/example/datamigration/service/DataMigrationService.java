package com.example.datamigration.service;

import com.example.datamigration.entity.TestCaseInfo;
import com.example.datamigration.entity.TestCasePool;
import com.example.datamigration.entity.TestCaseStep;
import com.example.datamigration.mapper.TestCaseInfoMapper;
import com.example.datamigration.mapper.TestCasePoolMapper;
import com.example.datamigration.mapper.TestCaseStepMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据迁移服务类
 * 负责将test_case_info表的数据迁移到test_case_pool和test_case_step表
 *
 * 支持两种迁移模式：
 * 1. 批量模式（推荐）- 使用MyBatis XML批量插入，性能高
 * 2. 单条模式（降级） - 逐条插入，用于批量失败时的降级处理
 */
@Slf4j
@Service
public class DataMigrationService {

    @Autowired
    private TestCaseInfoMapper testCaseInfoMapper;

    @Autowired
    private TestCasePoolMapper testCasePoolMapper;

    @Autowired
    private TestCaseStepMapper testCaseStepMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 每批处理的记录数
     * 建议值：500-2000，根据服务器性能和网络状况调整
     */
    private static final int BATCH_SIZE = 1000;

    /**
     * 失败记录日志文件路径
     */
    private static final String ERROR_LOG_FILE = "migration_error_log.txt";

    /**
     * 执行数据迁移
     * 主入口方法，协调整个迁移流程
     *
     * @return 迁移统计信息
     */
    public MigrationResult executeMigration() {
        log.info("================== 开始数据迁移 ==================");
        long startTime = System.currentTimeMillis();

        // 统计信息
        AtomicLong totalCount = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failedCount = new AtomicLong(0);
        List<Integer> failedIds = new ArrayList<>();

        // 创建错误日志文件
        String errorLogPath = initErrorLogFile();

        try {
            // 1. 查询总记录数
            long total = testCaseInfoMapper.selectTotalCount();
            totalCount.set(total);
            log.info("待迁移总记录数: {}", total);

            if (total == 0) {
                log.warn("源表无数据，迁移结束");
                return buildResult(totalCount.get(), successCount.get(), failedCount.get(),
                                 failedIds, startTime, errorLogPath);
            }

            // 2. 分批处理数据
            int batchCount = (int) Math.ceil((double) total / BATCH_SIZE);
            log.info("将分 {} 批次处理，每批 {} 条记录", batchCount, BATCH_SIZE);

            for (int i = 0; i < batchCount; i++) {
                int offset = i * BATCH_SIZE;
                log.info("处理第 {}/{} 批，offset={}", (i + 1), batchCount, offset);

                try {
                    // 查询一批数据
                    List<TestCaseInfo> batchData = testCaseInfoMapper.selectByPage(offset, BATCH_SIZE);

                    if (batchData == null || batchData.isEmpty()) {
                        log.warn("第 {} 批数据为空，跳过", (i + 1));
                        continue;
                    }

                    // 处理这批数据
                    processBatch(batchData, successCount, failedCount, failedIds, errorLogPath);

                    // 打印进度
                    long currentProgress = successCount.get() + failedCount.get();
                    double percentage = (double) currentProgress / total * 100;
                    log.info("进度: {}/{} ({:.2f}%), 成功: {}, 失败: {}",
                             currentProgress, total, percentage, successCount.get(), failedCount.get());

                } catch (Exception e) {
                    log.error("处理第 {} 批数据时发生异常，继续下一批", (i + 1), e);
                    writeErrorLog(errorLogPath, String.format("批次 %d 处理异常: %s", (i + 1), e.getMessage()));
                }
            }

        } catch (Exception e) {
            log.error("数据迁移过程中发生严重异常", e);
            writeErrorLog(errorLogPath, "严重异常: " + e.getMessage());
        }

        // 3. 打印最终统计
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;

        log.info("================== 数据迁移完成 ==================");
        log.info("总记录数: {}", totalCount.get());
        log.info("成功迁移: {}", successCount.get());
        log.info("失败记录: {}", failedCount.get());
        log.info("耗时: {} 秒", duration);
        log.info("错误日志文件: {}", errorLogPath);
        log.info("================================================");

        return buildResult(totalCount.get(), successCount.get(), failedCount.get(),
                         failedIds, startTime, errorLogPath);
    }

    /**
     * 处理一批数据
     * 策略：优先使用批量插入，失败时降级到单条处理
     *
     * @param batchData 批次数据
     * @param successCount 成功计数器
     * @param failedCount 失败计数器
     * @param failedIds 失败ID列表
     * @param errorLogPath 错误日志文件路径
     */
    private void processBatch(List<TestCaseInfo> batchData,
                             AtomicLong successCount,
                             AtomicLong failedCount,
                             List<Integer> failedIds,
                             String errorLogPath) {
        try {
            // 尝试批量处理（推荐模式，性能高）
            log.debug("尝试批量处理 {} 条记录", batchData.size());
            migrateBatch(batchData);

            // 批量成功，更新成功计数
            successCount.addAndGet(batchData.size());
            log.debug("批量处理成功，共 {} 条记录", batchData.size());

        } catch (Exception batchException) {
            // 批量处理失败，降级到单条处理
            log.warn("批量处理失败: {}，降级到单条处理模式", batchException.getMessage());
            writeErrorLog(errorLogPath, "批量处理失败，降级到单条模式: " + batchException.getMessage());

            // 逐条处理
            processBatchOneByOne(batchData, successCount, failedCount, failedIds, errorLogPath);
        }
    }

    /**
     * 逐条处理批次数据（降级模式）
     * 当批量处理失败时使用，确保每条记录独立处理
     *
     * @param batchData 批次数据
     * @param successCount 成功计数器
     * @param failedCount 失败计数器
     * @param failedIds 失败ID列表
     * @param errorLogPath 错误日志文件路径
     */
    private void processBatchOneByOne(List<TestCaseInfo> batchData,
                                     AtomicLong successCount,
                                     AtomicLong failedCount,
                                     List<Integer> failedIds,
                                     String errorLogPath) {
        for (TestCaseInfo info : batchData) {
            try {
                // 处理单条记录（带事务）
                migrateOneRecord(info);
                successCount.incrementAndGet();

            } catch (Exception e) {
                // 记录失败
                failedCount.incrementAndGet();
                failedIds.add(info.getId());

                // 写入错误日志
                String errorMsg = String.format("ID=%d 迁移失败: %s, title=%s",
                                              info.getId(), e.getMessage(), info.getTitle());
                log.error(errorMsg);
                writeErrorLog(errorLogPath, errorMsg);
            }
        }
    }

    /**
     * 批量迁移记录（推荐模式）
     * 使用MyBatis XML的批量插入功能，性能高
     * 整个批次使用一个事务，要么全部成功，要么全部失败
     *
     * @param batchData 批次数据
     * @throws Exception 处理失败时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateBatch(List<TestCaseInfo> batchData) throws Exception {
        if (batchData == null || batchData.isEmpty()) {
            return;
        }

        // 1. 准备所有的 TestCasePool 对象
        List<TestCasePool> poolList = new ArrayList<>(batchData.size());
        for (TestCaseInfo info : batchData) {
            TestCasePool pool = new TestCasePool();
            pool.setTitle(info.getTitle());
            pool.setMenuId(info.getSuit());
            poolList.add(pool);
        }

        // 2. 批量插入 test_case_pool（使用XML批量插入）
        int insertedPoolCount = testCasePoolMapper.batchInsert(poolList);
        if (insertedPoolCount != poolList.size()) {
            throw new RuntimeException(String.format(
                "批量插入test_case_pool失败，期望插入%d条，实际插入%d条",
                poolList.size(), insertedPoolCount));
        }

        log.debug("批量插入 test_case_pool 成功，共 {} 条记录", insertedPoolCount);

        // 3. 准备所有的 TestCaseStep 对象
        List<TestCaseStep> allSteps = new ArrayList<>();
        for (int i = 0; i < batchData.size(); i++) {
            TestCaseInfo info = batchData.get(i);
            TestCasePool pool = poolList.get(i);

            // 获取新插入的pool的ID
            Integer newPoolId = pool.getId();
            if (newPoolId == null) {
                throw new RuntimeException(String.format(
                    "获取test_case_pool的ID失败，原记录ID=%d", info.getId()));
            }

            // 解析step JSON字符串
            String stepJson = info.getStep();
            if (stepJson != null && !stepJson.trim().isEmpty()) {
                try {
                    // 解析JSON数组
                    List<Map<String, String>> steps = objectMapper.readValue(
                        stepJson,
                        new TypeReference<List<Map<String, String>>>() {}
                    );

                    // 构建TestCaseStep对象
                    if (steps != null && !steps.isEmpty()) {
                        for (Map<String, String> stepMap : steps) {
                            TestCaseStep step = new TestCaseStep();
                            step.setName(stepMap.get("name"));
                            step.setResult(stepMap.get("result"));
                            step.setCaseId(newPoolId);
                            allSteps.add(step);
                        }
                    }
                } catch (Exception e) {
                    // JSON解析失败，抛出异常回滚整个批次
                    throw new RuntimeException(String.format(
                        "ID=%d 的step字段JSON解析失败: %s, step=%s",
                        info.getId(), e.getMessage(), stepJson), e);
                }
            }
        }

        // 4. 批量插入 test_case_step（使用XML批量插入）
        if (!allSteps.isEmpty()) {
            int insertedStepCount = testCaseStepMapper.batchInsert(allSteps);
            if (insertedStepCount != allSteps.size()) {
                throw new RuntimeException(String.format(
                    "批量插入test_case_step失败，期望插入%d条，实际插入%d条",
                    allSteps.size(), insertedStepCount));
            }

            log.debug("批量插入 test_case_step 成功，共 {} 条记录", insertedStepCount);
        }
    }

    /**
     * 迁移单条记录（降级模式）
     * 将一条test_case_info记录转换并插入到test_case_pool和test_case_step表
     * 使用事务保证数据一致性
     *
     * 注意：保留此方法用于批量失败时的降级处理
     *
     * @param info 源记录
     * @throws Exception 处理失败时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateOneRecord(TestCaseInfo info) throws Exception {
        // 1. 数据校验
        if (info == null || info.getId() == null) {
            throw new IllegalArgumentException("记录或ID为空");
        }

        // 2. 插入test_case_pool
        TestCasePool pool = new TestCasePool();
        pool.setTitle(info.getTitle());
        pool.setMenuId(info.getSuit());

        int insertResult = testCasePoolMapper.insert(pool);
        if (insertResult <= 0) {
            throw new RuntimeException("插入test_case_pool失败");
        }

        // 3. 获取新插入的pool的ID
        Integer newPoolId = pool.getId();
        if (newPoolId == null) {
            throw new RuntimeException("获取新插入的test_case_pool的ID失败");
        }

        // 4. 解析step JSON字符串并插入test_case_step
        String stepJson = info.getStep();
        if (stepJson != null && !stepJson.trim().isEmpty()) {
            try {
                // 解析JSON数组
                List<Map<String, String>> steps = objectMapper.readValue(
                    stepJson,
                    new TypeReference<List<Map<String, String>>>() {}
                );

                // 插入步骤
                if (steps != null && !steps.isEmpty()) {
                    for (Map<String, String> stepMap : steps) {
                        TestCaseStep step = new TestCaseStep();
                        step.setName(stepMap.get("name"));
                        step.setResult(stepMap.get("result"));
                        step.setCaseId(newPoolId);

                        testCaseStepMapper.insert(step);
                    }
                }
            } catch (Exception e) {
                // JSON解析失败，抛出异常回滚
                log.warn("ID={} 的step字段JSON解析失败: {}, step={}",
                        info.getId(), e.getMessage(), stepJson);
                throw new RuntimeException("JSON解析失败: " + e.getMessage(), e);
            }
        }

        log.debug("成功迁移记录: id={}, title={}, 新pool_id={}",
                 info.getId(), info.getTitle(), newPoolId);
    }

    /**
     * 初始化错误日志文件
     * 创建带时间戳的日志文件
     *
     * @return 日志文件路径
     */
    private String initErrorLogFile() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "migration_error_" + timestamp + ".log";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("数据迁移错误日志\n");
            writer.write("开始时间: " + LocalDateTime.now() + "\n");
            writer.write("===========================================\n\n");
        } catch (IOException e) {
            log.error("创建错误日志文件失败", e);
            return ERROR_LOG_FILE;
        }

        return filename;
    }

    /**
     * 写入错误日志
     * 线程安全的日志写入方法
     *
     * @param logPath 日志文件路径
     * @param message 错误信息
     */
    private synchronized void writeErrorLog(String logPath, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logPath, true))) {
            writer.write(String.format("[%s] %s\n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        message));
        } catch (IOException e) {
            log.error("写入错误日志失败: {}", message, e);
        }
    }

    /**
     * 构建迁移结果对象
     *
     * @param total 总记录数
     * @param success 成功数
     * @param failed 失败数
     * @param failedIds 失败ID列表
     * @param startTime 开始时间
     * @param errorLogPath 错误日志路径
     * @return 迁移结果
     */
    private MigrationResult buildResult(long total, long success, long failed,
                                       List<Integer> failedIds, long startTime,
                                       String errorLogPath) {
        MigrationResult result = new MigrationResult();
        result.setTotalCount(total);
        result.setSuccessCount(success);
        result.setFailedCount(failed);
        result.setFailedIds(failedIds);
        result.setDurationSeconds((System.currentTimeMillis() - startTime) / 1000);
        result.setErrorLogPath(errorLogPath);
        return result;
    }

    /**
     * 迁移结果统计类
     */
    @lombok.Data
    public static class MigrationResult {
        /** 总记录数 */
        private long totalCount;

        /** 成功迁移数 */
        private long successCount;

        /** 失败数 */
        private long failedCount;

        /** 失败的ID列表 */
        private List<Integer> failedIds;

        /** 耗时（秒） */
        private long durationSeconds;

        /** 错误日志文件路径 */
        private String errorLogPath;
    }
}
