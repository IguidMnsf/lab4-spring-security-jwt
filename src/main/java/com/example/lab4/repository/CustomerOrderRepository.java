package com.example.lab4.repository;

import com.example.lab4.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findAllByOwnerUsernameOrderByOrderDateDesc(String username);

    Optional<CustomerOrder> findByIdAndOwnerUsername(Long id, String username);
}
