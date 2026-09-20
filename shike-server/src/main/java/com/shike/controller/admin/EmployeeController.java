package com.shike.controller.admin;

import com.shike.anno.Log;
import com.shike.anno.NoAuth;
import com.shike.constant.JwtClaimsConstant;
import com.shike.dto.EmployeeDTO;
import com.shike.dto.EmployeeLoginDTO;
import com.shike.dto.EmployeePageQueryDTO;
import com.shike.entity.Employee;
import com.shike.properties.JwtProperties;
import com.shike.result.PageResult;
import com.shike.result.Result;
import com.shike.service.EmployeeService;
import com.shike.utils.JwtUtil;
import com.shike.vo.EmployeeLoginVO;
import com.shike.vo.EmployeeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags="员工相关接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation(value="处理员工登录")
    @NoAuth
    // DTO 前端给后端传的数据   VO 从数据库查到的数据 后端传给前端
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("处理员工退出")
    public Result<String> logout() {
        return Result.success();
    }


    @PostMapping
    @ApiOperation("新增员工接口")
    @Log
    public Result<String> save(EmployeeDTO employeeDTO) {

        log.info("正在新增员工接受到参数:{}",employeeDTO);

        employeeService.save(employeeDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("员工列表分页查询接口")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO) {

        log.info("员工分页查询参数:{}",employeePageQueryDTO);
        PageResult pr =  employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pr);
    }
    @PostMapping("/status/{status}")
    @ApiOperation("启用或禁用员工")
    public Result<?> startOrStop(@PathVariable Integer status,Long id) {
        log.info("禁用或启用员工账户:{},{}",status,id);

        employeeService.startOrStop(status,id);
        return Result.success();
    }
    /**
     * 根据id 查询员工信息
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id获取员工信息接口")
    public Result<EmployeeVO> getById(@PathVariable Long id) {

        log.info("根据id查询员工信息 {}",id);
        EmployeeVO emp = employeeService.getEmpById(id);

        return Result.success(emp);
    }

    /**
     * 更新员工信息
     */
    @PutMapping
    @ApiOperation("更新员工信息")
    public Result updateEmp(@RequestBody EmployeeDTO employeeDTO) {
        log.info("更新员工信息{}",employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }
}
