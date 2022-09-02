package com.simple.log.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * @author fdrama
 */
@Data
@TableName("user")
public class UserDO {
    @TableId
    private Long id;
    private String name;
    private Integer age;
    private String email;
}
