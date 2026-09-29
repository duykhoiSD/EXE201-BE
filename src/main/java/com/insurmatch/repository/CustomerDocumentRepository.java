package com.insurmatch.repository;

import com.insurmatch.entity.CustomerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long> {
    @Query("SELECT d FROM CustomerDocument d WHERE d.contact.id = :contactId")
    List<CustomerDocument> findByContactId(@Param("contactId") Long contactId);

    List<CustomerDocument> findByContactOwnerContainingIgnoreCase(String owner);
}
