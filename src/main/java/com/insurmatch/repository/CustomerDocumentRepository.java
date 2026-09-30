package com.insurmatch.repository;

import com.insurmatch.entity.CustomerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long> {
    Optional<CustomerDocument> findByCode(String code);

    @Query("SELECT d FROM CustomerDocument d WHERE d.contact.id = :contactId")
    List<CustomerDocument> findByContactId(@Param("contactId") Long contactId);

    List<CustomerDocument> findByContactOwnerContainingIgnoreCase(String owner);

    @Query("SELECT d FROM CustomerDocument d WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.contactOwner) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(d.code IS NOT NULL AND LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<CustomerDocument> searchDocuments(@Param("search") String search);
}
