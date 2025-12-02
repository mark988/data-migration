package com.example.datamigration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datamigration.entity.TestCaseStep;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 测试用例步骤表Mapper
 */
@Mapper
public interface TestCaseStepMapper extends BaseMapper<TestCaseStep> {

    /**
     * 批量插入测试用例步骤记录
     * 使用XML配置实现批量插入，提高性能
     *
     * @param stepList 测试用例步骤列表
     * @return 插入的记录数
     */
    int batchInsert(List<TestCaseStep> stepList);
}
