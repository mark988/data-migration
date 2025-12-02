package com.example.datamigration.api;

import com.example.datamigration.service.DataMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据迁移控制器
 * 提供HTTP接口来触发和管理数据迁移任务
 */
@Slf4j
@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    @Autowired
    private DataMigrationService dataMigrationService;

    /**
     * 迁移任务运行状态标识
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 最近一次迁移结果
     */
    private DataMigrationService.MigrationResult lastResult;

    /**
     * 启动数据迁移
     * 异步执行迁移任务，避免HTTP请求超时
     *
     * @return 响应结果
     */
    @GetMapping("/start")
    public ResponseEntity<Map<String, Object>> startMigration() {
        Map<String, Object> response = new HashMap<>();

        // 检查是否已有任务在运行
        if (isRunning.get()) {
            response.put("success", false);
            response.put("message", "迁移任务正在运行中，请勿重复启动");
            return ResponseEntity.ok(response);
        }

        // 标记任务开始
        if (!isRunning.compareAndSet(false, true)) {
            response.put("success", false);
            response.put("message", "迁移任务正在运行中，请勿重复启动");
            return ResponseEntity.ok(response);
        }

        // 异步执行迁移任务
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始执行数据迁移任务");
                DataMigrationService.MigrationResult result = dataMigrationService.executeMigration();
                lastResult = result;
                log.info("数据迁移任务完成");
            } catch (Exception e) {
                log.error("数据迁移任务执行异常", e);
            } finally {
                // 任务完成，重置标识
                isRunning.set(false);
            }
        });

        response.put("success", true);
        response.put("message", "数据迁移任务已启动，请使用 /api/migration/status 查询进度");
        return ResponseEntity.ok(response);
    }

    /**
     * 查询迁移任务状态
     *
     * @return 任务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();

        response.put("isRunning", isRunning.get());

        if (lastResult != null) {
            response.put("lastResult", lastResult);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 获取最近一次迁移结果详情
     *
     * @return 迁移结果
     */
    @GetMapping("/result")
    public ResponseEntity<Map<String, Object>> getLastResult() {
        Map<String, Object> response = new HashMap<>();

        if (lastResult == null) {
            response.put("success", false);
            response.put("message", "暂无迁移记录");
        } else {
            response.put("success", true);
            response.put("result", lastResult);
        }

        return ResponseEntity.ok(response);
    }
}
