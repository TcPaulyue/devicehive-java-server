package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.security.jwt.JwtClientService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class JwtClientServiceTest  extends AbstractResourceTest {
    
    @Autowired
    private JwtClientService jwtClientService;
            
    @Value("${jwt.secret}")
    private String secret;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void should_generate_jwt_token_with_access_type() throws Exception {
        // Create payload
        Long userId = RandomUtils.nextLong(10, 1000); 
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceGuids = new HashSet<>();
        deviceGuids.add("string");
        JwtPayload.Builder builder = new JwtPayload.Builder();
        JwtPayload payload = builder.withPublicClaims(userId, actions, networkIds,deviceGuids).buildPayload();

        String token = jwtClientService.generateJwtAccessToken(payload);
        JwtPayload resultPayload = jwtClientService.getPayload(token);

        assertEquals(resultPayload.getTokenType(), TokenType.ACCESS);
    }

    @Test
    public void should_generate_jwt_token_with_refresh_type() throws Exception {
        // Create payload
        Long userId = RandomUtils.nextLong(10, 1000);
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceGuids = new HashSet<>();
        deviceGuids.add("string");
        JwtPayload.Builder builder = new JwtPayload.Builder();
        JwtPayload payload = builder.withPublicClaims(userId, actions, networkIds,deviceGuids).buildPayload();

        String token = jwtClientService.generateJwtRefreshToken(payload);
        JwtPayload resultPayload = jwtClientService.getPayload(token);

        assertEquals(resultPayload.getTokenType(), TokenType.REFRESH);
    }

    @Test(expected = MalformedJwtException.class)
    public void should_throw_MalformedJwtException_whet_pass_token_without_expiration_and_type() throws Exception {
        // Create payload
        Long userId = RandomUtils.nextLong(10, 1000);
        Set<String> actions = new HashSet<>();
        actions.add("string");
        Set<String> networkIds = new HashSet<>();
        networkIds.add("string");
        Set<String> deviceGuids = new HashSet<>();
        deviceGuids.add("string");
        JwtPayload.Builder builder = new JwtPayload.Builder();
        JwtPayload payload = builder.withPublicClaims(userId, actions, networkIds, deviceGuids).buildPayload();

        // Generate key without expiration date and token type
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put(JwtPayload.JWT_CLAIM_KEY, payload);
        Claims claims = Jwts.claims(jwtMap);
        String malformedToken = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
        jwtClientService.getPayload(malformedToken);
    }
    
}
