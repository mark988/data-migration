package com.example.datamigration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 测试用例信息表（源表）
 */
@Data
@TableName("test_case_info")
public class TestCaseInfo {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用例标题
     */
    private String title;

    /**
     * 测试步骤，JSON字符串格式
     * 格式：[{"name":"ssss","result":"success"},{"name":"ssss","result":"success"}]
     */
    private String step;

    /**
     * 所属套件
     */
    private Integer suit;

    /**
     * 用例ID
     */
    private Integer caseId;
}
