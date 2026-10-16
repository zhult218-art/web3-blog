package com.web3.shop.service;

import com.web3.shop.entity.Order;
import com.web3.shop.mapper.OrderMapper;
import com.web3.shop.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderService 单元测试（2026-10-16 重写，与当前 OrderService API 对齐）。
 *
 * <p>覆盖 updateStatus 状态机路径：
 * <ul>
 *   <li>PENDING → CANCELLED：状态落库 + 库存回补；</li>
 *   <li>PAID → CANCELLED：状态落库 + 库存回补；</li>
 *   <li>非法目标状态：抛异常且不回补库存。</li>
 * </ul>
 * 用 Mockito 桩住 OrderMapper / ProductMapper / ProductService，无需真实 DB。</p>
 */
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Order order(long id, String status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setProductId(2001L);
        order.setQuantity(3);
        return order;
    }

    @Test
    void cancelPendingOrder_shouldRestoreStock() {
        when(orderMapper.selectById(1001L)).thenReturn(order(1001L, "PENDING"));

        orderService.updateStatus(1001L, "CANCELLED");

        // 状态落库（非 PAID 路径走 updateById）
        verify(orderMapper).updateById(any(Order.class));
        // 库存回补被调用（productId=2001, +3）
        verify(productService).restoreStock(eq(2001L), eq(3));
    }

    @Test
    void cancelPaidOrder_shouldRestoreStock() {
        when(orderMapper.selectById(1002L)).thenReturn(order(1002L, "PAID"));

        orderService.updateStatus(1002L, "CANCELLED");

        verify(orderMapper).updateById(any(Order.class));
        verify(productService).restoreStock(eq(2001L), eq(3));
    }

    @Test
    void updateStatus_invalidTargetStatus_throws() {
        assertThrows(RuntimeException.class, () -> orderService.updateStatus(1003L, "INVALID_STATUS"));
        // 非法状态直接拒绝，不应回补库存、不应写库
        verify(productService, never()).restoreStock(anyLong(), anyInt());
        verify(orderMapper, never()).updateById(any(Order.class));
    }
}
