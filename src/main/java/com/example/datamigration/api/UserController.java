package com.example.datamigration.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.datamigration.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.example.datamigration.entity.User;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 📌 1. 新增
    @PostMapping("/add")
    public String add(@RequestBody User user) {
        userService.save(user);
        return "success";
    }

    // 📌 2. 查询全部
    @GetMapping("/list")
    public List<User> list() {
        return userService.list();
    }

    // 📌 3. 分页查询
    @GetMapping("/page")
    public Page<User> page(@RequestParam int page,
                           @RequestParam int size) {
        return userService.page(new Page<>(page, size));
    }

    // 📌 4. 根据 ID 查询
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    // 📌 5. 修改
    @PutMapping("/put")
    public String update(@RequestBody User user) {
        userService.updateById(user);
        return "success";
    }

    // 📌 6. 删除（逻辑删除）
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        userService.removeById(id);
        return "success";
    }
}

