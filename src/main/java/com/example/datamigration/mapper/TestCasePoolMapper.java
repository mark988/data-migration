package com.example.datamigration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datamigration.entity.TestCasePool;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 测试用例池表Mapper
 */
@Mapper
public interface TestCasePoolMapper extends BaseMapper<TestCasePool> {

    /**
     * 批量插入测试用例池记录
     * 使用XML配置实现批量插入，提高性能
     *
     * @param poolList 测试用例池列表
     * @return 插入的记录数
     */
    int batchInsert(List<TestCasePool> poolList);
}
