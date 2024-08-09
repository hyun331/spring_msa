package com.beyond.order_system.ordering.service;

import com.beyond.order_system.common.service.StockInventoryService;

import com.beyond.order_system.ordering.controller.SseController;
import com.beyond.order_system.ordering.domain.OrderDetail;
import com.beyond.order_system.ordering.domain.OrderStatus;
import com.beyond.order_system.ordering.domain.Ordering;
import com.beyond.order_system.ordering.dto.OrderingReqDto;
import com.beyond.order_system.ordering.dto.OrderListResDto;
import com.beyond.order_system.ordering.dto.StockDecreaseEvent;
import com.beyond.order_system.ordering.repository.OrderDetailRepository;
import com.beyond.order_system.ordering.repository.OrderingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StockInventoryService stockInventoryService;
    private final StockDecreaseEventHandler stockDecreaseEventHandler;
    private final SseController sseController;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, OrderDetailRepository orderDetailRepository, StockInventoryService stockInventoryService, StockDecreaseEventHandler stockDecreaseEventHandler, SseController sseController) {
        this.orderingRepository = orderingRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.stockInventoryService = stockInventoryService;
        this.stockDecreaseEventHandler = stockDecreaseEventHandler;
        this.sseController = sseController;
    }

    public Ordering orderingCreate(List<OrderingReqDto> dtos) {
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Ordering ordering = Ordering.builder()
                .memberEmail(memberEmail)
                .build();

//        for(OrderingReqDto dto : dtos){
//            //product api의 요청을 통해 product 객체를 조회해야함
//            if(product.getName().contains("sale")){
//                int newQuantity = stockInventoryService.decreaseStock(product.getId(), dto.getProductCount()).intValue();
//                if(newQuantity<0){
//                    throw new IllegalArgumentException("재고 부족");
//                }
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(product.getId(), dto.getProductCount()));
//            }
//            else{
//                //재고 수 확인
//                if(dto.getProductCount()>product.getStockQuantity()){
//                    throw new IllegalArgumentException(product.getName()+" 재고 부족");
//                }
//                product.updateStockQunatity(dto.getProductCount());
//            }
//
//            int quantity = dto.getProductCount();
//            OrderDetail orderDetail =  OrderDetail.builder()
//                    .product(product)
//                    .quantity(quantity)
//                    .ordering(ordering)
//                    .build();
//            ordering.getOrderDetails().add(orderDetail);
//        }

        Ordering savedOrdering = orderingRepository.save(ordering);
        sseController.publishMessage(savedOrdering.fromEntity(), "admin@naver.com");
        return savedOrdering;
    }

    public List<OrderListResDto> orderList() {
        List<Ordering> orderingList = orderingRepository.findAll(); //주문 리스트
        List<OrderListResDto> orderListResDtoList = new ArrayList<>();
        for(Ordering ordering : orderingList){  //각 주문마다
            orderListResDtoList.add(ordering.fromEntity());
        }
        return orderListResDtoList;
    }

    public List<OrderListResDto> myOrderList() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Ordering> orderingList = orderingRepository.findAllByMemberEmail(email);
        List<OrderListResDto> orderListResDtoList = new ArrayList<>();
        for(Ordering ordering : orderingList){
            orderListResDtoList.add(ordering.fromEntity());
        }
        return orderListResDtoList;
    }

    public Ordering cancelOrder(Long id) {
        Ordering ordering = orderingRepository.findById(id).orElseThrow(()->new EntityNotFoundException("order not found"));
        ordering.updateStatus(OrderStatus.CANCLED);
        return ordering;
    }
}
