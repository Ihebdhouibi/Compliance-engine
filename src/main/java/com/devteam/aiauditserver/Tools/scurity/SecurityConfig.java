package com.devteam.aiauditserver.Tools.scurity;

import com.devteam.aiauditserver.Tools.util.JwtAuthenticationEntryPoint;
import com.devteam.aiauditserver.services.Oathloginservice.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private final String[] PUBLIC_ENDPOINTS = {
            "/v2/api-docs",
            "/swagger-resources/**",
            "/swagger-ui.html",
            "/swagger.json",
            "/favicon.ico/**",
            "/view/**",
            "/resources/**",
            "/swagger-ui/**",
            "/api/v1/auth/**",
            "/api/v1/**",
            "/api/v1/public/**",
            "/api/v1/country/**",
            "/api/v1/contact/**",
            "/WebContent/**",
            "/api/v1/add_missing_social_data",
            "/asset/**",
            "/ws",
            "/STATIC/**",
            "/api/v1/faq/**",
            "/socket",
            "/apple",
            "/apple/**",
            "/socket/**",
            "/auth/**",
            "/api/auth_otp/**",
            "/v3/api-docs/**",
            "/dashboard/v1/**",
            "/app/**",
            "/api/v1/filter/**",
            "/images/**",
            "/user/**"
    };

    @Bean
    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    @Bean
    public AuthFilter authFilter() {
        return new AuthFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder
                .userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors().and().csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(PUBLIC_ENDPOINTS).permitAll()
                .anyRequest().authenticated();

        // Standard Filter for JWT, removing the oauth2Login block entirely
        http.addFilterBefore(authFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
