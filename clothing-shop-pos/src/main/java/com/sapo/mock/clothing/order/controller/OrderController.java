package com.sapo.mock.clothing.order.controller;

import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.order.dto.ResOrderDTO;
import com.sapo.mock.clothing.exception.IdInvalidException;
import com.sapo.mock.clothing.order.service.OrderService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import com.sapo.mock.clothing.util.constant.OrderStatus;
import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_ORDER', 'CREATE_ORDER')")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ApiMessage("Tạo mới đơn hàng thành công")
    public ResponseEntity<ResOrderDTO> createOrder(@Valid @RequestBody ReqCreateOrderDTO dto)
            throws IdInvalidException {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Vui lòng đăng nhập"));
        ResOrderDTO newOrder = orderService.createOrder(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy chi tiết đơn hàng thành công")
    public ResponseEntity<ResOrderDTO> getOrderById(@PathVariable Integer id) {
        ResOrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    @ApiMessage("Lấy danh sách đơn hàng thành công")
    public ResponseEntity<ResultPaginationDTO> getAllOrders(
            Pageable pageable,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search) {
        ResultPaginationDTO rs = orderService.getAllOrders(pageable, status, search);
        return ResponseEntity.ok(rs);
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật đơn hàng thành công")
    public ResponseEntity<ResOrderDTO> updateOrder(
            @PathVariable Integer id,
            @Valid @RequestBody ReqCreateOrderDTO dto) throws IdInvalidException {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Vui lòng đăng nhập"));
        ResOrderDTO updatedOrder = orderService.updateOrder(id, dto, username);
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/{id}/cancel")
    @ApiMessage("Hủy đơn hàng thành công")
    public ResponseEntity<ResOrderDTO> cancelOrder(@PathVariable Integer id) {
        ResOrderDTO canceledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(canceledOrder);
    }

    @PatchMapping("/{id}/mark-printed")
    @ApiMessage("Đánh dấu đơn hàng đã in thành công")
    public ResponseEntity<ResOrderDTO> markPrinted(@PathVariable Integer id) {
        ResOrderDTO updatedOrder = orderService.updatePrintStatus(id, true);
        return ResponseEntity.ok(updatedOrder);
    }
}
