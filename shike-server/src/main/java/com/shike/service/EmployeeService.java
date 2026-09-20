package com.shike.service;

import com.shike.dto.EmployeeDTO;
import com.shike.dto.EmployeeLoginDTO;
import com.shike.dto.EmployeePageQueryDTO;
import com.shike.entity.Employee;
import com.shike.result.PageResult;
import com.shike.vo.EmployeeVO;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);


    /**
     * 新增员工
     * @param employeeDTO  员工信息
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 员工列表分页查询
     * @param pageQueryDto
     */
     PageResult pageQuery(EmployeePageQueryDTO pageQueryDto);

    /**
     * 启用或禁用员工账户
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    EmployeeVO getEmpById(Long id);

    void update(EmployeeDTO employeeDTO);
}
