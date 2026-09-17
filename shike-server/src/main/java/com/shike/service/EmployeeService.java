package com.shike.service;

import com.shike.dto.EmployeeLoginDTO;
import com.shike.entity.Employee;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

}
