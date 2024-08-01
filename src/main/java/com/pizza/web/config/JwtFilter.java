package com.pizza.web.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. Validar que sea un Header Authorization valido
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || authHeader.isEmpty() || !authHeader.startsWith("Bearer")){ //validamos si es nulo o si es vacío o si no empieza por Bearer, no será una petición valida
            filterChain.doFilter(request, response); // Le decimos al filterChain que por el lado de este filtro no se realizó ningún proceso de carga de usuario y por lo tanto es una petición invalida
            return;
        }

        // 2. Validar que el JWT sea valido
        String jwt = authHeader
                .split(" ")[1] // separamos la cadena del JWT por el espacio entre Bearer y los caracteres, lo que genera un arreglo con dos filas, la fila 0 contiene el string Bearer y la fila 1 contiene el string de la cadena de caractéres.
                .trim(); // Nos aseguramos que la cadena que obtuvimos de la fila 1 no tenga espacios ni antes ni después.

        if (!this.jwtUtil.isValid(jwt)){ // Verificamos si el JWT NO es valido
            filterChain.doFilter(request, response); // Si el JWT NO es valido le decimos al filterChain que es una petición invalida y que no siga con lo que se encuentre abajo.
            return;
        }

        // 3. Cargar el usuario del UserDetailService
        String username = this.jwtUtil.getUsername(jwt);
        User user = (User) this.userDetailsService.loadUserByUsername(username);


        // 4. Cargar al usuario en el contexto de seguridad
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                user.getUsername(), user.getPassword(), user.getAuthorities());

        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        System.out.println(authenticationToken);
        filterChain.doFilter(request, response);

    }

}
