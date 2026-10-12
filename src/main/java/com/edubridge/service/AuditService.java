package com.edubridge.service;
import com.edubridge.entity.AuditEvent;
import com.edubridge.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import java.util.List;
@Service public class AuditService { private final AuditEventRepository repository; public AuditService(AuditEventRepository repository){this.repository=repository;} public void record(String actor,String action,String target,String details){repository.save(new AuditEvent(actor,action,target,details));} public List<AuditEvent> latest(){return repository.findTop100ByOrderByCreatedAtDesc();} }
