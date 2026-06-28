package com.sapo.mock.clothing.customer.service.file;

import com.sapo.mock.clothing.customer.repository.CustomerRepository;
import com.sapo.mock.clothing.entity.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerFileService {

    private final CustomerRepository customerRepository;

    @Transactional // QUAN TRỌNG: Đảm bảo rollback nếu có lỗi xảy ra giữa chừng
    public void saveCustomersFromExcel(MultipartFile file) {
        try {
            // 1. Parse dữ liệu từ file
            List<Customer> excelCustomers = ExcelHelper.excelToCustomers(file.getInputStream());

            if (excelCustomers.isEmpty()) {
                throw new RuntimeException("File Excel không có dữ liệu khách hàng hợp lệ.");
            }

            // 2. Lấy danh sách số điện thoại từ Excel
            List<String> phonesInExcel = excelCustomers.stream()
                    .map(Customer::getPhone)
                    .filter(phone -> phone != null && !phone.isEmpty())
                    .collect(Collectors.toList());

            // 3. Truy vấn DB xem các số điện thoại này đã tồn tại chưa
            // Ở đây lọc ra danh sách khách hàng CHƯA tồn tại trong hệ thống (dựa theo SĐT)
            List<Customer> validCustomersToSave = excelCustomers.stream()
                    .filter(c -> {
                        if (c.getPhone() == null || c.getPhone().isEmpty()) return false; // Bỏ qua nếu ko có SĐT
                        return !customerRepository.existsByPhone(c.getPhone()); 
                    })
                    .collect(Collectors.toList());

            if (validCustomersToSave.isEmpty()) {
                throw new RuntimeException("Tất cả khách hàng trong file Excel đã tồn tại trong hệ thống (trùng Số điện thoại).");
            }

            // 4. Chỉ lưu những khách hàng chưa tồn tại
            customerRepository.saveAll(validCustomersToSave);

        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc dữ liệu file Excel: " + e.getMessage());
        }
    }
}