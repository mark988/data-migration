package com.example.datamigration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.datamigration.entity.TestCaseInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 测试用例信息表Mapper
 */
@Mapper
public interface TestCaseInfoMapper extends BaseMapper<TestCaseInfo> {

    /**
     * 分页查询测试用例信息
     * 用于批量迁移数据
     *
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 测试用例列表
     */
    @Select("SELECT id, title, step, suit, case_id FROM test_case_info LIMIT #{offset}, #{limit}")
    List<TestCaseInfo> selectByPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询总记录数
     *
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM test_case_info")
    long selectTotalCount();
}
