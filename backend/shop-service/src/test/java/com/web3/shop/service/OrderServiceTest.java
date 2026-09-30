package com.web3.shop.service;

import com.web3.shop.entity.Order;
import com.web3.shop.mapper.OrderMapper;
import com.web3.shop.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OrderService 单元测试样板（2026-09-17 新增）。
 *
 * <p>覆盖 updateStatus 方法中"取消订单自动回补库存"路径，
 * 验证 {@code @Transactional} 修复后回补逻辑确实被调用。
 * 用 Mockito 桩住 OrderMapper / ProductMapper，无需真实 DB。</p>
 *
 * <p>注：仓库后端历史零测试，此文件作为样板，CI 当前 -DskipTests 不跑；
 * 后续可补 OrderService 的更多分支（PAID→SHIPPED、SHIPPED→COMPLETED 等）。</p>
 */
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void cancelPaidOrder_shouldRestoreStock() {
        // 真实 Order 结构：单商品订单，productId=2001, quantity=3
        Order order = new Order();
        order.setId(1001L);
        order.setStatus("PAID");
        order.setProductId(2001L);
        order.setQuantity(3);
        when(orderMapper.selectById(1001L)).thenReturn(order);
        when(orderMapper.updateStatus(eq(1001L), eq("CANCELLED"))).thenReturn(1);

        orderService.updateStatus(1001L, "CANCELLED");

        // 验证状态被改为 CANCELLED
        verify(orderMapper).updateStatus(1001L, "CANCELLED");
        // 验证库存回补被调用（productId=2001, +3）
        verify(productMapper).restoreStock(2001L, 3);
    }

    @Test
    void updateStatus_invalidTargetStatus_throws() {
        assertThrows(RuntimeException.class, () -> orderService.updateStatus(1002L, "INVALID_STATUS"));
        // 不应回补库存
        verify(productMapper, never()).restoreStock(anyLong(), anyInt());
    }
}
