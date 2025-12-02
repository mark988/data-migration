package com.example.datamigration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 测试用例步骤表（目标表）
 */
@Data
@TableName("test_case_step")
public class TestCaseStep {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 步骤名称
     */
    private String name;

    /**
     * 预期结果
     */
    private String result;

    /**
     * 关联的用例池ID（对应test_case_pool.id）
     */
    private Integer caseId;
}
