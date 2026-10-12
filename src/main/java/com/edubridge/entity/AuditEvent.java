package com.edubridge.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection="audit_events")
@Getter @Setter @NoArgsConstructor
public class AuditEvent {
 @Id private String id;
 @Indexed private String actorEmail;
 private String action;
 private String targetId;
 private String details;
 private LocalDateTime createdAt=LocalDateTime.now();
 public AuditEvent(String actorEmail,String action,String targetId,String details){this.actorEmail=actorEmail;this.action=action;this.targetId=targetId;this.details=details;this.createdAt=LocalDateTime.now();}
}
