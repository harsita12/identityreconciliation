package com.example.demo.repository;

import com.example.demo.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByEmail(String email);
    List<Contact> findByPhoneNumber(String phoneNumber);
    List<Contact> findByLinkedId(Long linkedId);
}
