package com.shike.mapper;

import com.github.pagehelper.Page;
import com.shike.anno.AutoFill;
import com.shike.dto.EmployeePageQueryDTO;
import com.shike.entity.Employee;
import com.shike.enumeration.OperationType;
import com.shike.service.EmployeeService;
import com.shike.vo.EmployeeVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);


    /**
     * 新增员工接口
     * @param employee
     */
    @Insert("INSERT INTO employee (name,username,password,phone,sex,id_number,create_time,update_time,create_user,update_user,status)"+
            " values(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{createTime},#{updateTime},#{createUser},#{updateUser},#{status})")
    @AutoFill(value= OperationType.INSERT)
    void add(Employee employee);


    /**
     * 分页查询
     * @param pageQueryDTO
     * @return
     */
    Page<Employee> pageQuery(EmployeePageQueryDTO pageQueryDTO);
    @AutoFill(value=OperationType.UPDATE)
    void update(Employee employee);


    @Select("select * from employee where id =#{id}")
    EmployeeVO getEmpById(Long id);
}
