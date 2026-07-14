package com.sapo.mock.clothing.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sapo.mock.clothing.common.dto.response.RestResponse;

/**
 * Xử lý tập trung tất cả exception trong hệ thống, trả về format RestResponse
 * chuẩn.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Xử lý lỗi validate input từ @Valid — trả về danh sách các field lỗi.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<RestResponse<Object>> handleValidationException(MethodArgumentNotValidException exception) {

		BindingResult bindingResult = exception.getBindingResult();
		List<String> errorMessages = bindingResult.getAllErrors().stream().map(error -> {
			if (error instanceof FieldError fieldError) {
				return fieldError.getField() + ": " + fieldError.getDefaultMessage();
			}
			return error.getDefaultMessage();
		}).collect(Collectors.toList());

		RestResponse<Object> response = new RestResponse<>();
		response.setStatusCode(HttpStatus.BAD_REQUEST.value());
		response.setError("Dữ liệu đầu vào không hợp lệ");
		response.setMessage(errorMessages.size() == 1 ? errorMessages.get(0) : errorMessages);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * Xử lý lỗi thiếu hoặc sai định dạng request body (thiếu dữ liệu gửi lên)
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<RestResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
		RestResponse<Object> response = new RestResponse<>();
		response.setStatusCode(HttpStatus.BAD_REQUEST.value());
		response.setError("Dữ liệu gửi lên không hợp lệ hoặc bị trống");
		response.setMessage("Hệ thống không thể đọc được dữ liệu. Vui lòng kiểm tra lại cấu trúc dữ liệu gửi lên.");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * Xử lý lỗi đăng nhập sai thông tin (email/password không đúng).
	 */
	@ExceptionHandler({ BadCredentialsException.class, UsernameNotFoundException.class })
	public ResponseEntity<RestResponse<Object>> handleBadCredentials(RuntimeException exception) {
		RestResponse<Object> response = new RestResponse<>();
		response.setStatusCode(HttpStatus.UNAUTHORIZED.value());
		response.setError("Sai thông tin đăng nhập");
		response.setMessage(exception.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	/**
	 * Xử lý lỗi ID không hợp lệ (không tìm thấy entity theo ID).
	 */
	@ExceptionHandler(IdInvalidException.class)
	public ResponseEntity<RestResponse<Object>> handleIdInvalidException(IdInvalidException exception) {
		RestResponse<Object> response = new RestResponse<>();
		response.setStatusCode(HttpStatus.BAD_REQUEST.value());
		response.setError("ID không hợp lệ");
		response.setMessage(exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * Xử lý lỗi không tìm thấy resource.
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<RestResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException exception) {
		RestResponse<Object> response = new RestResponse<>();
		response.setStatusCode(HttpStatus.NOT_FOUND.value());
		response.setError("Không tìm thấy dữ liệu");
		response.setMessage(exception.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	/**
	 * Xử lý lỗi không có quyền truy cập.
	 */
	@ExceptionHandler(PermissionException.class)
	public ResponseEntity<RestResponse<Object>> handlePermissionException(PermissionException exception) {
		RestResponse<Object> response = new RestResponse<>();
		response.setStatusCode(HttpStatus.FORBIDDEN.value());
		response.setError("Không có quyền truy cập");
		response.setMessage(exception.getMessage());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
	}

	/**
	 * Xử lý lỗi nghiệp vụ Client gửi yêu cầu sai trạng thái logic.
	 */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<RestResponse<Object>> handleBadRequestException(BadRequestException exception) {
		RestResponse<Object> response = new RestResponse<>();
		response.setStatusCode(HttpStatus.BAD_REQUEST.value());
		response.setError("Yêu cầu không hợp lệ");
		response.setMessage(exception.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	/**
	 * Xử lý các lỗi runtime không mong đợi còn lại.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<RestResponse<Object>> handleGenericException(Exception exception) {
		RestResponse<Object> response = new RestResponse<>();
		response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		response.setError("Lỗi hệ thống");
		response.setMessage(exception.getMessage());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	/**
	 * Xử lý lỗi tranh chấp dữ liệu (Optimistic Locking) khi nhiều người thao tác cùng lúc
	 */
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<RestResponse<Object>> handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException exception) {
		RestResponse<Object> response = new RestResponse<>();
		response.setStatusCode(HttpStatus.CONFLICT.value());
		response.setError("Dữ liệu đã bị thay đổi");
		response.setMessage("Dữ liệu này vừa được cập nhật bởi một người khác. Vui lòng tải lại trang và thao tác lại!");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

    /**
     * crm
     * Xử lý riêng các lỗi Logic nghiệp vụ (Nhập sai ID, trùng SĐT...)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RestResponse<Object>> handleRuntimeException(RuntimeException exception) {
        RestResponse<Object> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.BAD_REQUEST.value()); // Mã 400: Yêu cầu không hợp lệ
        response.setError("Lỗi xử lý nghiệp vụ");
        response.setMessage(exception.getMessage()); // Trả về câu "Không tìm thấy khách hàng..."

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
