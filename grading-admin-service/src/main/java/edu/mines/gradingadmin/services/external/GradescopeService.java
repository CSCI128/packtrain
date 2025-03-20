package edu.mines.gradingadmin.services.external;

import edu.mines.gradingadmin.config.ExternalServiceConfig;
import edu.mines.gradingadmin.managers.ImpersonationManager;
import edu.mines.gradingadmin.models.CredentialType;
import edu.mines.gradingadmin.models.User;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GradescopeService {
    private final ExternalServiceConfig.GradescopeConfig config;
    private final ImpersonationManager impersonationManager;

    private RestClient client;



    public GradescopeService(ExternalServiceConfig.GradescopeConfig config, ImpersonationManager impersonationManager) {
        this.config = config;
        this.impersonationManager = impersonationManager;

        if (!this.config.isEnabled()){
            log.warn("Gradescope is disabled!");
            return;
        }

        client = RestClient.builder().baseUrl(config.getUri()).build();
    }

    private Map<String, HttpCookie> login(ImpersonationManager.ImpersonatedUserProvider provider, UUID course){
        ResponseEntity<String> res = client.get().uri("/login").retrieve().toEntity(String.class);
        if (!res.getHeaders().containsKey(HttpHeaders.SET_COOKIE)){
            return Map.of();
        }

        Map<String, HttpCookie> cookies =  res.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .flatMap(rawCookie -> HttpCookie.parse(HttpHeaders.SET_COOKIE + ":" + rawCookie).stream())
                .collect(Collectors.toUnmodifiableMap(HttpCookie::getName, c->c));

        String[] credential = provider.getCredential(CredentialType.GRADESCOPE, course).split(":");

        Document document = Jsoup.parse(Objects.requireNonNull(res.getBody()));

        Element authKeyElement = document.forms().getFirst().getAllElements().stream().filter(e -> e.attr("name").equals("authenticity_token")).toList().getFirst();

        String authorizationKey = authKeyElement.attr("value");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("utf8", "✓");
        formData.add("session[email]", credential[0]);
        formData.add("session[password]", credential[1]);
        formData.add("session[remember_me]", "0");
        formData.add("session[remember_me_sso]", "0");
        formData.add("commit", "Log In");
        formData.add("authenticity_token", authorizationKey);


        ResponseEntity<Void> loginRes = client.post().uri("/login").cookie("_gradescope_session", cookies.get("_gradescope_session").getValue()).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formData).retrieve().toBodilessEntity();

        if (loginRes.getStatusCode() != HttpStatus.FOUND){
            return Map.of();
        }

        return cookies;
    }

    private void logout(HttpCookie cookie){
        ResponseEntity<Void> logoutRes = client.get().uri("/logout").cookie("_gradescope_session", cookie.getValue()).retrieve().toBodilessEntity();
    }

    public MultipartFile downloadCSV(User actingUser, UUID course){
        ImpersonationManager.ImpersonatedUserProvider provider = impersonationManager.impersonateUser(actingUser);

        Map<String, HttpCookie> cookies = login(provider, course);

        if (!cookies.containsKey("_gradescope_session")){
            return null;
        }

        logout(cookies.get("_gradescope_session"));

    }
}
