package com.insurmatch.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * FlexibleLocalDateDeserializer — Hỗ trợ deserialize ngày tháng linh hoạt từ Frontend:
 * - "11/12/1984" (MM/dd/yyyy)
 * - "1984-11-12" (yyyy-MM-dd)
 * - "11-12-1984" (MM-dd-yyyy)
 * - "" hoặc whitespace -> null (không throw lỗi khi user xóa ô ngày)
 */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_DATE,                    // yyyy-MM-dd
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),           // MM/dd/yyyy
            DateTimeFormatter.ofPattern("M/d/yyyy"),             // M/d/yyyy
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),           // yyyy/MM/dd
            DateTimeFormatter.ofPattern("MM-dd-yyyy")            // MM-dd-yyyy
    );

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // Thử format tiếp theo
            }
        }

        return null;
    }
}
