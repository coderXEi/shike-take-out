package com.shike.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.shike.constant.MessageConstant;
import com.shike.constant.PasswordConstant;
import com.shike.constant.StatusConstant;
import com.shike.dto.EmployeeDTO;
import com.shike.dto.EmployeeLoginDTO;
import com.shike.dto.EmployeePageQueryDTO;
import com.shike.entity.Employee;
import com.shike.exception.AccountLockedException;
import com.shike.exception.AccountNotFoundException;
import com.shike.exception.PasswordErrorException;
import com.shike.mapper.EmployeeMapper;
import com.shike.result.PageResult;
import com.shike.service.EmployeeService;
import com.shike.utils.CurrentHolder;
import com.shike.vo.EmployeeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 对明文密码md5 编码处理 并比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        //
        BeanUtils.copyProperties(employeeDTO, employee);
        // 设置默认状态
        employee.setStatus(StatusConstant.ENABLE);
        // 设置默认密码
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        // 设置新增和更新时间  已通过autoFill注解 自动注入公共字段


        // 设置当前记录创建人id和修改人id



        employeeMapper.add(employee);
    }

    @Override
    public PageResult pageQuery(EmployeePageQueryDTO pageQueryDto) {

        PageHelper.startPage(pageQueryDto.getPage(), pageQueryDto.getPageSize());
        Page<Employee> emp =  employeeMapper.pageQuery(pageQueryDto);
        Long total = emp.getTotal();
        List<Employee> list = emp.getResult();
        return new PageResult(total, list);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();

        employeeMapper.update(employee);
    }

    @Override
    public EmployeeVO getEmpById(Long id) {

        return employeeMapper.getEmpById(id);
    }

    @Override
    public void update(EmployeeDTO employeeDTO) {

        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        employeeMapper.update(employee);

    }

}
