package com.example.datamigration.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.example.datamigration.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

