package com.sapo.mock.clothing.order.controller;

import com.sapo.mock.clothing.order.dto.ReqCreateOrderDTO;
import com.sapo.mock.clothing.order.dto.ResOrderDTO;
import com.sapo.mock.clothing.exception.IdInvalidException;
import com.sapo.mock.clothing.order.service.OrderService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ApiMessage("Tạo mới đơn hàng thành công")
    public ResponseEntity<ResOrderDTO> createOrder(@Valid @RequestBody ReqCreateOrderDTO dto) throws IdInvalidException {
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
    public ResponseEntity<ResultPaginationDTO> getAllOrders(Pageable pageable) {
        ResultPaginationDTO rs = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(rs);
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
