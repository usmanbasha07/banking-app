package com.ub.banking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ub.banking.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long>{
	
}
