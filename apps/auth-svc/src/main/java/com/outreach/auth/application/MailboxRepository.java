package com.outreach.auth.application;

import com.outreach.auth.domain.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailboxRepository extends JpaRepository<Mailbox, Long> {
    List<Mailbox> findByUserId(Long userId);
}
