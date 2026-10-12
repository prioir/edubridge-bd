package com.edubridge.service;

import com.edubridge.entity.Opportunity;
import com.edubridge.entity.User;
import com.edubridge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class DeadlineReminderService {
 private static final Logger log=LoggerFactory.getLogger(DeadlineReminderService.class);
 private final UserRepository users; private final OpportunityService opportunities; private final JavaMailSender mail; private final String from;
 public DeadlineReminderService(UserRepository users, OpportunityService opportunities, JavaMailSender mail, @Value("${app.mail.from:no-reply@edubridge.local}") String from){this.users=users;this.opportunities=opportunities;this.mail=mail;this.from=from;}
 @Scheduled(cron="0 0 9 * * *")
 public void sendTomorrowDeadlineReminders(){
  LocalDate tomorrow=LocalDate.now().plusDays(1);
  for(User user:users.findAll()){
   if(!user.isEmailRemindersEnabled() || user.getBookmarkIds()==null || user.getBookmarkIds().isEmpty()) continue;
   for(Opportunity o:opportunities.findPublishedByIds(user.getBookmarkIds())){
    if(!tomorrow.equals(o.getDeadline())) continue;
    String key=o.getId()+":"+tomorrow; if(user.getSentReminderKeys()!=null && user.getSentReminderKeys().contains(key)) continue;
    try { SimpleMailMessage msg=new SimpleMailMessage(); msg.setFrom(from); msg.setTo(user.getEmail()); msg.setSubject("EduBridge BD reminder: deadline tomorrow"); msg.setText("Hello "+user.getFullName()+",\n\nThe deadline for your saved opportunity is tomorrow: "+o.getTitle()+" ("+o.getOrganizationName()+").\nPlease review the official application page in EduBridge BD.\n\nEduBridge BD"); mail.send(msg);
     if(user.getSentReminderKeys()==null) user.setSentReminderKeys(new java.util.LinkedHashSet<>()); user.getSentReminderKeys().add(key); users.save(user);
    } catch(MailException ex){ log.warn("Could not send deadline reminder to {}. Check SMTP configuration.",user.getEmail()); }
   }
  }
 }
}
