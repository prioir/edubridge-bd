package com.edubridge.controller;

import com.edubridge.entity.Opportunity;
import com.edubridge.entity.StudentDocument;
import com.edubridge.entity.User;
import com.edubridge.repository.StudentDocumentRepository;
import com.edubridge.service.OpportunityService;
import com.edubridge.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentToolsController {
    private final UserService users;
    private final OpportunityService opportunities;
    private final StudentDocumentRepository documents;
    private final com.edubridge.service.AiRecommendationService aiRecommendations;
    private final Path uploadRoot;

    public StudentToolsController(UserService users, OpportunityService opportunities,
                                  StudentDocumentRepository documents,
                                  com.edubridge.service.AiRecommendationService aiRecommendations,
                                  @Value("${app.upload.directory:uploads}") String uploadDirectory) {
        this.users=users; this.opportunities=opportunities; this.documents=documents; this.aiRecommendations=aiRecommendations;
        this.uploadRoot=Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @GetMapping("/recommendations")
    public String recommendations(Principal principal, Model model) {
        User user=users.getByEmail(principal.getName());
        List<Opportunity> all=opportunities.getAllPublished();
        // Use AI ranking only when configured and reachable; otherwise use transparent local ranking.
        Set<String> terms=new HashSet<>();
        for(String text: Arrays.asList(user.getInterests(),user.getDepartment(),user.getStudyLevel(),user.getUniversity())) {
            if(text!=null) Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                    .filter(t->t.length()>2).forEach(terms::add);
        }
        List<Opportunity> matches=all.stream().filter(o->!o.isExpired())
                .sorted(Comparator.<Opportunity>comparingInt(o -> score(o,terms)).reversed()
                        .thenComparing(Opportunity::getDeadline))
                .limit(30).toList();
        boolean aiPowered=false;
        Optional<List<String>> aiOrder=aiRecommendations.rankIds(user,matches);
        if(aiOrder.isPresent()) {
            Map<String,Integer> rank=new HashMap<>(); int i=0; for(String id:aiOrder.get()) rank.put(id,i++);
            matches=matches.stream().sorted(Comparator.comparingInt(o->rank.getOrDefault(o.getId(),Integer.MAX_VALUE))).toList();
            aiPowered=true;
        }
        model.addAttribute("opportunities",matches);
        model.addAttribute("aiPowered",aiPowered);
        model.addAttribute("hasProfileSignals",!terms.isEmpty());
        return "student/recommendations";
    }

    private int score(Opportunity o, Set<String> terms) {
        String text=(o.getTitle()+" "+o.getOrganizationName()+" "+o.getShortDescription()+" "+o.getEligibility()).toLowerCase(Locale.ROOT);
        int score=0; for(String term:terms) if(text.contains(term)) score+=2;
        if(o.isVerified()) score++;
        if(o.getDaysLeft()>=0 && o.getDaysLeft()<=14) score++;
        return score;
    }

    @GetMapping("/calendar")
    public String calendar(Principal principal, Model model) {
        User user=users.getByEmail(principal.getName());
        List<Opportunity> upcoming=users.getBookmarkedOpportunities(user).stream()
                .filter(o->!o.isExpired()).sorted(Comparator.comparing(Opportunity::getDeadline)).toList();
        model.addAttribute("upcoming",upcoming);
        return "student/calendar";
    }

    @GetMapping("/notifications")
    public String notifications(Principal principal, Model model) {
        User user=users.getByEmail(principal.getName());
        LocalDate today=LocalDate.now();
        List<Opportunity> upcoming=users.getBookmarkedOpportunities(user).stream()
                .filter(o->!o.isExpired() && !o.getDeadline().isAfter(today.plusDays(14)))
                .sorted(Comparator.comparing(Opportunity::getDeadline)).toList();
        model.addAttribute("upcoming",upcoming);
        model.addAttribute("emailRemindersEnabled",user.isEmailRemindersEnabled());
        return "student/notifications";
    }

    @PostMapping("/notifications/email")
    public String emailPreference(@RequestParam(defaultValue="false") boolean enabled, Principal principal,
                                  RedirectAttributes flash) {
        User user=users.getByEmail(principal.getName());
        user.setEmailRemindersEnabled(enabled);
        users.save(user);
        flash.addFlashAttribute("successMessage", enabled
                ? "Email reminders enabled. They will send when SMTP is configured." : "Email reminders disabled.");
        return "redirect:/student/notifications";
    }

    @GetMapping("/documents")
    public String documentVault(Principal principal, Model model) {
        User user=users.getByEmail(principal.getName());
        model.addAttribute("documents",documents.findByUserIdOrderByUploadedAtDesc(user.getId()));
        return "student/documents";
    }

    @PostMapping("/documents")
    public String uploadDocument(@RequestParam("file") MultipartFile file, Principal principal,
                                 RedirectAttributes flash) {
        if(file==null || file.isEmpty()) { flash.addFlashAttribute("errorMessage","Choose a document first."); return "redirect:/student/documents"; }
        if(file.getSize()>5L*1024*1024) { flash.addFlashAttribute("errorMessage","Maximum document size is 5 MB."); return "redirect:/student/documents"; }
        String type=Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        String ext;
        if(type.equals("application/pdf")) ext=".pdf";
        else if(type.equals("image/jpeg")) ext=".jpg";
        else if(type.equals("image/png")) ext=".png";
        else { flash.addFlashAttribute("errorMessage","Only PDF, JPG and PNG files are accepted."); return "redirect:/student/documents"; }
        try {
            byte[] header=file.getInputStream().readNBytes(8);
            boolean valid=ext.equals(".pdf") ? header.length>=5 && header[0]=='%' && header[1]=='P' && header[2]=='D' && header[3]=='F' && header[4]=='-' :
                    ext.equals(".jpg") ? header.length>=3 && (header[0]&255)==255 && (header[1]&255)==216 && (header[2]&255)==255 :
                    header.length>=8 && (header[0]&255)==137 && header[1]=='P' && header[2]=='N' && header[3]=='G';
            if(!valid){ flash.addFlashAttribute("errorMessage","The file content does not match its type."); return "redirect:/student/documents"; }
            User user=users.getByEmail(principal.getName());
            Path dir=uploadRoot.resolve("documents").resolve(user.getId()).normalize();
            if(!dir.startsWith(uploadRoot.resolve("documents").normalize())) throw new IOException("Invalid storage path");
            Files.createDirectories(dir);
            String stored=UUID.randomUUID()+ext;
            Files.copy(file.getInputStream(),dir.resolve(stored));
            StudentDocument doc=new StudentDocument(); doc.setUserId(user.getId());
            String original=Optional.ofNullable(file.getOriginalFilename()).orElse("document" ).replaceAll("[\\r\\n\\\\/]+","_");
            doc.setOriginalFilename(original.length()>160?original.substring(original.length()-160):original);
            doc.setStoredFilename(stored); doc.setContentType(type); doc.setSize(file.getSize());
            documents.save(doc); flash.addFlashAttribute("successMessage","Document uploaded to your private vault.");
        } catch(IOException ex) { flash.addFlashAttribute("errorMessage","Could not store the document."); }
        return "redirect:/student/documents";
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id, Principal principal) throws IOException {
        User user=users.getByEmail(principal.getName());
        StudentDocument doc=documents.findByIdAndUserId(id,user.getId()).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        Path file=uploadRoot.resolve("documents").resolve(user.getId()).resolve(doc.getStoredFilename()).normalize();
        if(!file.startsWith(uploadRoot.resolve("documents").resolve(user.getId()).normalize()) || !Files.isRegularFile(file)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        String safe=URLEncoder.encode(doc.getOriginalFilename(),StandardCharsets.UTF_8).replace("+","%20");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+safe)
                .contentLength(Files.size(file)).body(new InputStreamResource(Files.newInputStream(file)));
    }

    @PostMapping("/documents/{id}/delete")
    public String deleteDocument(@PathVariable String id, Principal principal, RedirectAttributes flash) {
        User user=users.getByEmail(principal.getName());
        StudentDocument doc=documents.findByIdAndUserId(id,user.getId()).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        Path file=uploadRoot.resolve("documents").resolve(user.getId()).resolve(doc.getStoredFilename()).normalize();
        try { Files.deleteIfExists(file); } catch(IOException ignored) { }
        documents.delete(doc); flash.addFlashAttribute("successMessage","Document deleted.");
        return "redirect:/student/documents";
    }
}
