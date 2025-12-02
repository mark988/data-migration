package com.example.datamigration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 测试用例池表（目标表）
 */
@Data
@TableName("test_case_pool")
public class TestCasePool {

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
     * 菜单ID（对应test_case_info.suit）
     */
    private Integer menuId;
}
