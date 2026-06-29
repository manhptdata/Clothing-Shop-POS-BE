package com.sapo.mock.clothing.returnorder.controller;

import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
import com.sapo.mock.clothing.exception.IdInvalidException;
import com.sapo.mock.clothing.returnorder.dto.ReqCreateReturnDTO;
import com.sapo.mock.clothing.returnorder.dto.ResReturnOrderDTO;
import com.sapo.mock.clothing.returnorder.service.ReturnOrderService;
import com.sapo.mock.clothing.util.SecurityUtil;
import com.sapo.mock.clothing.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SALE')")
public class ReturnOrderController {

    private final ReturnOrderService returnOrderService;

    @PostMapping
    @ApiMessage("Tạo mới phiếu trả hàng thành công")
    public ResponseEntity<ResReturnOrderDTO> createReturn(@Valid @RequestBody ReqCreateReturnDTO dto) throws IdInvalidException {
        String username = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Vui lòng đăng nhập"));
        ResReturnOrderDTO newReturn = returnOrderService.createReturn(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(newReturn);
    }

    @GetMapping("/{id}")
    @ApiMessage("Lấy chi tiết phiếu trả hàng thành công")
    public ResponseEntity<ResReturnOrderDTO> getReturnById(@PathVariable Integer id) {
        ResReturnOrderDTO returnOrder = returnOrderService.getReturnById(id);
        return ResponseEntity.ok(returnOrder);
    }

    @GetMapping("/order/{orderId}")
    @ApiMessage("Lấy danh sách phiếu trả hàng của hóa đơn thành công")
    public ResponseEntity<List<ResReturnOrderDTO>> getReturnsByOriginalOrderId(@PathVariable Integer orderId) {
        List<ResReturnOrderDTO> list = returnOrderService.getReturnsByOriginalOrderId(orderId);
        return ResponseEntity.ok(list);
    }

    @GetMapping
    @ApiMessage("Lấy danh sách phiếu trả hàng thành công")
    public ResponseEntity<ResultPaginationDTO> getAllReturns(
            Pageable pageable,
            @RequestParam(required = false) String search) {
        ResultPaginationDTO rs = returnOrderService.getAllReturns(pageable, search);
        return ResponseEntity.ok(rs);
    }
}
