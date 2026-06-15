package com.example.rec.config;

import com.example.rec.model.Content;
import com.example.rec.model.User;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Order(10)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    public DataSeeder(UserRepository userRepository, ContentRepository contentRepository) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            seedUsers();
        }
        if (contentRepository.count() == 0) {
            seedContent();
        }
    }

    private void seedUsers() {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            User u = new User();
            u.setUsername("User " + i);
            u.setHandle("@user" + i);
            u.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + i);
            u.setPassword("password"); // In real app, encode this
            u.setRole(i == 1 ? "ADMIN" : "USER");
            users.add(u);
        }
        userRepository.saveAll(users);
        System.out.println("Seeded 50 users.");
    }

    private void seedContent() {
        List<Content> contents = new ArrayList<>();
        String[] categories = {"Tech", "Life", "Sports", "News"};
        Random rand = new Random();
        List<User> users = userRepository.findAll();

        for (int i = 1; i <= 200; i++) {
            Content c = new Content();
            // Random Author
            User author = users.get(rand.nextInt(users.size()));
            c.setAuthor(author);
            
            c.setTitle("Topic " + i);
            c.setContent("Just setting up my Twitter clone! This is post #" + i + ". #java #vue");
            c.setCategory(categories[rand.nextInt(categories.length)]);
            
            // Random Image for some posts
            if (rand.nextBoolean()) {
                c.setImageUrl("https://picsum.photos/seed/" + i + "/400/200");
            }

            c.setViewCount(rand.nextInt(1000));
            c.setLikeCount(rand.nextInt(100));
            c.setCommentCount(rand.nextInt(20));
            contents.add(c);
        }
        contentRepository.saveAll(contents);
        System.out.println("Seeded 200 content items.");
    }
}
