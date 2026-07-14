package com.sapo.mock.clothing.receipt.controller;

import com.sapo.mock.clothing.common.dto.response.ResultPaginationDTO;
import com.sapo.mock.clothing.entity.StockLog;
import com.sapo.mock.clothing.receipt.DTO.StockLogResponse;
import com.sapo.mock.clothing.receipt.repository.StockLogRepository;
import com.sapo.mock.clothing.util.constant.StockLogReferenceType;
import com.sapo.mock.clothing.util.constant.StockLogSource;
import com.sapo.mock.clothing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/stock-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_RECEIPT', 'MANAGE_RECEIPT')")
public class StockLogController {

    private final StockLogRepository stockLogRepository;
    private final UserRepository userRepository;

    /**
     * GET /api/stock-logs
     * Lấy lịch sử biến động kho toàn bộ (bán hàng + nhập hàng + trả hàng + hủy đơn)
     * Filter theo: source, variantId, referenceType, from, to
     */
    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAllStockLogs(
            @RequestParam(required = false) StockLogSource source,
            @RequestParam(required = false) Integer variantId,
            @RequestParam(required = false) StockLogReferenceType referenceType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable) {

        // Xây dựng Specification động
        Specification<StockLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.conjunction());

            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (variantId != null) {
                predicates.add(cb.equal(root.get("variant").get("id"), variantId));
            }
            if (referenceType != null) {
                predicates.add(cb.equal(root.get("referenceType"), referenceType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockLog> page = stockLogRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());
        rs.setMeta(meta);

        List<StockLogResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        rs.setResult(content);

        return ResponseEntity.ok(rs);
    }

    /**
     * GET /api/stock-logs/variant/{variantId}
     * Lấy toàn bộ lịch sử biến động của 1 SKU cụ thể
     */
    @GetMapping("/variant/{variantId}")
    public ResponseEntity<ResultPaginationDTO> getLogsByVariant(
            @PathVariable Integer variantId,
            Pageable pageable) {

        Page<StockLog> page = stockLogRepository.findByVariantIdOrderByCreatedAtDesc(variantId, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());
        rs.setMeta(meta);
        rs.setResult(page.getContent().stream().map(this::mapToResponse).toList());

        return ResponseEntity.ok(rs);
    }

    private StockLogResponse mapToResponse(StockLog log) {
        String productName = null;
        String variantSku = null;
        String createdBy = null;

        if (log.getVariant() != null) {
            try {
                variantSku = log.getVariant().getSku();
                if (log.getVariant().getProduct() != null) {
                    productName = log.getVariant().getProduct().getName();

                    // Ghép option values vào tên
                    List<String> opts = new ArrayList<>();
                    if (log.getVariant().getOption1Value() != null)
                        opts.add(log.getVariant().getOption1Value().getValue());
                    if (log.getVariant().getOption2Value() != null)
                        opts.add(log.getVariant().getOption2Value().getValue());
                    if (log.getVariant().getOption3Value() != null)
                        opts.add(log.getVariant().getOption3Value().getValue());
                    if (!opts.isEmpty())
                        productName += " (" + String.join(" - ", opts) + ")";
                }
            } catch (jakarta.persistence.EntityNotFoundException e) {
                productName = "Sản phẩm đã bị xóa";
            } catch (RuntimeException e) {
                // Fallback for any other proxy initialization errors
                productName = "Sản phẩm không khả dụng";
            }
        }

        if (log.getCreatedBy() != null) {
            try {
                String fullName = log.getCreatedBy().getFullName();
                createdBy = fullName != null ? fullName : log.getCreatedBy().getUsername();
            } catch (jakarta.persistence.EntityNotFoundException e) {
                createdBy = "Người dùng đã xóa";
            } catch (RuntimeException e) {
                createdBy = "Người dùng không khả dụng";
            }
        }

        return StockLogResponse.builder()
                .id(log.getId())
                .variantId(log.getVariant() != null ? log.getVariant().getId() : null)
                .variantSku(variantSku)
                .productName(productName)
                .quantityBefore(log.getQuantityBefore())
                .quantityChange(log.getQuantityChange())
                .quantityAfter(log.getQuantityAfter())
                .source(log.getSource())
                .referenceType(log.getReferenceType())
                .referenceId(log.getReferenceId())
                .note(log.getNote())
                .createdAt(log.getCreatedAt())
                .createdByUsername(createdBy)
                .build();
    }
}
