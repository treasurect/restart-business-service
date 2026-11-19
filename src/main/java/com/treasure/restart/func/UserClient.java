package com.treasure.restart.func;

import com.treasure.common.response.BaseResponse;
import com.treasure.restart.dto.UserSimpleVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestClient restClient;

    public UserClient(@Value("${restart.user.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 批量查询用户信息，失败时降级返回空 Map，避免用户服务异常影响主流程。
     */
    public Map<Long, UserSimpleVO> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> distinctIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            String authorization = currentAuthorization();
            if (authorization == null) {
                log.warn("当前请求未携带 Authorization，跳过用户信息批量查询");
                return Collections.emptyMap();
            }

            BaseResponse<List<UserSimpleVO>> response = restClient.post()
                    .uri("/api/user/batch")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("ids", distinctIds))
                    .retrieve()
                    .body(new ParameterizedTypeReference<BaseResponse<List<UserSimpleVO>>>() {
                    });

            if (response == null || response.code() == null || response.code() != 200 || response.data() == null) {
                log.warn("用户批量查询响应异常: {}", response);
                return Collections.emptyMap();
            }

            return response.data().stream()
                    .filter(user -> user != null && user.id() != null)
                    .collect(Collectors.toMap(
                            UserSimpleVO::id,
                            Function.identity(),
                            (left, right) -> left
                    ));
        } catch (RestClientException e) {
            log.warn("批量查询用户信息失败，降级返回空用户信息: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String currentAuthorization() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
