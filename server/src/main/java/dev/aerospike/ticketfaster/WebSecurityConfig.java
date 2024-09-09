package dev.aerospike.ticketfaster;

import org.springframework.context.annotation.Configuration;

@Configuration
//@EnableWebSecurity
public class WebSecurityConfig {

	// See: https://www.youtube.com/watch?v=phs90_s0Mjk
//	@Bean
//	public CorsConfigurationSource corsConfigurationSource() {
//		CorsConfiguration configuration = new CorsConfiguration();
//		
//		configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:5174"));
//		configuration.setAllowedMethods(Arrays.asList("GET", "POST"));
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", configuration);
//		return source;
//	}
//	
//	@Bean
//	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//		http
//			.exceptionHandling(c -> c.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
//			.cors(c -> c.configurationSource(corsConfigurationSource()))
//			.csrf(AbstractHttpConfigurer::disable)
//			.sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//			.authorizeHttpRequests(req -> req.anyRequest().permitAll());
//		return http.build();
//	}

}