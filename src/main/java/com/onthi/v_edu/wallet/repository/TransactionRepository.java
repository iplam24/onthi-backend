package com.onthi.v_edu.wallet.repository;

import com.onthi.v_edu.wallet.entity.Transaction;
import com.onthi.v_edu.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findByOrderCode(Long orderCode);
    Page<Transaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
