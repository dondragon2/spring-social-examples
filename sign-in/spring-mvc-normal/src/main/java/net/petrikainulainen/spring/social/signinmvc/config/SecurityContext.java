package net.petrikainulainen.spring.social.signinmvc.config;

import net.petrikainulainen.spring.social.signinmvc.security.service.RepositoryUserDetailsService;
import net.petrikainulainen.spring.social.signinmvc.security.service.SimpleSocialUserDetailsService;
import net.petrikainulainen.spring.social.signinmvc.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.UserIdSource;
import org.springframework.social.security.AuthenticationNameUserIdSource;
import org.springframework.social.security.SocialUserDetailsService;
import org.springframework.social.security.SpringSocialConfigurer;

/**
 * @author Petri Kainulainen
 */
@Configuration
@EnableWebSecurity
public class SecurityContext extends WebSecurityConfigurerAdapter {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web
                //Spring Security ignores request to static resources such as CSS or JS files.
                .ignoring()
                    .antMatchers("/static/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                //Configures form login
                .formLogin()
                    .loginPage("/login")
                    .loginProcessingUrl("/login/authenticate")
                    .failureUrl("/login?error=bad_credentials")
                //Configures the logout function
                .and()
                    .logout()
                        .deleteCookies("JSESSIONID")
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                //Configures url based authorization
                .and()
                    .authorizeRequests()
                        //Anyone can access the urls
                        .antMatchers(
                                "/auth/**",
                                "/login",
                                "/signin/**",
                                "/signup/**",
                                "/user/register/**"
                        ).permitAll()
                        //The rest of the our application is protected.
                        .antMatchers("/**").hasRole("USER")
                //Adds the SocialAuthenticationFilter to Spring Security's filter chain.
                .and()
                    .apply(new SpringSocialConfigurer())
                .and()
                    .setSharedObject(ApplicationContext.class, context);
    }

    /**
     * Configures the authentication manager bean which processes authentication
     * requests.
     */
    @Override
    protected void registerAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService())
                .passwordEncoder(passwordEncoder());
    }

    /**
     * This is used to hash the password of the user.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * This bean is used to load the user specific data when social sign in
     * is used.
     */
    @Bean
    public SocialUserDetailsService socialUserDetailsService() {
        return new SimpleSocialUserDetailsService(userDetailsService());
    }

    /**
     * This bean encrypts the authorization details of the connection. In
     * our example, the authorization details are stored as plain text.
     * DO NOT USE THIS IN PRODUCTION.
     */
    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.noOpText();
    }

    /**
     * This bean is load the user specific data when form login is used.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new RepositoryUserDetailsService(userRepository);
    }

    /**
     * This bean determines the account ID of the user. The example application
     * uses the username as the account ID.
     */
    @Bean
    public UserIdSource userIdSource() {
        return new AuthenticationNameUserIdSource();
    }
}
