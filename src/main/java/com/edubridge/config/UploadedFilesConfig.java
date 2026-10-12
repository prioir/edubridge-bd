package com.edubridge.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;
@Configuration public class UploadedFilesConfig implements WebMvcConfigurer {
 @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
  String profileLocation=Paths.get("uploads","profile").toAbsolutePath().normalize().toUri().toString();
  if (!profileLocation.endsWith("/")) profileLocation += "/";
  registry.addResourceHandler("/uploads/profile/**").addResourceLocations(profileLocation);
 }
}
