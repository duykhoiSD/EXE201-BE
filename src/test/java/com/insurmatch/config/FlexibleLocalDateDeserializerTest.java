package com.insurmatch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurmatch.dto.ContactDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FlexibleLocalDateDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("Should parse US date format MM/dd/yyyy (e.g. 11/12/1984)")
    void shouldParseUsDateFormat() throws Exception {
        String json = "{\"dateOfBirth\":\"11/12/1984\"}";
        ContactDTO dto = objectMapper.readValue(json, ContactDTO.class);
        assertNotNull(dto.getDateOfBirth());
        assertEquals(LocalDate.of(1984, 11, 12), dto.getDateOfBirth());
    }

    @Test
    @DisplayName("Should parse ISO date format yyyy-MM-dd (e.g. 1984-11-12)")
    void shouldParseIsoDateFormat() throws Exception {
        String json = "{\"dateOfBirth\":\"1984-11-12\"}";
        ContactDTO dto = objectMapper.readValue(json, ContactDTO.class);
        assertNotNull(dto.getDateOfBirth());
        assertEquals(LocalDate.of(1984, 11, 12), dto.getDateOfBirth());
    }

    @Test
    @DisplayName("Should handle 'dob' alias from FE")
    void shouldHandleDobAlias() throws Exception {
        String json = "{\"dob\":\"05/10/1988\"}";
        ContactDTO dto = objectMapper.readValue(json, ContactDTO.class);
        assertNotNull(dto.getDateOfBirth());
        assertEquals(LocalDate.of(1988, 5, 10), dto.getDateOfBirth());
    }

    @Test
    @DisplayName("Should gracefully handle empty or blank string as null")
    void shouldHandleEmptyOrBlankDateAsNull() throws Exception {
        String jsonEmpty = "{\"dateOfBirth\":\"\"}";
        ContactDTO dtoEmpty = objectMapper.readValue(jsonEmpty, ContactDTO.class);
        assertNull(dtoEmpty.getDateOfBirth());

        String jsonSpaces = "{\"dateOfBirth\":\"   \"}";
        ContactDTO dtoSpaces = objectMapper.readValue(jsonSpaces, ContactDTO.class);
        assertNull(dtoSpaces.getDateOfBirth());
    }

    @Test
    @DisplayName("Should parse dateExpired with US format")
    void shouldParseDateExpired() throws Exception {
        String json = "{\"dateExpired\":\"10/20/2035\"}";
        ContactDTO dto = objectMapper.readValue(json, ContactDTO.class);
        assertNotNull(dto.getDateExpired());
        assertEquals(LocalDate.of(2035, 10, 20), dto.getDateExpired());
    }
}
