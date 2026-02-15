package org.example.userservice.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.example.userservice.exception.UserAlreadyExistException;
import org.example.userservice.exception.UserNotFoundException;
import org.example.userservice.exception.WrongPasswordException;
import org.example.userservice.model.Role;
import org.example.userservice.model.Session;
import org.example.userservice.model.User;
import org.example.userservice.repository.SessionRepository;
import org.example.userservice.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
public class AuthService {

    private UserRepository userRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    SecretKey key = Jwts.SIG.HS256.key().build();
    private SessionRepository sessionRepository;

    public AuthService(UserRepository userRepository,BCryptPasswordEncoder bCryptPasswordEncoder,SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.sessionRepository = sessionRepository;
    }

    public boolean  signUp(String email, String password) throws UserAlreadyExistException {
        if(userRepository.findByEmail(email).isPresent()){
            throw new UserAlreadyExistException("User with email"+email+"already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        userRepository.save(user);
        return true;
    }

    public String login(String email, String password) throws UserNotFoundException, WrongPasswordException {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            throw new UserNotFoundException("User with email"+email+"not found");
        }
        boolean matches = bCryptPasswordEncoder.matches(
                password,
                userOptional.get().getPassword());

        if(matches){
            String token = createJwtToken(userOptional.get().getId(),new ArrayList<>(),userOptional.get().getEmail());
            Session session =  new Session();
            session.setToken(token);
            session.setUser(userOptional.get());

            Calendar calendar = Calendar.getInstance();
            Date currentDate = calendar.getTime();

            calendar.add(Calendar.DAY_OF_MONTH, 30);
            Date datePlus30Days = calendar.getTime();

            session.setExpiringAt(datePlus30Days);
            sessionRepository.save(session);
            return token;
        }else {
            throw new WrongPasswordException("wrong password");
        }
    }

    public String createJwtToken(Long userId,List<Role> roles,String email){
        Map<String, Object> dataInJwt = new HashMap<>();
        dataInJwt.put("userId",userId);
        dataInJwt.put("roles",roles);
        dataInJwt.put("email",email);

        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, 30);
        Date datePlus30Days = calendar.getTime();

        String token = Jwts.builder()
                .claims(dataInJwt)
                .expiration(datePlus30Days)
                .issuedAt(new Date())
                .signWith(key)
                .compact();
        return token;
    }

    public boolean validate(String token){
        try{
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            Date expiryAt = claims.getPayload().getExpiration();
            Long userId = claims.getPayload().get("userId", Long.class);
        }catch(Exception e){
            return false;
        }
        return true;

    }
}
