package com.ub.banking.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ub.banking.dto.AccountDto;
import com.ub.banking.entity.Account;
import com.ub.banking.exception.AccountNotFound;
import com.ub.banking.mapper.AccountMapper;
import com.ub.banking.repository.AccountRepository;
import com.ub.banking.service.AccountService;


@Service
public class AccountServiceImpl implements AccountService{

	@Autowired
	private AccountRepository accountRepository;
	
	@Override
	public AccountDto createAccount(AccountDto accountDto) {
		
		Account account =AccountMapper.mapToAccount(accountDto);
		Account saveAccount=accountRepository.save(account);
		return AccountMapper.mapToAccountDto(saveAccount);
	}

	@Override
	public AccountDto getAccountById(Long id) {
		Account account= accountRepository.findById(id).orElseThrow(()->new AccountNotFound(String.format("Account %d does not exist",id)));
		return AccountMapper.mapToAccountDto(account);
	}

	@Override
	public AccountDto deposite(Long id, double amount) {
		Account account= accountRepository.findById(id).orElseThrow(()->new RuntimeException("Account does not exist"));
		
		double total=account.getBalance()+amount;
		account.setBalance(total);
		Account saveAccount=accountRepository.save(account);
		
		return AccountMapper.mapToAccountDto(saveAccount);
	}

	@Override
	public AccountDto withdraw(Long id, double amount) {
		Account account= accountRepository.findById(id).orElseThrow(()->new RuntimeException("Account does not exist"));
		
		if(account.getBalance()<amount) {
			throw new RuntimeException("Insufficent blance");
		}
		double total=account.getBalance()-amount;
		account.setBalance(total);
		Account saveAccount=accountRepository.save(account);
		
		return AccountMapper.mapToAccountDto(saveAccount);
	
	}

	@Override
	public List<AccountDto> getAllAccounts() {
		List<Account> accounts= accountRepository.findAll();
		return accounts.stream().map(
				(account)->AccountMapper.mapToAccountDto(account)
				).collect(Collectors.toList());
	}

	@Override
	public void deleteAccount(Long id) {
		Account account= accountRepository.findById(id).orElseThrow(()->new RuntimeException("Account does not exist"));
	
		accountRepository.deleteById(id);
	}

	
	
}
