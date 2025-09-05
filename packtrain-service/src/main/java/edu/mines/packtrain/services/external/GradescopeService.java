package edu.mines.packtrain.services.external;

import edu.mines.packtrain.config.ExternalServiceConfig;
import edu.mines.packtrain.managers.ImpersonationManager;
import edu.mines.packtrain.models.ExternalAssignment;
import edu.mines.packtrain.models.User;
import edu.mines.packtrain.models.enums.CredentialType;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.HttpCookie;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class GradescopeService {
    private final ExternalServiceConfig.GradescopeConfig config;
    private final ImpersonationManager impersonationManager;

    private RestClient client;

    public GradescopeService(ExternalServiceConfig.GradescopeConfig config,
                             ImpersonationManager impersonationManager) {
        this.config = config;
        this.impersonationManager = impersonationManager;

        if (!this.config.isEnabled()) {
            log.warn("Gradescope is disabled!");
            return;
        }

        client = RestClient.builder().baseUrl(config.getUri()).build();
    }

    private Map<String, String> login(ImpersonationManager.ImpersonatedUserProvider provider,
                                      UUID course) {
        ResponseEntity<String> res = client.get().uri("/login").retrieve().toEntity(String.class);
        if (!res.getHeaders().containsKey(HttpHeaders.SET_COOKIE)) {
            return Map.of();
        }

        Map<String, String> cookies = res.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .flatMap(rawCookie ->
                        HttpCookie.parse(HttpHeaders.SET_COOKIE + ":" + rawCookie).stream())
                .collect(Collectors.toUnmodifiableMap(HttpCookie::getName, HttpCookie::getValue));


        String[] credential = provider.getCredential(CredentialType.GRADESCOPE, course)
                .split(":");

        Document document = Jsoup.parse(Objects.requireNonNull(res.getBody()));

        Element authKeyElement = document.forms().getFirst().getAllElements().stream()
                .filter(e -> e.attr("name").equals("authenticity_token"))
                .toList().getFirst();

        String authorizationKey = authKeyElement.attr("value");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("utf8", "✓");
        formData.add("session[email]", credential[0]);
        formData.add("session[password]", credential[1]);
        formData.add("session[remember_me]", "0");
        formData.add("session[remember_me_sso]", "0");
        formData.add("commit", "Log In");
        formData.add("authenticity_token", authorizationKey);


        Map<String, String> finalCookies = cookies;
        ResponseEntity<Void> loginRes = client.post().uri("/login")
                .cookies(c -> c.setAll(finalCookies))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();

        if (loginRes.getStatusCode() != HttpStatus.FOUND) {
            return Map.of();
        }

        cookies = res.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .flatMap(rawCookie -> HttpCookie.parse(HttpHeaders.SET_COOKIE + ":"
                        + rawCookie).stream())
                .collect(Collectors.toUnmodifiableMap(HttpCookie::getName, HttpCookie::getValue));

        return cookies;
    }

    private void logout(Map<String, String> cookies) {
        ResponseEntity<Void> logoutRes = client.get().uri("/logout")
                .cookies(c -> c.setAll(cookies))
                .retrieve()
                .toBodilessEntity();
    }

    public InputStream downloadCSV(User actingUser, UUID course, String courseId,
                                   ExternalAssignment assignment) {
        if (client == null) {
            throw new ExternalServiceDisabledException("Gradescope Service");
        }

        ImpersonationManager.ImpersonatedUserProvider provider =
                impersonationManager.impersonateUser(actingUser);

        Map<String, String> cookies = login(provider, course);

        if (!cookies.containsKey("_gradescope_session")) {
            return null;
        }

        Flux<DataBuffer> res = WebClient.create(config.getUri().toString())
                .get().uri("/courses/{}/assignments/{}/scores.csv",
                        courseId, assignment.getExternalId())
                .cookies(c -> c.setAll(cookies))
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToFlux(DataBuffer.class);


        PipedOutputStream output = new PipedOutputStream();
        PipedInputStream inputStream = new PipedInputStream();

        try {
            inputStream.connect(output);

            DataBufferUtils.write(res, output).subscribe();

            logout(cookies);

            return inputStream;
        } catch (IOException e) {
            log.error("Failed to pipe data!", e);
            return InputStream.nullInputStream();
        }
    }
}
