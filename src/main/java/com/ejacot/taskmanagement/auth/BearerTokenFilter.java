package com.ejacot.taskmanagement.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {
    private final TokenService tokens; private final UserDetailsService users;
    public BearerTokenFilter(TokenService tokens, UserDetailsService users){this.tokens=tokens;this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException, IOException {
        String header=request.getHeader("Authorization");
        if(header!=null&&header.startsWith("Bearer ")&&SecurityContextHolder.getContext().getAuthentication()==null){
            tokens.username(header.substring(7)).ifPresent(username->{var details=users.loadUserByUsername(username);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(details,null,details.getAuthorities()));});
        }
        chain.doFilter(request,response);
    }
}
