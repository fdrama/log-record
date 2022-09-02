package com.simple.log.record;

import com.google.common.collect.Lists;
import com.simple.log.LogRecordTestApplication;
import com.simple.log.model.Order;
import com.simple.log.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fdrama
 * @date 2022年08月24日 16:48
 */
@Slf4j
@SpringBootTest(classes = LogRecordTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class LogRecordTest {

    @Autowired
    private OrderService orderService;

    /**
     * 单纯文字记录 操作人
     */
    @Test
    public void test1() {
        orderService.query("121231231");
    }


    /**
     * 简单动态文本
     */
    @Test
    public void test3() {
        Order order = new Order();
        order.setOrderId(99L);
        order.setOrderNo("MT0000099");
        order.setProductName("超值优惠红烧肉套餐");
        order.setPurchaseName("张三");
        Order.UserDO userDO = new Order.UserDO();
        userDO.setUserId(9001L);
        userDO.setUserName("用户1");
        order.setCreator(userDO);
        order.setItems(Lists.newArrayList("123", "bbb"));


        Order order1 = new Order();
        order1.setOrderId(88L);
        order1.setOrderNo("MT0000099");
        order1.setProductName("麻辣烫套餐");
        order1.setPurchaseName("赵四");
        Order.UserDO userDO1 = new Order.UserDO();
        userDO1.setUserId(9002L);
        userDO1.setUserName("用户2");
        order1.setCreator(userDO1);
        order1.setItems(Lists.newArrayList("123", "aaa"));
        orderService.diff(order, order1);
    }

    @Test
    public void test4() {
        orderService.query("121231231");
    }

}
