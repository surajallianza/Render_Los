package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long> {
    List<CustomerDocument> findByCustomerId(Long customerId);
    List<CustomerDocument> findByCustomerIdAndDocumentTypeLookupIdAndIsDeletedFalse(Long customerId, Long documentTypeLookupId);

    List<CustomerDocument> findByCustomerIdAndIsDeletedFalseOrderByUploadedAtDesc(Long customerId);

    Optional<CustomerDocument> findByIdAndCustomerIdAndIsDeletedFalse(Long id, Long customerId);

    Optional<CustomerDocument> findByIdAndIsDeletedFalse(Long id);
}
