package com.sapo.mock.clothing.customer.service.file;


import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import com.sapo.mock.clothing.util.constant.GenderEnum;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {

    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // Kiểm tra định dạng file gửi lên có phải là Excel (.xlsx) không
    public static boolean hasExcelFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType());
    }

    public static List<Customer> excelToCustomers(InputStream is) {
        try {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên
            Iterator<Row> rows = sheet.iterator();

            List<Customer> customers = new ArrayList<>();
            int rowNumber = 0;

            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // Bỏ qua dòng tiêu đề
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Customer customer = new Customer();
                DataFormatter formatter = new DataFormatter();

                // Cột 0 (A): Họ và Tên (Bắt buộc)
                Cell cell0 = currentRow.getCell(0);
                if (cell0 != null) {
                    String fullName = formatter.formatCellValue(cell0).trim();
                    if (fullName.isEmpty()) continue; // Bỏ qua nếu dòng không có tên
                    customer.setFullName(fullName);
                } else {
                    continue; // Bỏ qua dòng trống
                }

                // Cột 1 (B): Số điện thoại (Bắt buộc)
                Cell cell1 = currentRow.getCell(1);
                if (cell1 != null) {
                    String phoneStr = formatter.formatCellValue(cell1).trim();

                    // VALIDATE: Nếu số điện thoại quá dài (vượt quá 15 ký tự) hoặc trống, bỏ qua dòng này để tránh crash DB
                    if (phoneStr.isEmpty() || phoneStr.length() > 15) {
                        continue;
                    }

                    customer.setPhone(phoneStr);
                } else {
                    continue; // Không có SĐT thì bỏ qua dòng
                }
                // Cột 2 (C): Email
                Cell cell2 = currentRow.getCell(2);
                if (cell2 != null) {
                    customer.setEmail(formatter.formatCellValue(cell2).trim());
                }

                // Cột 3 (D): Ngày sinh (Hỗ trợ định dạng Date trong Excel hoặc Text yyyy-MM-dd)
                Cell cell3 = currentRow.getCell(3);
                if (cell3 != null) {
                    if (cell3.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell3)) {
                        LocalDate birthDate = cell3.getDateCellValue().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDate();
                        customer.setDateOfBirth(birthDate);
                    } else {
                        String dateStr = formatter.formatCellValue(cell3).trim();
                        if (!dateStr.isEmpty()) {
                            try {
                                customer.setDateOfBirth(LocalDate.parse(dateStr)); // Format chuẩn: yyyy-MM-dd
                            } catch (Exception e) {
                                // Bỏ qua nếu lỗi format ngày
                            }
                        }
                    }
                }

                // Cột 4 (E): Giới tính (MALE, FEMALE, OTHER)
                Cell cell4 = currentRow.getCell(4);
                if (cell4 != null) {
                    try {
                        customer.setGender(GenderEnum.valueOf(formatter.formatCellValue(cell4).trim().toUpperCase()));
                    } catch (Exception e) {
                        customer.setGender(GenderEnum.OTHER); // Mặc định nếu nhập sai
                    }
                }

                // Cột 5 (F): Địa chỉ
                Cell cell5 = currentRow.getCell(5);
                if (cell5 != null) {
                    customer.setAddress(formatter.formatCellValue(cell5).trim());
                }

                // Cột 6 (G): Ghi chú
                Cell cell6 = currentRow.getCell(6);
                if (cell6 != null) {
                    customer.setNote(formatter.formatCellValue(cell6).trim());
                }

                // Các trường mặc định cho khách hàng mới
                customer.setStatus(CustomerStatusEnum.ACTIVE);
                customer.setRewardPoints(0);
                customer.setTotalSpent(java.math.BigDecimal.ZERO);

                customers.add(customer);
            }

            workbook.close();
            return customers;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc và parse file Excel: " + e.getMessage());
        }
    }
}