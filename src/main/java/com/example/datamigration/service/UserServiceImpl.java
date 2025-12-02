package com.example.datamigration.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.datamigration.mapper.UserMapper;
import org.springframework.stereotype.Service;
import com.example.datamigration.entity.User;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
