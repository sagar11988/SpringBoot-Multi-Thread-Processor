package com.sds.springbootmultithreading.service;

import com.sds.springbootmultithreading.entity.User;
import com.sds.springbootmultithreading.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;


@Service
@Log4j2
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    @Qualifier("processExecutor")
    Executor executor;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Async
    public CompletableFuture<List<User>> saveUsers(MultipartFile file) throws Exception {
        long start = System.currentTimeMillis();
        List<User> users = parseCSVFile(file);
        log.info("saving list of users of size {}", users.size(), "" + Thread.currentThread().getName());
        users = repository.saveAll(users);
        long end = System.currentTimeMillis();
        log.info("Total time {}", (end - start));
        return CompletableFuture.completedFuture(users);
    }

    @Async
    public CompletableFuture<List<User>> findAllUsers() {
        log.info("get list of user by " + Thread.currentThread().getName());
        List<User> users = repository.findAll();
        users.parallelStream().forEach(user -> {
            log.info("Processing user {} on {}", user.getName(), Thread.currentThread().getName());
        });
        return CompletableFuture.completedFuture(users);
    }


    public List<User> findAllUsersByTaskExecutor() {
        log.info("get list of user by " + Thread.currentThread().getName());
        List<User> users = repository.findAll();
        CountDownLatch latch = new CountDownLatch(users.size());
        for (User user : users){
           try {
               executor.execute(() -> {
                   String oldName = user.getName();
                   user.setName(oldName + "_updated");  // change the name
                   log.info("Updated user {} → {} on {}", oldName, user.getName(),
                           Thread.currentThread().getName());
               });
           }finally {
               latch.countDown();
           }
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            log.info("Processing of "+users.size() +" has been completed!!");
        }

        return users;
    }

    private List<User> parseCSVFile(final MultipartFile file) throws Exception {
        final List<User> users = new ArrayList<>();
        try {
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    final String[] data = line.split(",");
                    final User user = new User();
                    user.setName(data[0]);
                    user.setEmail(data[1]);
                    user.setGender(data[2]);
                    users.add(user);
                }
                return users;
            }
        } catch (final IOException e) {
            log.error("Failed to parse CSV file {}", e);
            throw new Exception("Failed to parse CSV file {}", e);
        }
    }

}
