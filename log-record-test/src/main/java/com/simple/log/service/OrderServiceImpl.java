package com.simple.log.service;


import com.simple.log.annoation.LogRecord;
import com.simple.log.model.Order;

import org.springframework.stereotype.Service;

/**
 * @author fdrama
 * @date 2022年08月30日 17:22
 */
@Service
public class OrderServiceImpl implements OrderService {


    @Override
    public Order query(String orderId) {
        return null;
    }

    @Override
    public Order update(Order orderBO) {
        return null;
    }

    @Override
    public Order add(Order orderBO) {
        return null;
    }

    @Override
    public boolean delete(String orderId) {
        return false;
    }

    @Override
    @LogRecord(success = "更新了订单{_DIFF{#oldOrder, #newOrder}}",
            type = "ORDER", businessId = "{{#newOrder.orderNo}}",
            extra = "{{#newOrder.toString()}}")
    public void diff(Order oldOrder, Order newOrder) {

    }

}
