package com.insurmatch.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Tên thành viên không được để trống")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Builder.Default
    private String role = "agent";

    private String phone;

    private String npn;

    private String department;

    /**
     * Nhận mảng string từ FE: ["TX (TDI)", "CA (CDI)"] hoặc chuỗi text
     */
    private Object statesLicensed;
}
