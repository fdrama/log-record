package com.simple.log.function.user;


import com.simple.log.model.Operator;

/**
 * @author muzhantong
 * create on 2020/4/29 5:45 下午
 */
public class DefaultOperatorGetServiceImpl implements IOperatorGetService {

    @Override
    public Operator getUser() {
        Operator operator = new Operator();
        operator.setOperatorId("Admin");
        return operator;
    }
}
